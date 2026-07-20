package com.speedwatch.app.ui.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey as BaseNavKey

@Serializable
sealed interface NavKey : BaseNavKey {
    @Serializable
    data object Onboarding : NavKey
    @Serializable
    data object Dashboard : NavKey
    @Serializable
    data object History : NavKey
    @Serializable
    data class HistoryDetail(val logId: Long) : NavKey
    @Serializable
    data object Reports : NavKey
    @Serializable
    data object Settings : NavKey
    @Serializable
    data object Premium : NavKey
    @Serializable
    data object Diagnostics : NavKey
}
