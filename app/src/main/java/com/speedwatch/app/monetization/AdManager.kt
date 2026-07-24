package com.speedwatch.app.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {

    private val TAG = "AdManager"

    // Official AdMob Test Ad Unit IDs
    val bannerAdUnitId = "ca-app-pub-3940256099942544/6300978111"
    private val interstitialAdUnitId = "ca-app-pub-3940256099942544/1033173712"
    private val rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob SDK Initialized: ${status.adapterStatusMap}")
            preloadInterstitialAd()
            preloadRewardedAd()
        }
    }

    fun preloadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            interstitialAdUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial Ad Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial Ad Failed to load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            interstitialAd = null
            preloadInterstitialAd() // Reload for next time
            onAdDismissed()
        } else {
            preloadInterstitialAd()
            onAdDismissed()
        }
    }

    fun preloadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            rewardedAdUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded Ad Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded Ad Failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onRewardGranted: () -> Unit, onAdFailed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardGranted()
            }
            rewardedAd = null
            preloadRewardedAd()
        } else {
            preloadRewardedAd()
            onAdFailed()
        }
    }
}
