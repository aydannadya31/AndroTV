package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.DeviceMode
import com.example.ui.screens.MainDashboardViewModel
import com.example.ui.screens.PhoneHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBackgroundDark

/**
 * Android Akıllı Telefonlar İçin Özel Medya İletici ve TV Bağlantı Aktivitesi.
 * QR kamera tarayıcı, galeri/ses seçici ve sıfır depolama ile canlı TV aktarımını barındırır.
 */
class PhoneActivity : ComponentActivity() {

    private val dashboardViewModel: MainDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Telefon Medya Gönderici Modunu Başlat
        dashboardViewModel.setDeviceMode(DeviceMode.PHONE_SENDER)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBackgroundDark
                ) {
                    PhoneHomeScreen(viewModel = dashboardViewModel)
                }
            }
        }
    }
}
