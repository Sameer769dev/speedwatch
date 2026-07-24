package com.speedwatch.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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

    // Hide banner completely if user is Pro/Premium
    if (settings?.isPremium == true) {
        return
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val adaptiveAdSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }

    val adHeightDp = remember(adaptiveAdSize) {
        adaptiveAdSize.height.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(adHeightDp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdSize(adaptiveAdSize)
                        adUnitId = app.adManager.bannerAdUnitId
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
