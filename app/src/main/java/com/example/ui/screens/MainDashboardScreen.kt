package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.QrCode
import android.content.Intent
import com.example.PairingActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MediaItemEntity
import com.example.ui.components.MediaPosterCard
import com.example.ui.components.SmartSensorCard
import com.example.ui.components.TvFocusableCard
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

@Composable
fun MainDashboardScreen(
    viewModel: MainDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Eğer uygulama telefonda çalıştırılıyorsa veya telefon moduna geçildiyse
    if (uiState.deviceMode == DeviceMode.PHONE_SENDER) {
        PhoneSharePanel(
            uiState = uiState,
            onCastMedia = viewModel::streamMediaOverSocket,
            onPairWithTv = viewModel::pairWithTv,
            onUnpair = viewModel::unpairPhone,
            onUpdateTvIp = viewModel::updateTargetTvIp,
            onSwitchToTvMode = viewModel::toggleDeviceMode,
            modifier = modifier
        )
    } else {
        // TV Bölünmüş Ekranı (Split-Dashboard)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TvBackgroundDark)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // SOL PANEL: SMART DASH
                SmartDashSection(
                    uiState = uiState,
                    onToggleSensor = viewModel::onToggleSensor,
                    onToggleCinemaMode = viewModel::toggleCinemaMode,
                    onRefreshScan = viewModel::triggerScan,
                    onRegeneratePin = viewModel::regenerateTvPin,
                    onSwitchToPhoneMode = viewModel::toggleDeviceMode,
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .background(TvSurfaceDark)
                )

                // Dikey Ayırıcı
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(TvSurfaceBorder)
                )

                // SAĞ PANEL: MEDYAKUTUSU
                MediaKutusuSection(
                    uiState = uiState,
                    onSelectTab = viewModel::onTabSelected,
                    onSelectMedia = viewModel::onMediaSelected,
                    onRefreshScan = viewModel::triggerScan,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Canlı Socket Veri İletimi TV HUD Göstergesi (Sağ Alt Köşe)
            AnimatedVisibility(
                visible = uiState.activeTransferProgress != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                val progress = uiState.activeTransferProgress
                if (progress != null) {
                    Box(
                        modifier = Modifier
                            .width(340.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Güvenli Socket Akışı",
                                        color = EmeraldLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "%${progress.progressPercent}",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = progress.fileName,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EmeraldPrimary,
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${progress.bytesTransferred / 1024} KB / ${progress.totalBytes / 1024} KB",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "%.1f KB/s (TCP:8095)".format(progress.transferSpeedKbps),
                                    color = AccentCyan,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Telefondan Yansıtma Anlık TV Bildirim Kartı (Pop-up Toast)
            AnimatedVisibility(
                visible = uiState.castNotification != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            ) {
                if (uiState.castNotification != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF131B2A))
                            .border(1.5.dp, EmeraldPrimary, RoundedCornerShape(14.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.castNotification ?: "",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.dismissCastNotification() }
                            )
                        }
                    }
                }
            }

            // Medya Oynatıcı Tam Ekran Katmanı
            if (uiState.playerState.activeItem != null) {
                VideoPlayerOverlay(
                    playerState = uiState.playerState,
                    onPlayPauseToggle = viewModel::togglePlayPause,
                    onSeekRelative = viewModel::seekRelative,
                    onClose = viewModel::closePlayer,
                    onToggleCinemaLights = viewModel::toggleCinemaMode
                )
            }
        }
    }
}

/**
 * Sol Panel: Smart Dash
 */
@Composable
fun SmartDashSection(
    uiState: DashboardUiState,
    onToggleSensor: (com.example.data.local.SmartSensorEntity) -> Unit,
    onToggleCinemaMode: () -> Unit,
    onRefreshScan: () -> Unit,
    onRegeneratePin: () -> Unit,
    onSwitchToPhoneMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MediaDash Başlığı ve Canlı MQTT Rozeti
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "MediaDash Hub",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MediaDash",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " Hub",
                        color = EmeraldPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "MQTT Çevrimiçi",
                        color = EmeraldLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // TV Güvenli Eşleştirme ve Socket Durum Kartı (PIN & QR Kod Açma)
        item {
            val context = LocalContext.current
            TvFocusableCard(
                onClick = {
                    val intent = Intent(context, PairingActivity::class.java).apply {
                        putExtra("INITIAL_MODE", "TV_DISPLAY")
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(14.dp),
                backgroundColor = Color(0xFF0F172A),
                focusedBorderColor = EmeraldPrimary,
                testTag = "btn_tv_pairing_card"
            ) { isFocused ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Kod",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "TV Güvenli Eşleşme (QR)",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.tvIpAddress}:8095 (TCP)",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldPrimary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.tvPairCode,
                                color = Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Tam Ekran QR",
                            tint = if (isFocused) EmeraldPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Büyük Saat ve Tarih Alanı
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                TvSurfaceElevated,
                                TvSurfaceDark
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = uiState.timeString,
                    color = TextPrimary,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = uiState.dateString,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Anlık Hava Durumu
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(TvSurfaceElevated)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Hava Durumu",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = uiState.weather.temp,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.weather.condition,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = uiState.weather.aqi,
                        color = EmeraldLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Nem: ${uiState.weather.humidity}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Akıllı Ev Sensörleri Başlığı
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EV DURUMU & SENSÖRLER",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${uiState.sensors.size} Cihaz",
                    color = AccentCyan,
                    fontSize = 11.sp
                )
            }
        }

        // Sensör Kartları
        items(uiState.sensors, key = { it.entityId }) { sensor ->
            SmartSensorCard(
                sensor = sensor,
                onToggle = { onToggleSensor(sensor) }
            )
        }

        // Hızlı Aksiyonlar ve Telefon Modu Geçişi
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = "HIZLI AKSİYONLAR",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvFocusableCard(
                        onClick = onToggleCinemaMode,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = if (uiState.isCinemaModeActive) EmeraldPrimary else TvSurfaceElevated,
                        testTag = "btn_quick_cinema"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Sinema",
                                tint = if (uiState.isCinemaModeActive) Color.Black else EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sinema Modu",
                                color = if (uiState.isCinemaModeActive) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    TvFocusableCard(
                        onClick = onRefreshScan,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = TvSurfaceElevated,
                        testTag = "btn_quick_refresh"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Yenile",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isScanning) "Taranıyor" else "Ağı Tara",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Telefona Özel Geçiş Butonu
                TvFocusableCard(
                    onClick = onSwitchToPhoneMode,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    backgroundColor = Color(0xFF1E293B),
                    testTag = "btn_switch_phone_mode"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = "Telefon Modu",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Telefon Medya Paylaşım Moduna Geç",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sağ Panel: MediaKutusu
 */
@Composable
fun MediaKutusuSection(
    uiState: DashboardUiState,
    onSelectTab: (MediaTabFilter) -> Unit,
    onSelectMedia: (MediaItemEntity) -> Unit,
    onRefreshScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Üst Bar: Kategori Filtre Çipleri ve Depolama Durumu
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kategori Çipleri (TV D-pad Odaklanabilir)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MediaTabFilter.values().forEach { tab ->
                        val isSelected = uiState.selectedTab == tab
                        TvFocusableCard(
                            onClick = { onSelectTab(tab) },
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = if (isSelected) EmeraldPrimary else TvSurfaceElevated,
                            focusedBorderColor = EmeraldLight,
                            testTag = "tab_${tab.name}"
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // Depolama ve Yansıtma Rozeti
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TvSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Ağ ve Telefon Yansıtma",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NAS • SMB • Socket Aktif",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // İzlemeye Devam Et (Resume) Bölümü
        if (uiState.resumeItems.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Devam Et",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kaldığınız Yerden Devam Edin",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.resumeItems, key = { "resume_${it.id}" }) { item ->
                            MediaPosterCard(
                                media = item,
                                onSelect = { onSelectMedia(item) },
                                cardWidth = 210.dp,
                                cardHeight = 150.dp
                            )
                        }
                    }
                }
            }
        }

        // Ana Medya Vitrini
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.selectedTab.title} (${uiState.displayedMedia.size})",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Hızlı İndeks",
                            tint = AccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4K / Donanımsal TV Çözümü",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.displayedMedia, key = { "media_${it.id}" }) { mediaItem ->
                        MediaPosterCard(
                            media = mediaItem,
                            onSelect = { onSelectMedia(mediaItem) },
                            cardWidth = 190.dp,
                            cardHeight = 270.dp
                        )
                    }
                }
            }
        }

        // İkinci Raf: 4K Koleksiyon ve Telefon Paylaşımları
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Ağ Paylaşımları & Yansıtılan Medyalar",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.displayedMedia.reversed(), key = { "rev_${it.id}" }) { mediaItem ->
                        MediaPosterCard(
                            media = mediaItem,
                            onSelect = { onSelectMedia(mediaItem) },
                            cardWidth = 190.dp,
                            cardHeight = 250.dp
                        )
                    }
                }
            }
        }
    }
}
