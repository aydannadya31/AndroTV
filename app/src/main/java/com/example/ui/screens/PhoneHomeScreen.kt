package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PairingActivity
import com.example.data.local.MediaType
import com.example.service.PairingState
import com.example.service.PhoneStreamingForegroundService
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvBackgroundDark
import com.example.ui.theme.TvSurfaceBorder
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated

/**
 * Android Akıllı Telefonlar İçin Özel Medya İletim ve TV Uzaktan Kontrol Arayüzü.
 * - Portre odaklı, modern dokunmatik M3 tasarım
 * - Kamera ile TV ekranındaki QR kodu tek dokunuşla tarayıp anında eşleşme
 * - Sıfır TV Depolaması: Fotoğraf, Video ve Müzikleri TV hafızasını doldurmadan doğrudan telefondan canlı yayınlama
 * - Kilit ekranı / Ekran kapalıyken kesintisiz arka plan yayını (Partial WakeLock + WifiLock)
 * - TV tarafına ait gereksiz sensör/D-pad kayıtlarından tamamen arındırılmış temiz mobil deneyim
 */
@Composable
fun PhoneHomeScreen(
    viewModel: MainDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tvIpInput by remember { mutableStateOf(uiState.targetTvIp) }
    var pinInput by remember { mutableStateOf("") }
    var showManualSettings by remember { mutableStateOf(false) }
    var remoteVolume by remember { mutableFloatStateOf(0.85f) }
    var isPaused by remember { mutableStateOf(false) }

    val isPaired = uiState.activePhoneSession != null || uiState.pairingState == PairingState.PAIRED
    val isStreamingServiceActive by PhoneStreamingForegroundService.isServiceRunning.collectAsStateWithLifecycle()
    val currentStreamingTitle by PhoneStreamingForegroundService.currentStreamingTitle.collectAsStateWithLifecycle()

    // Kamera İzni ve Eşleşme QR Tarayıcısı Launcher
    val qrScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pairedIp = result.data?.getStringExtra("PAIRED_TV_IP")
            val pairedPin = result.data?.getStringExtra("PAIRED_PIN")
            if (!pairedIp.isNullOrEmpty()) {
                tvIpInput = pairedIp
                viewModel.updateTargetTvIp(pairedIp)
            }
            if (!pairedPin.isNullOrEmpty()) {
                pinInput = pairedPin
                viewModel.pairWithTv(pairedPin)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val intent = Intent(context, PairingActivity::class.java).apply {
                putExtra("INITIAL_MODE", "PHONE_SCANNER")
            }
            qrScanLauncher.launch(intent)
        }
    }

    // Android Fotoğraf Seçici (Sıfır Depolama - TV'de 4K Görüntüleme)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Telefondan Fotoğraf"
            viewModel.streamMediaOverSocket(fileName, MediaType.PHOTO, uri.toString())
        }
    }

    // Android Müzik & Ses Seçici (Kayıpsız Canlı Yayın)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Telefondan Müzik"
            viewModel.streamMediaOverSocket(fileName, MediaType.MUSIC, uri.toString())
        }
    }

    // Android Video Seçici (TV Diskine Yazmadan Doğrudan RAM HTTP Yayını)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Telefondan Video"
            viewModel.streamMediaOverSocket(fileName, MediaType.MOVIE, uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackgroundDark)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ÜST BAR: TELEFON MEDYA AKTARICISI & DURUM ROZETİ
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Telefon",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MediaDash TV İletici",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Canlı TV Aktarım & Sıfır Depolama",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Bağlantı Durumu Hapı
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isPaired) EmeraldPrimary.copy(alpha = 0.2f)
                            else AccentAmber.copy(alpha = 0.15f)
                        )
                        .border(
                            1.dp,
                            if (isPaired) EmeraldPrimary else AccentAmber.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isPaired) EmeraldPrimary else AccentAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaired) "TV Bağlı" else "Bağlı Değil",
                            color = if (isPaired) EmeraldLight else AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. ÖNCELİKLİ HIZLI BAĞLANTI: TV QR KODUNU KAMERA İLE TARA (BÜYÜK EYLEM KARTI)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isPaired) Color(0xFF062E22) else Color(0xFF131D31),
                                TvSurfaceDark
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        if (isPaired) EmeraldPrimary else AccentCyan,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPaired) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = if (isPaired) EmeraldPrimary else AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPaired) "TV ile Güvenli Eşleşildi" else "TV'nin QR Kodunu Tara",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPaired)
                                "Eşleşmiş TV: ${uiState.targetTvIp}:8095 (Güvenli Token Aktif)"
                            else
                                "TV ekranındaki QR koda kameranızı tutun, anında bağlanın.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Büyük Kamera QR Tarama Butonu
                Button(
                    onClick = {
                        val hasCam = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCam) {
                            val intent = Intent(context, PairingActivity::class.java).apply {
                                putExtra("INITIAL_MODE", "PHONE_SCANNER")
                            }
                            qrScanLauncher.launch(intent)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_phone_scan_qr"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPaired) TvSurfaceElevated else EmeraldPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Kamera ile Tara",
                        tint = if (isPaired) EmeraldPrimary else Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPaired) "Tekrar QR Tara (TV Değiştir)" else "Kamerayı Aç ve TV QR Kodunu Tara",
                        color = if (isPaired) TextPrimary else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Manuel IP / PIN Girişi Akordiyon Başlığı
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualSettings = !showManualSettings }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Veya Manuel IP ve PIN ile Bağlan",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = if (showManualSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Manuel Giriş Formu (Açılır/Kapanır)
                AnimatedVisibility(visible = showManualSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = tvIpInput,
                            onValueChange = {
                                tvIpInput = it
                                viewModel.updateTargetTvIp(it)
                            },
                            label = { Text("Android TV IP Adresi", fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = TvSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("input_tv_ip")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { if (it.length <= 6) pinInput = it },
                                label = { Text("TV PIN Kodu (6 Hane)", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = TvSurfaceBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f).testTag("input_tv_pin")
                            )

                            Button(
                                onClick = {
                                    if (pinInput.isNotBlank()) {
                                        viewModel.pairWithTv(pinInput)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("btn_manual_pair")
                            ) {
                                Text("Bağlan", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. MEDYA İLETİM MERKEZİ: FOTOĞRAF, VİDEO, MÜZİK/SES SEÇİMİ
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "CANLI MEDYA YAYINI (TV'YE YÜKLEMEDEN)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fotoğraf Seçici Kartı
                    PhoneMediaActionTile(
                        icon = Icons.Default.Image,
                        title = "Fotoğraf",
                        subtitle = "4K Canlı Görsel",
                        accentColor = AccentCyan,
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        testTag = "btn_cast_photo",
                        modifier = Modifier.weight(1f)
                    )

                    // Video Seçici Kartı
                    PhoneMediaActionTile(
                        icon = Icons.Default.Videocam,
                        title = "Video",
                        subtitle = "Doğrudan Akış",
                        accentColor = EmeraldPrimary,
                        onClick = {
                            videoPickerLauncher.launch("video/*")
                        },
                        testTag = "btn_cast_video",
                        modifier = Modifier.weight(1f)
                    )

                    // Müzik & Ses Seçici Kartı
                    PhoneMediaActionTile(
                        icon = Icons.Default.Audiotrack,
                        title = "Müzik/Ses",
                        subtitle = "Kayıpsız FLAC",
                        accentColor = AccentAmber,
                        onClick = {
                            audioPickerLauncher.launch("audio/*")
                        },
                        testTag = "btn_cast_audio",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. KİLİT EKRANI & ARKA PLAN KORUMASI GARANTİ ROZETİ
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1E2E))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Kilit Ekranı Koruması",
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Kilit Ekranı Koruması Devrede",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Yayın esnasında telefon ekranı kapansa veya kilitlense dahi, 'PhoneStreamingForegroundService' (Partial WakeLock ve WifiLock) sayesinde bağlantı ve TV yayını kesilmeden arka planda devam eder.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 5. CANLI YAYIN VE UZAKTAN KUMANDA KONTROLLERİ
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvSurfaceDark)
                    .border(1.dp, TvSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CastConnected,
                            contentDescription = null,
                            tint = if (isStreamingServiceActive || uiState.isPhoneCasting) EmeraldPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yayın Kontrol Paneli",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isStreamingServiceActive || uiState.isPhoneCasting) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "CANLI YAYINDA",
                                color = EmeraldLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Oynatılan Medya Bilgisi
                val activeTitle = currentStreamingTitle ?: uiState.castNotification ?: "Aktif yayın bulunmuyor"
                Text(
                    text = activeTitle,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Oynat/Duraklat ve Yayını Durdur Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TvSurfaceElevated
                        )
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "Sürdür" else "Duraklat",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            PhoneStreamingForegroundService.stopStreaming(context)
                            viewModel.dismissCastNotification()
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F1D1D)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yayını Kes",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TV Ses Seviyesi Ayarı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Ses",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = remoteVolume,
                        onValueChange = { remoteVolume = it },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary,
                            inactiveTrackColor = TvSurfaceBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%${(remoteVolume * 100).toInt()}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. ADIM ADIM KULLANIM REHBERİ
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvSurfaceDark)
                    .padding(14.dp)
            ) {
                Text(
                    text = "HIZLI KULLANIM REHBERİ",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. TV'deki MediaDash uygulamasını açın ve sol taraftaki QR kodunu görün.\n" +
                            "2. Yukarıdaki 'TV'nin QR Kodunu Tara' butonuna basarak kameranızı TV'ye doğrultun.\n" +
                            "3. Bağlantı saniyeler içinde kurulur. Ardından Fotoğraf, Video veya Müzik seçin.\n" +
                            "4. Seçtiğiniz içerik TV diskine yüklenmeden doğrudan telefonunuz üzerinden TV ekranında canlı oynatılır.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

/**
 * Telefon Medya Seçim Kartı Bileşeni
 */
@Composable
private fun PhoneMediaActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TvSurfaceDark)
            .border(1.dp, TvSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
