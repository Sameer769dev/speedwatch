package com.speedwatch.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.speedwatch.app.SpeedWatchApplication
import androidx.navigation3.runtime.NavKey as BaseNavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.speedwatch.app.ui.components.AdBanner
import com.speedwatch.app.ui.dashboard.DashboardScreen
import com.speedwatch.app.ui.diagnostics.DiagnosticsScreen
import com.speedwatch.app.ui.history.HistoryDetailScreen
import com.speedwatch.app.ui.history.HistoryScreen
import com.speedwatch.app.ui.navigation.NavKey
import com.speedwatch.app.ui.navigation.bottomNavItems
import com.speedwatch.app.ui.onboarding.OnboardingScreen
import com.speedwatch.app.ui.premium.PremiumScreen
import com.speedwatch.app.ui.reports.ReportsScreen
import com.speedwatch.app.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SpeedWatchApp() {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    
    // Explicitly track both the settings value and whether we've received the first emission
    var isInitialized by remember { mutableStateOf(false) }
    val settings by produceState<com.speedwatch.app.data.model.IspSettings?>(initialValue = null) {
        app.repository.ispSettings.collect {
            value = it
            isInitialized = true
        }
    }
    
    val backStack = rememberNavBackStack(NavKey.Dashboard as BaseNavKey)
    val listDetailStrategy = rememberListDetailSceneStrategy<BaseNavKey>()

    // Handle deep link navigation events
    LaunchedEffect(Unit) {
        app.navigationEvents.collect { route ->
            if (route == "premium") {
                backStack.add(NavKey.Premium)
            }
        }
    }

    val myEntryProvider = entryProvider<BaseNavKey> {
        addEntryProvider(NavKey.Onboarding as BaseNavKey) {
            OnboardingScreen(
                onComplete = {
                    backStack.clear()
                    backStack.add(NavKey.Dashboard)
                },
                onNavigateToPremium = {
                    backStack.clear()
                    backStack.add(NavKey.Dashboard)
                    backStack.add(NavKey.Premium)
                }
            )
        }
        
        addEntryProvider(NavKey.Dashboard as BaseNavKey) { 
            DashboardScreen(onNavigateToPremium = { backStack.add(NavKey.Premium) }) 
        }
        
        addEntryProvider(
            key = NavKey.History as BaseNavKey,
            metadata = ListDetailSceneStrategy.listPane(
                detailPlaceholder = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Select a log to see details", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    }
                }
            )
        ) { 
            HistoryScreen(
                onLogClick = { id -> backStack.add(NavKey.HistoryDetail(id)) },
                onNavigateToPremium = { backStack.add(NavKey.Premium) }
            ) 
        }
        
        addEntryProvider(
            clazz = NavKey.HistoryDetail::class,
            metadata = ListDetailSceneStrategy.detailPane()
        ) { key ->
            HistoryDetailScreen(logId = key.logId, onBack = { backStack.removeAt(backStack.size - 1) })
        }

        addEntryProvider(NavKey.Settings as BaseNavKey) { 
            SettingsScreen(onNavigateToPremium = { backStack.add(NavKey.Premium) }) 
        }
        
        addEntryProvider(NavKey.Reports as BaseNavKey) { 
            ReportsScreen(
                onNavigateToPremium = { backStack.add(NavKey.Premium) },
                onRunTest = {
                    backStack.clear()
                    backStack.add(NavKey.Dashboard)
                }
            ) 
        }

        addEntryProvider(NavKey.Diagnostics as BaseNavKey) { DiagnosticsScreen() }

        addEntryProvider(NavKey.Premium as BaseNavKey) {
            PremiumScreen(onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) })
        }
    }

    val currentKey = backStack.lastOrNull()
    val showBottomBar = currentKey != NavKey.Onboarding

    // Redirection logic: Wait for initialization before redirecting to onboarding
    LaunchedEffect(isInitialized, settings) {
        if (isInitialized && settings == null && currentKey != NavKey.Onboarding) {
            backStack.clear()
            backStack.add(NavKey.Onboarding)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar && isInitialized) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AdBanner()
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentKey == item.key || 
                                (item.key == NavKey.History && currentKey is NavKey.HistoryDetail)
                                
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (!isSelected) {
                                        backStack.clear()
                                        backStack.add(item.key as BaseNavKey)
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Only show navigation once we've checked settings to avoid flicker
            if (isInitialized) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize(),
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                    entryProvider = myEntryProvider,
                    sceneStrategy = listDetailStrategy,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(500)) + slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(500)
                        ) togetherWith fadeOut(animationSpec = tween(500)) + slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(500)
                        )
                    },
                    popTransitionSpec = {
                        fadeIn(animationSpec = tween(500)) + slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(500)
                        ) togetherWith fadeOut(animationSpec = tween(500)) + slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(500)
                        )
                    }
                )
            } else {
                // Optional: Show a loading indicator or just empty space while initializing
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
