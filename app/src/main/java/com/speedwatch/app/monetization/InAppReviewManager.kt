package com.speedwatch.app.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

class InAppReviewManager(private val context: Context) {

    private val TAG = "InAppReviewManager"
    private val reviewManager = ReviewManagerFactory.create(context)

    fun launchInAppReview(activity: Activity, onComplete: () -> Unit = {}) {
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    Log.d(TAG, "In-app review flow completed.")
                    onComplete()
                }
            } else {
                Log.e(TAG, "Failed to request in-app review flow: ${task.exception?.message}")
                onComplete()
            }
        }
    }

    fun promptIfUserSatisfied(activity: Activity, downloadMbps: Double, totalTestCount: Int) {
        // Satisfaction criteria: User completed 3+ tests and has a decent speed (> 5 Mbps)
        if (totalTestCount >= 3 && downloadMbps >= 5.0) {
            launchInAppReview(activity)
        }
    }
}
