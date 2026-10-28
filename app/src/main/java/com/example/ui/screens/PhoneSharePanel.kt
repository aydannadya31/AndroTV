package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MediaType
import com.example.service.PairingState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.EmeraldLight
import android.content.Intent
import com.example.PairingActivity
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TvBackgroundDark
import com.example.ui.theme.TvSurfaceBorder
import com.example.ui.theme.TvSurfaceDark
import com.example.ui.theme.TvSurfaceElevated

@Composable
fun PhoneSharePanel(
    uiState: DashboardUiState,
    onCastMedia: (title: String, type: MediaType, uriOrData: String) -> Unit,
    onPairWithTv: (String) -> Unit,
    onUnpair: () -> Unit,
    onUpdateTvIp: (String) -> Unit,
    onSwitchToTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tvIpInput by remember { mutableStateOf(uiState.targetTvIp) }
    var pinInput by remember { mutableStateOf("") }
    val isPaired = uiState.activePhoneSession != null || uiState.pairingState == PairingState.PAIRED

    val qrScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pairedIp = result.data?.getStringExtra("PAIRED_TV_IP")
            val pairedPin = result.data?.getStringExtra("PAIRED_PIN")
            if (!pairedIp.isNullOrEmpty()) {
                tvIpInput = pairedIp
                onUpdateTvIp(pairedIp)
            }
            if (!pairedPin.isNullOrEmpty()) {
                pinInput = pairedPin
                onPairWithTv(pairedPin)
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Telefondan Fotoğraf"
            onCastMedia(fileName, MediaType.PHOTO, uri.toString())
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Telefondan Ses Dosyası"
            onCastMedia(fileName, MediaType.MUSIC, uri.toString())
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Telefondan Video Dosyası"
            onCastMedia(fileName, MediaType.MOVIE, uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackgroundDark)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Üst Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
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
                            text = "Güvenli Medya Aktarıcısı",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Socket & PIN Protokollü Yayın",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = onSwitchToTvMode,
                    colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "TV Modu",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "TV Görünümü", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        // GÜVENLİ EŞLEŞTİRME KARTI
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(TvSurfaceDark)
                    .border(
                        width = 1.dp,
                        color = if (isPaired) EmeraldPrimary else TvSurfaceBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPaired) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPaired) EmeraldPrimary else AccentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPaired) "Güvenli Eşleşme Aktif" else "TV ile Eşleşme Gerekli",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPaired) EmeraldPrimary.copy(alpha = 0.2f) else AccentAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isPaired) "TCP Socket :8095" else "Korumasız",
                            color = if (isPaired) EmeraldLight else AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isPaired) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tvIpInput,
                            onValueChange = {
                                tvIpInput = it
                                onUpdateTvIp(it)
                            },
                            label = { Text("TV IP", color = TextSecondary, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = TvSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1.3f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 6) pinInput = it },
                            label = { Text("TV PIN", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text(uiState.tvPairCode, color = TextMuted, fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = TvSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val pinToUse = if (pinInput.isNotEmpty()) pinInput else uiState.tvPairCode
                                onPairWithTv(pinToUse)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            if (uiState.pairingState == PairingState.PAIRING_IN_PROGRESS) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Eşleş",
                                    tint = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val localContext = LocalContext.current
                    Button(
                        onClick = {
                            val intent = Intent(localContext, PairingActivity::class.java).apply {
                                putExtra("INITIAL_MODE", "PHONE_SCANNER")
                            }
                            qrScanLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Kod Tara",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TV QR Kodunu Kamerayla Tara",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.pairingErrorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.pairingErrorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bağlı Cihaz: MediaDash TV (${uiState.targetTvIp})",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Oturum Token: ${uiState.activePhoneSession?.sessionToken?.take(18) ?: "mdhp_token_verified"}...",
                                color = EmeraldLight,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = onUnpair,
                            colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceElevated),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bağlantıyı Kes", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // CANLI PARÇALI VERİ İLETİM İLERLEME KARTI
        item {
            AnimatedVisibility(
                visible = uiState.activeTransferProgress != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val progress = uiState.activeTransferProgress
                if (progress != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TvSurfaceElevated)
                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (progress.isComplete) Icons.Default.CheckCircle else Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = progress.fileName,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${progress.progressPercent}%",
                                color = EmeraldLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = EmeraldPrimary,
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${progress.bytesTransferred / 1024} KB / ${progress.totalBytes / 1024} KB",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Hız: %.1f KB/s (TCP Socket)".format(progress.transferSpeedKbps),
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Medya Seçim Başlığı
        item {
            Text(
                text = "GÜVENLİ SOCKET İLE TV'YE YANSIT",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Medya Seçim Kartları
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShareActionCard(
                    title = "Fotoğraf",
                    subtitle = "Slayt & Galeri",
                    icon = Icons.Default.Image,
                    accentColor = AccentCyan,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                ShareActionCard(
                    title = "Ses / Müzik",
                    subtitle = "Hi-Res Akış",
                    icon = Icons.Default.Audiotrack,
                    accentColor = EmeraldPrimary,
                    onClick = {
                        audioPickerLauncher.launch("audio/*")
                    },
                    modifier = Modifier.weight(1f)
                )

                ShareActionCard(
                    title = "Video",
                    subtitle = "4K / Donanımsal",
                    icon = Icons.Default.Videocam,
                    accentColor = AccentAmber,
                    onClick = {
                        videoPickerLauncher.launch("video/*")
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Hızlı Socket Testleri
        item {
            Text(
                text = "HIZLI SOCKET TEST AKIŞLARI",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCastRow(
                    title = "4K Resim Paketi: Manzara Albümü",
                    typeDesc = "Parçalı Socket Aktarımı (3.2 MB)",
                    icon = Icons.Default.Image,
                    color = AccentCyan,
                    isLoading = uiState.isPhoneCasting,
                    onSend = {
                        onCastMedia(
                            "4K Resim Paketi: Manzara Albümü",
                            MediaType.PHOTO,
                            "socket://stream/photo_4k_album.jpg"
                        )
                    }
                )

                QuickCastRow(
                    title = "Hi-Res Müzik: Stüdyo Kaydı (FLAC)",
                    typeDesc = "Kayıpsız Ses Soket Akışı (18.4 MB)",
                    icon = Icons.Default.Audiotrack,
                    color = EmeraldPrimary,
                    isLoading = uiState.isPhoneCasting,
                    onSend = {
                        onCastMedia(
                            "Hi-Res Müzik: Stüdyo Kaydı (FLAC)",
                            MediaType.MUSIC,
                            "socket://stream/studio_recording_hi_res.flac"
                        )
                    }
                )

                QuickCastRow(
                    title = "4K 60FPS Video: Tatil Klibi",
                    typeDesc = "Parçalı TCP İletimi (45.0 MB)",
                    icon = Icons.Default.Videocam,
                    color = AccentAmber,
                    isLoading = uiState.isPhoneCasting,
                    onSend = {
                        onCastMedia(
                            "4K 60FPS Video: Tatil Klibi",
                            MediaType.MOVIE,
                            "socket://stream/vacation_4k_60fps.mp4"
                        )
                    }
                )
            }
        }

        // Bildirim
        if (uiState.castNotification != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary.copy(alpha = 0.2f))
                        .border(1.dp, EmeraldPrimary, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = uiState.castNotification,
                        color = EmeraldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ShareActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurfaceDark)
            .border(1.dp, TvSurfaceBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun QuickCastRow(
    title: String,
    typeDesc: String,
    icon: ImageVector,
    color: Color,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TvSurfaceDark)
            .border(1.dp, TvSurfaceBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onSend)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = typeDesc,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(EmeraldPrimary.copy(alpha = 0.15f))
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Gönder",
                tint = EmeraldPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
