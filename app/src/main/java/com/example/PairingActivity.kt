package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.CastHubManager
import com.example.service.SecurePairingProtocol
import com.example.service.SocketTransmissionService
import com.example.ui.components.QrCodeView
import com.example.ui.components.TvFocusableCard
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvBackgroundDark
import com.example.ui.theme.TvSurfaceBorder
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class PairingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialMode = intent.getStringExtra("INITIAL_MODE") ?: "TV_DISPLAY"

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBackgroundDark
                ) {
                    PairingScreen(
                        initialMode = initialMode,
                        onPairingSuccess = { tvIp, pin ->
                            val resultIntent = Intent().apply {
                                putExtra("PAIRED_TV_IP", tvIp)
                                putExtra("PAIRED_PIN", pin)
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

enum class PairingViewMode {
    TV_QR_DISPLAY,      // TV Ekranında QR Kodu Göster
    PHONE_QR_SCANNER    // Telefondan QR Kodu Tara ve Eşleş
}

@Composable
fun PairingScreen(
    initialMode: String,
    onPairingSuccess: (tvIp: String, pin: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val castHubManager = remember { CastHubManager(context) }
    val socketService = remember { SocketTransmissionService(context) }

    var viewMode by remember {
        mutableStateOf(if (initialMode == "PHONE_SCANNER") PairingViewMode.PHONE_QR_SCANNER else PairingViewMode.TV_QR_DISPLAY)
    }

    val tvIp = remember { castHubManager.getLocalIpAddress() }
    var currentPin by remember { mutableStateOf(socketService.currentPin.value) }

    // Eşleşme JSON Yükü
    val qrPayloadString = remember(tvIp, currentPin) {
        JSONObject().apply {
            put("protocol", "MDHP_V1")
            put("tvName", "MediaDash Hub TV")
            put("ip", tvIp)
            put("port", SecurePairingProtocol.SOCKET_PORT)
            put("pin", currentPin)
        }.toString()
    }

    // Telefon Tarayıcı Durumları
    var isScanning by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Kamerayı TV Ekranındaki QR Koda Doğrultun") }

    // Tarama Çizgisi Animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_anim")
    val scanLineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TvBackgroundDark)
            .padding(24.dp)
    ) {
        // Üst Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TvSurfaceElevated)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (viewMode == PairingViewMode.TV_QR_DISPLAY) "TV Hızlı Eşleştirme" else "QR Kod Tarayıcı",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Yerel Ağ Güvenli Soket Protokolü",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // TV / Telefon Görünümü Değiştirici
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvSurfaceDark)
                    .border(1.dp, TvSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == PairingViewMode.TV_QR_DISPLAY) EmeraldPrimary else Color.Transparent)
                        .clickable { viewMode = PairingViewMode.TV_QR_DISPLAY }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TV Kodu Göster",
                        color = if (viewMode == PairingViewMode.TV_QR_DISPLAY) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == PairingViewMode.PHONE_QR_SCANNER) EmeraldPrimary else Color.Transparent)
                        .clickable { viewMode = PairingViewMode.PHONE_QR_SCANNER }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Telefonla Tara",
                        color = if (viewMode == PairingViewMode.PHONE_QR_SCANNER) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GÖRÜNÜM 1: TV QR KOD GÖSTERİMİ
        if (viewMode == PairingViewMode.TV_QR_DISPLAY) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol Panel: QR Kod Çerçevesi
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(3.dp, EmeraldPrimary, RoundedCornerShape(20.dp))
                            .padding(8.dp)
                    ) {
                        QrCodeView(
                            dataString = qrPayloadString,
                            sizeDp = 240.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Kamerayı veya MediaDash'i Doğrultun",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Sağ Panel: TV Bilgileri ve PIN
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvSurfaceDark)
                        .border(1.dp, TvSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MediaDash Hub TV",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Yerel Ağ Soketi: $tvIp:8095",
                        color = AccentCyan,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "MANUEL DOĞRULAMA PIN KODU",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 6 Haneli PIN Göstergesi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentPin.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TvSurfaceElevated)
                                    .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit.toString(),
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TvFocusableCard(
                        onClick = {
                            currentPin = socketService.regeneratePin()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = TvSurfaceElevated,
                        testTag = "btn_regenerate_pin_activity"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yenile",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Yeni PIN ve QR Üret",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // GÖRÜNÜM 2: TELEFON QR TARAYICI (OPTİK VİZÖR)
        else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Kamera Vizörü (Viewfinder Frame)
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(
                            width = 2.dp,
                            color = if (scanSuccess) EmeraldPrimary else AccentCyan,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Arka Plan Kamera Simülasyon Ağı
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF0F231D), Color(0xFF060B12))
                                )
                            )
                    )

                    // Animasyonlu Lazer Tarama Çizgisi
                    if (isScanning && !scanSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset { IntOffset(0, (scanLineOffset - 120).dp.roundToPx()) }
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, EmeraldPrimary, Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Başarılı Olduğunda Onay İkonu
                    if (scanSuccess) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Onay",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "TV Eşleşti!",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Vizör Köşe Göstergeleri
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Tarayıcı",
                            tint = if (isScanning) EmeraldPrimary else TextMuted,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = scanMessage,
                    color = if (scanSuccess) EmeraldLight else TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tarama / Eşleşme Eylemleri
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = {
                            isScanning = true
                            scanSuccess = false
                            scanMessage = "Optik Vizör Aktif: TV QR Kodu Taranıyor..."

                            coroutineScope.launch {
                                delay(1800) // Hızlı QR tanıma simülasyonu
                                isScanning = false
                                scanSuccess = true
                                scanMessage = "✅ MediaDash TV ($tvIp) Başarıyla Tanındı!"
                                delay(900)
                                onPairingSuccess(tvIp, currentPin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isScanning) "Taranıyor..." else "QR Kodu Tara",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onPairingSuccess(tvIp, currentPin)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hızlı Eşleş",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
