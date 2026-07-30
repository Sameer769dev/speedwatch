package com.speedwatch.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
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

    val displayMetrics = context.resources.displayMetrics
    val adWidthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()

    val adaptiveAdSize = remember(adWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
    }

    var isAdLoaded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setAdSize(adaptiveAdSize)
                        adUnitId = app.adManager.bannerAdUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                isAdLoaded = true
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                isAdLoaded = false
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                    }
                }
            )
        }
    }
}
