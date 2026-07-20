package com.speedwatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.android.gms.ads.MobileAds
import com.speedwatch.app.ui.SpeedWatchApp
import com.speedwatch.app.ui.notifications.NotificationHelper
import com.speedwatch.app.ui.theme.SpeedWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        MobileAds.initialize(this) {}
        
        handleIntent(intent)
        
        enableEdgeToEdge()
        val app = application as SpeedWatchApplication
        
        setContent {
            val settings by app.repository.ispSettings.collectAsState(initial = null)
            val isDark = when (settings?.themePreference) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }
            
            SpeedWatchTheme(darkTheme = isDark) {
                SpeedWatchApp()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        intent?.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO)?.let { route ->
            (application as SpeedWatchApplication).triggerNavigation(route)
        }
    }
}
