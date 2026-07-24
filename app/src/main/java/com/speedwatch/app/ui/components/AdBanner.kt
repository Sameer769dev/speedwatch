package com.speedwatch.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.speedwatch.app.SpeedWatchApplication

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    val settings by app.repository.ispSettings.collectAsState(initial = null)

    if (settings?.isPremium == true) {
        Spacer(modifier = Modifier.height(0.dp))
    } else {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = app.adManager.bannerAdUnitId
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
