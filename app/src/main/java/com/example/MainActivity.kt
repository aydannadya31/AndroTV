package com.example

import android.app.UiModeManager
import android.content.res.Configuration
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
import com.example.ui.screens.TvDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TvBackgroundDark

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: MainDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cihaz türünü dinamik tespit et (Android TV kutusu mu yoksa dokunmatik telefon mu?)
        val uiModeManager = getSystemService(UI_MODE_SERVICE) as? UiModeManager
        val isTelevision = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
                !packageManager.hasSystemFeature("android.hardware.touchscreen")

        if (isTelevision) {
            dashboardViewModel.setDeviceMode(DeviceMode.TV_HUB)
        } else {
            dashboardViewModel.setDeviceMode(DeviceMode.PHONE_SENDER)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TvBackgroundDark
                ) {
                    if (isTelevision) {
                        TvDashboardScreen(viewModel = dashboardViewModel)
                    } else {
                        PhoneHomeScreen(viewModel = dashboardViewModel)
                    }
                }
            }
        }
    }
}
