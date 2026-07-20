package com.speedwatch.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val key: NavKey, val label: String, val icon: ImageVector) {
    object Dashboard : BottomNavItem(NavKey.Dashboard, "Dashboard", Icons.Default.Dashboard)
    object Lab : BottomNavItem(NavKey.Diagnostics, "Lab", Icons.Default.Science)
    object Reports : BottomNavItem(NavKey.Reports, "Reports", Icons.Default.Assessment)
    object History : BottomNavItem(NavKey.History, "History", Icons.Default.History)
    object Settings : BottomNavItem(NavKey.Settings, "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Lab,
    BottomNavItem.Reports,
    BottomNavItem.History,
    BottomNavItem.Settings
)
