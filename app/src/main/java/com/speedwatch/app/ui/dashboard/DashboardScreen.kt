package com.speedwatch.app.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.ui.components.AdBanner
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(onNavigateToPremium: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (!notificationPermissionState.status.isGranted) {
            LaunchedEffect(Unit) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    val viewModel: DashboardViewModel = viewModel {
        DashboardViewModel(app.repository, app.speedMeasurer, app.networkInfoProvider, app.notificationHelper)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    val settings by app.repository.ispSettings.collectAsState(initial = null)
    
    var showTooltip by remember { mutableStateOf<String?>(null) }
    var showProNag by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            SpeedWatchTopBar(
                title = "Dashboard",
                actions = {
                    if (settings?.isPremium != true) {
                        IconButton(onClick = onNavigateToPremium) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = "Go Pro",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            ) 
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content area (Non-Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val currentSpeed = when (val state = uiState) {
                    is DashboardUiState.Testing -> state.lastResult
                    is DashboardUiState.Success -> state.download
                    else -> 0.0
                }.coerceAtLeast(0.0)
                
                // Speedometer slightly smaller to fit
                Speedometer(speed = currentSpeed)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "ResultAnimation"
                ) { state ->
                    if (state is DashboardUiState.Success) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                ResultColumn("Download", state.download, "Mbps") {
                                    showTooltip = "Download speed is how fast you can pull data from the internet."
                                }
                                ResultColumn("Upload", state.upload, "Mbps") {
                                    showTooltip = "Upload speed is how fast you can send data to the internet."
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                ResultColumn("Ping", state.latency.toDouble(), "ms") {
                                    showTooltip = "Latency (Ping) is the delay in your connection. Lower is better for gaming."
                                }
                                ResultColumn("Jitter", state.jitter, "ms") {
                                    showTooltip = "Jitter measures the stability of your ping. High jitter causes lag spikes."
                                }
                            }
                            
                            StabilityBadge(jitter = state.jitter)
                        }
                    } else if (state is DashboardUiState.Testing) {
                        Text(
                            text = "Measuring ${state.stage}...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "Ready to test",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.runSpeedTest() },
                    enabled = uiState !is DashboardUiState.Testing,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.height(56.dp).fillMaxWidth(0.7f),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        if (uiState is DashboardUiState.Testing) "Testing..." else "Start Speed Test",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                if (uiState is DashboardUiState.Error) {
                    Text(
                        text = (uiState as DashboardUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            // Bottom Anchored Ad Banner (Fixed above nav bar)
            if (settings?.isPremium != true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AdBanner(modifier = Modifier.padding(vertical = 4.dp))
                            
                            IconButton(
                                onClick = { showProNag = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Ads",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTooltip != null) {
        AlertDialog(
            onDismissRequest = { showTooltip = null },
            confirmButton = { TextButton(onClick = { showTooltip = null }) { Text("Got it") } },
            title = { Text("Network Insight") },
            text = { Text(showTooltip!!) }
        )
    }

    if (showProNag) {
        AlertDialog(
            onDismissRequest = { showProNag = false },
            confirmButton = { 
                Button(onClick = { 
                    showProNag = false
                    onNavigateToPremium()
                }) { Text("Go Pro") } 
            },
            dismissButton = {
                TextButton(onClick = { showProNag = false }) { Text("Maybe Later") }
            },
            title = { Text("Remove Advertisements") },
            text = { Text("Upgrade to SpeedWatch Pro to remove ads, unlock unlimited history, and get hourly background checks.") }
        )
    }
}

@Composable
fun ResultColumn(label: String, value: Double, unit: String, onInfoClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            IconButton(onClick = onInfoClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
        Text(
            text = if (unit == "ms") value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(text = unit, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StabilityBadge(jitter: Double) {
    val (text, color) = when {
        jitter < 5 -> "Rock Solid" to Color(0xFF4CAF50)
        jitter < 15 -> "Stable" to Color(0xFF8BC34A)
        jitter < 30 -> "Unstable" to Color(0xFFFFC107)
        else -> "Very Jittery" to Color(0xFFF44336)
    }
    
    Surface(
        modifier = Modifier.padding(top = 12.dp),
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = stringResource(R.string.stability_label, text),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun Speedometer(speed: Double, maxSpeed: Double = 100.0) {
    val animatedSpeed by animateFloatAsState(targetValue = speed.toFloat(), label = "SpeedAnimation")
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondaryContainer
    
    // Increased container size and added internal padding to prevent needle/stroke clipping
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp).padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            // radius calculated based on padded size
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Draw background arc
            drawArc(
                color = secondaryColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Draw speed arc
            val sweepAngle = (animatedSpeed / maxSpeed.toFloat()) * 270f
            drawArc(
                color = primaryColor,
                startAngle = 135f,
                sweepAngle = sweepAngle.coerceAtMost(270f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Draw needle
            val angleRad = (135f + sweepAngle.coerceAtMost(270f)) * (PI / 180f)
            val needleLength = radius * 0.8f
            val startX = size.width / 2
            val startY = size.height / 2
            val endX = startX + cos(angleRad).toFloat() * needleLength
            val endY = startY + sin(angleRad).toFloat() * needleLength
            
            drawLine(
                color = primaryColor,
                start = center,
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "SPEED", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text = String.format(Locale.getDefault(), "%.1f", animatedSpeed), fontSize = 48.sp, fontWeight = FontWeight.Black)
            Text(text = "Mbps", style = MaterialTheme.typography.labelSmall)
        }
    }
}
