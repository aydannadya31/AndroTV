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
import com.example.ui.screens.MainDashboardScreen
import com.example.ui.screens.MainDashboardViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBackgroundDark

/**
 * Android Akıllı Telefonlar İçin Özel Medya İletici ve Bağlantı Aktivitesi.
 */
class PhoneActivity : ComponentActivity() {

    private val dashboardViewModel: MainDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Doğrudan Telefon Medya Gönderici Modunu Başlat
        dashboardViewModel.setDeviceMode(DeviceMode.PHONE_SENDER)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBackgroundDark
                ) {
                    MainDashboardScreen(viewModel = dashboardViewModel)
                }
            }
        }
    }
}
