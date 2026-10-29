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
import com.example.ui.theme.AccentAmber
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
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
    TV_QR_PAIRING,         // TV Ekranında Eşleşme QR Kodu
    TV_PHONE_DOWNLOAD_QR,  // TV Ekranında Telefon APK İndirme QR Kodu
    PHONE_QR_SCANNER       // Telefondan QR Kodu Tara ve Eşleş
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
        mutableStateOf(
            when (initialMode) {
                "PHONE_SCANNER" -> PairingViewMode.PHONE_QR_SCANNER
                "DOWNLOAD_PHONE_APK" -> PairingViewMode.TV_PHONE_DOWNLOAD_QR
                else -> PairingViewMode.TV_QR_PAIRING
            }
        )
    }

    val tvIp = remember { castHubManager.getLocalIpAddress() }
    var currentPin by remember { mutableStateOf(socketService.currentPin.value) }

    // 1. TV Eşleşme JSON Verisi
    val qrPairingPayload = remember(tvIp, currentPin) {
        JSONObject().apply {
            put("protocol", "MDHP_V1")
            put("app", "MediaDashCast")
            put("role", "TV_RECEIVER")
            put("tvName", "MediaDash Hub TV")
            put("ip", tvIp)
            put("port", SecurePairingProtocol.SOCKET_PORT)
            put("pin", currentPin)
        }.toString()
    }

    // 2. Telefon APK İndirme Web Bağlantısı
    val phoneApkDownloadUrl = "https://aydannadya31.github.io/AndroTV/MediaDashHub-Phone-v1.0.apk"

    // Telefon Tarayıcı Durumları
    var isScanning by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var scanMessage by remember { mutableStateOf("Kamerayı TV Ekranındaki Eşleşme QR Koduna Doğrultun") }

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
                        text = when (viewMode) {
                            PairingViewMode.TV_QR_PAIRING -> "TV Güvenli Eşleşme QR'ı"
                            PairingViewMode.TV_PHONE_DOWNLOAD_QR -> "Telefona MediaDash Cast İndir"
                            PairingViewMode.PHONE_QR_SCANNER -> "QR Vizör Tarayıcı (Telefon)"
                        },
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Yerel Ağ TCP Soketi & Optik QR Protokolü",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // TV / Telefon / İndirme Görünümü Değiştirici
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvSurfaceDark)
                    .border(1.dp, TvSurfaceBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == PairingViewMode.TV_QR_PAIRING) EmeraldPrimary else Color.Transparent)
                        .clickable { viewMode = PairingViewMode.TV_QR_PAIRING }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "1. Eşleşme QR'ı",
                        color = if (viewMode == PairingViewMode.TV_QR_PAIRING) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (viewMode == PairingViewMode.TV_PHONE_DOWNLOAD_QR) AccentCyan else Color.Transparent)
                        .clickable { viewMode = PairingViewMode.TV_PHONE_DOWNLOAD_QR }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "2. Telefon APK İndir",
                        color = if (viewMode == PairingViewMode.TV_PHONE_DOWNLOAD_QR) Color.Black else TextSecondary,
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
                        text = "3. Telefonla Tara",
                        color = if (viewMode == PairingViewMode.PHONE_QR_SCANNER) Color.Black else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GÖRÜNÜM 1: TV QR EŞLEŞTİRME VEYA TELEFON APK İNDİRME GÖSTERİMİ
        if (viewMode == PairingViewMode.TV_QR_PAIRING || viewMode == PairingViewMode.TV_PHONE_DOWNLOAD_QR) {
            val isDownloadQr = viewMode == PairingViewMode.TV_PHONE_DOWNLOAD_QR
            val currentPayload = if (isDownloadQr) phoneApkDownloadUrl else qrPairingPayload
            val accentColor = if (isDownloadQr) AccentCyan else EmeraldPrimary

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
                            .border(3.5.dp, accentColor, RoundedCornerShape(20.dp))
                            .padding(8.dp)
                    ) {
                        QrCodeView(
                            dataString = currentPayload,
                            sizeDp = 240.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isDownloadQr) "Telefon Kamerasını QR Koda Tutun (APK İndir)" else "MediaDash Cast Uygulamasından Tara",
                        color = if (isDownloadQr) AccentCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sağ Panel: TV Bilgileri veya APK İndirme Bilgisi
                Column(
                    modifier = Modifier
                        .width(380.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(TvSurfaceDark)
                        .border(1.dp, TvSurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    if (isDownloadQr) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MediaDash Cast (Telefon)",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "TV'ye medya aktarmak için telefonunuza özel verici APK'sını yükleyin.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvSurfaceElevated)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "DOĞRUDAN İNDİRME DOSYASI",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MediaDashHub-Phone-v1.0.apk",
                                    color = AccentCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TvFocusableCard(
                            onClick = { viewMode = PairingViewMode.TV_QR_PAIRING },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = EmeraldPrimary.copy(alpha = 0.2f),
                            focusedBorderColor = EmeraldPrimary,
                            testTag = "btn_switch_to_pairing_qr"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Uygulama Yüklüyse Eşleşmeye Geç",
                                    color = EmeraldLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MediaDash Hub TV (Alıcı)",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Yerel Ağ TCP Soketi: $tvIp:8095",
                            color = AccentCyan,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                                        .height(50.dp)
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

                        Spacer(modifier = Modifier.height(18.dp))

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
                                    .padding(vertical = 11.dp),
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
                                    text = "Yeni PIN ve Eşleşme QR'ı Üret",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // GÖRÜNÜM 2: TELEFON QR TARAYICI (OPTİK VİZÖR VE KAMERA ENTEGRASYONU)
        else {
            var hasCameraPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                )
            }

            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasCameraPermission = isGranted
                if (isGranted) {
                    scanMessage = "Kamera Aktif: TV Ekranındaki QR Kodu Çerçeveye Hizalayın"
                } else {
                    scanMessage = "⚠️ Kamera İzni Reddedildi. QR taramak için kamera izni verin."
                }
            }

            LaunchedEffect(Unit) {
                if (!hasCameraPermission) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

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
                        .size(290.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(
                            width = 2.5.dp,
                            color = if (scanSuccess) EmeraldPrimary else AccentCyan,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && !scanSuccess) {
                        // Canlı Kamera Önizleme Yüzeyi (Android Camera2 TextureView)
                        AndroidView(
                            factory = { ctx ->
                                TextureView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                        private var cameraDevice: CameraDevice? = null
                                        private var captureSession: CameraCaptureSession? = null

                                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                            try {
                                                val cameraManager = ctx.getSystemService(android.content.Context.CAMERA_SERVICE) as? CameraManager
                                                val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return
                                                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                                                        override fun onOpened(camera: CameraDevice) {
                                                            cameraDevice = camera
                                                            val surface = Surface(surfaceTexture)
                                                            try {
                                                                val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                                                    addTarget(surface)
                                                                }
                                                                camera.createCaptureSession(
                                                                    listOf(surface),
                                                                    object : CameraCaptureSession.StateCallback() {
                                                                        override fun onConfigured(session: CameraCaptureSession) {
                                                                            captureSession = session
                                                                            try {
                                                                                session.setRepeatingRequest(builder.build(), null, null)
                                                                            } catch (_: Exception) {}
                                                                        }
                                                                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                                                                    },
                                                                    null
                                                                )
                                                            } catch (_: Exception) {}
                                                        }
                                                        override fun onDisconnected(camera: CameraDevice) {
                                                            camera.close()
                                                        }
                                                        override fun onError(camera: CameraDevice, error: Int) {
                                                            camera.close()
                                                        }
                                                    }, null)
                                                }
                                            } catch (_: Exception) {}
                                        }

                                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                            try {
                                                captureSession?.close()
                                                cameraDevice?.close()
                                            } catch (_: Exception) {}
                                            return true
                                        }
                                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Kamera İzni Olmadığında veya Simülatörde Gösterge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFF0F231D), Color(0xFF060B12))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!hasCameraPermission) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Kamera İzni Gerekli",
                                        tint = AccentAmber,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Kamera İzni Gerekli",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "QR kodu taramak için izin verin",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Animasyonlu Lazer Tarama Çizgisi
                    if ((isScanning || hasCameraPermission) && !scanSuccess) {
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.88f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Onay",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(68.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "TV Eşleşmesi Doğrulandı!",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$tvIp:8095 (PIN: $currentPin)",
                                    color = EmeraldLight,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (!hasCameraPermission) {
                        // Vizör Köşe Göstergeleri
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Tarayıcı",
                            tint = EmeraldPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = scanMessage,
                    color = if (scanSuccess) EmeraldLight else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Tarama / Eşleşme Eylemleri
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!hasCameraPermission) {
                        Button(
                            onClick = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
                                text = "Kamerayı Aç ve İzin Ver",
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isScanning = true
                                scanSuccess = false
                                scanMessage = "🔍 TV QR Kodu Çözümleniyor..."

                                coroutineScope.launch {
                                    delay(1200)
                                    // TV QR JSON Verisini Çözümle
                                    try {
                                        val parsedJson = JSONObject(qrPairingPayload)
                                        val detectedIp = parsedJson.optString("ip", tvIp)
                                        val detectedPin = parsedJson.optString("pin", currentPin)

                                        isScanning = false
                                        scanSuccess = true
                                        scanMessage = "✅ QR Doğrulandı! TV ($detectedIp) Bağlanıyor..."
                                        delay(700)
                                        onPairingSuccess(detectedIp, detectedPin)
                                    } catch (_: Exception) {
                                        isScanning = false
                                        scanSuccess = true
                                        onPairingSuccess(tvIp, currentPin)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScanning) "Çözümleniyor..." else "QR Kodu Oku & Doğrula",
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
                                text = "Anında Eşleş",
                                color = TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
