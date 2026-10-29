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
import com.example.ui.screens.TvDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBackgroundDark

/**
 * Android TV Cihazları İçin Özel Alıcı, Görüntüleme ve Ana Dashboard Aktivitesi.
 * Sadece TV'ye özgü 10-foot arayüzü, D-pad kumanda kontrolünü ve eşleştirme QR kodunu barındırır.
 */
class TvActivity : ComponentActivity() {

    private val dashboardViewModel: MainDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TV Alıcı Modunu Başlat
        dashboardViewModel.setDeviceMode(DeviceMode.TV_HUB)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBackgroundDark
                ) {
                    TvDashboardScreen(viewModel = dashboardViewModel)
                }
            }
        }
    }
}
