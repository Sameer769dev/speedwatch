package com.speedwatch.app.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.speedwatch.app.domain.NetworkDetails
import com.speedwatch.app.ui.components.AdBanner
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.theme.DarkNavy
import com.speedwatch.app.ui.theme.ElectricCyan
import com.speedwatch.app.ui.theme.BrightBlue
import com.speedwatch.app.ui.theme.DeepIndigo
import com.speedwatch.app.ui.theme.NeonGreen
import com.speedwatch.app.ui.theme.AmberWarning
import com.speedwatch.app.ui.theme.CoralRed
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
    val networkDetails by viewModel.networkDetails.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is DashboardUiState.Success && settings?.isPremium != true) {
            val activity = context as? android.app.Activity
            activity?.let {
                app.adManager.showInterstitialAd(it) {}
            }
        }
    }

    var showTooltip by remember { mutableStateOf<String?>(null) }
    var showProNag by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }

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
                                tint = ElectricCyan
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val currentSpeed = when (val state = uiState) {
                    is DashboardUiState.Testing -> state.lastResult
                    is DashboardUiState.Success -> state.download
                    else -> 0.0
                }.coerceAtLeast(0.0)

                Speedometer(
                    speed = currentSpeed,
                    isTesting = uiState is DashboardUiState.Testing
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400)) + slideInVertically { it / 2 }) togetherWith
                                (fadeOut(animationSpec = tween(300)) + slideOutVertically { -it / 2 })
                    },
                    label = "ResultAnimation"
                ) { state ->
                    if (state is DashboardUiState.Success) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Download",
                                    value = "%.1f".format(Locale.getDefault(), state.download),
                                    unit = "Mbps",
                                    icon = Icons.Default.ArrowDownward,
                                    iconColor = ElectricCyan,
                                    onClickInfo = {
                                        showTooltip = "Download speed measures how quickly data transfers to your device."
                                    }
                                )
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Upload",
                                    value = "%.1f".format(Locale.getDefault(), state.upload),
                                    unit = "Mbps",
                                    icon = Icons.Default.ArrowUpward,
                                    iconColor = BrightBlue,
                                    onClickInfo = {
                                        showTooltip = "Upload speed measures how fast data is sent from your device."
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Ping",
                                    value = "${state.latency}",
                                    unit = "ms",
                                    icon = Icons.Default.Timer,
                                    iconColor = DeepIndigo,
                                    onClickInfo = {
                                        showTooltip = "Ping (latency) is connection response delay. Lower values mean smoother gaming & calls."
                                    }
                                )
                                MetricCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Jitter",
                                    value = "%.1f".format(Locale.getDefault(), state.jitter),
                                    unit = "ms",
                                    icon = Icons.Default.Equalizer,
                                    iconColor = NeonGreen,
                                    onClickInfo = {
                                        showTooltip = "Jitter measures ping stability over time. Low jitter prevents lag spikes."
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            StabilityBadge(jitter = state.jitter)

                            if (settings?.isPremium == true) {
                                TextButton(
                                    onClick = { showInsights = !showInsights },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showInsights) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (showInsights) "Hide Technical Insights" else "Show Technical Insights",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                AnimatedVisibility(
                                    visible = showInsights,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    TechnicalInsightsCard(networkDetails)
                                }
                            }
                        }
                    } else if (state is DashboardUiState.Testing) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = ElectricCyan.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ElectricCyan
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Measuring ${state.stage}...",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tap below to run a speed test",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isTesting = uiState is DashboardUiState.Testing
                Button(
                    onClick = { viewModel.runSpeedTest() },
                    enabled = !isTesting,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color(0xFF00363D)
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.75f),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isTesting) Icons.Default.HourglassTop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTesting) "Testing..." else "Start Speed Test",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (uiState is DashboardUiState.Error) {
                    Text(
                        text = (uiState as DashboardUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showTooltip != null) {
        AlertDialog(
            onDismissRequest = { showTooltip = null },
            confirmButton = {
                TextButton(onClick = { showTooltip = null }) { Text("Got it") }
            },
            title = { Text("Network Insight", fontWeight = FontWeight.Bold) },
            text = { Text(showTooltip!!, style = MaterialTheme.typography.bodyMedium) }
        )
    }

    if (showProNag) {
        AlertDialog(
            onDismissRequest = { showProNag = false },
            confirmButton = {
                Button(
                    onClick = {
                        showProNag = false
                        onNavigateToPremium()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF00363D))
                ) { Text("Go Pro") }
            },
            dismissButton = {
                TextButton(onClick = { showProNag = false }) { Text("Maybe Later") }
            },
            title = { Text("Remove Advertisements", fontWeight = FontWeight.Bold) },
            text = { Text("Upgrade to SpeedWatch Pro to remove ads, unlock unlimited history, and enable automatic background checks.") }
        )
    }
}

@Composable
fun NetworkHealthCard(details: NetworkDetails?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val pulseInfinite = rememberInfiniteTransition(label = "Pulse")
            val pulseAlpha by pulseInfinite.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "AlphaPulse"
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (details?.isValidated == true) NeonGreen.copy(alpha = 0.15f) else BrightBlue.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (details?.transport) {
                        "Wi-Fi" -> Icons.Default.Wifi
                        "Cellular" -> Icons.Default.NetworkCheck
                        else -> Icons.Default.Speed
                    },
                    contentDescription = null,
                    tint = if (details?.isValidated == true) NeonGreen else BrightBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (details?.isValidated == true) NeonGreen.copy(alpha = pulseAlpha) else AmberWarning
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (details?.isValidated == true) stringResource(R.string.connected_validated) else stringResource(R.string.checking_connection),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details?.let { "${it.transport} • ${it.detailInfo}" } ?: stringResource(R.string.identifying_network),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClickInfo: () -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onClickInfo() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun StabilityBadge(jitter: Double) {
    val (text, color) = when {
        jitter < 5 -> "Rock Solid" to NeonGreen
        jitter < 15 -> "Stable" to BrightBlue
        jitter < 30 -> "Unstable" to AmberWarning
        else -> "Very Jittery" to CoralRed
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.stability_label, text),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TechnicalInsightsCard(details: NetworkDetails?) {
    if (details == null) return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Technical Insights",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            InsightRow("Local IP", details.localIp ?: "Unknown")
            InsightRow("Interface", details.interfaceName ?: "Unknown")
            if (details.isVpn) {
                InsightRow("VPN", "Active", color = CoralRed)
            }
            if (details.dnsServers.isNotEmpty()) {
                InsightRow("Primary DNS", details.dnsServers.first())
            }
            InsightRow("Hardware Cap", "%.0f Mbps".format(details.bandwidthDownKbps / 1000.0))
        }
    }
}

@Composable
fun InsightRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun Speedometer(speed: Double, maxSpeed: Double = 100.0, isTesting: Boolean = false) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "SpeedAnimation"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(270.dp)
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 22.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val centerOffset = Offset(size.width / 2, size.height / 2)

            // Draw Background Track Arc
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw Gradient Speed Sweep Arc
            val sweepAngle = ((animatedSpeed / maxSpeed.toFloat()) * 270f).coerceIn(0f, 270f)
            if (sweepAngle > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to ElectricCyan,
                        0.5f to BrightBlue,
                        1.0f to DeepIndigo
                    ),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Draw Gauge Needle
            val angleRad = (135f + sweepAngle) * (PI / 180f)
            val needleLength = radius * 0.75f
            val endX = centerOffset.x + cos(angleRad).toFloat() * needleLength
            val endY = centerOffset.y + sin(angleRad).toFloat() * needleLength

            // Needle shadow & main line
            drawLine(
                color = ElectricCyan,
                start = centerOffset,
                end = Offset(endX, endY),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle Center Hub
            drawCircle(
                color = ElectricCyan,
                radius = 8.dp.toPx(),
                center = centerOffset
            )
            drawCircle(
                color = DarkNavy,
                radius = 4.dp.toPx(),
                center = centerOffset
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            Text(
                text = "SPEED",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = String.format(Locale.getDefault(), "%.1f", animatedSpeed),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Mbps",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
        }
    }
}

