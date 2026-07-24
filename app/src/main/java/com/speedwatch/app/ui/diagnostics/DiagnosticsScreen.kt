package com.speedwatch.app.ui.diagnostics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.data.model.LabAudit
import com.speedwatch.app.domain.BufferbloatResult
import com.speedwatch.app.domain.DnsAuditResult
import com.speedwatch.app.domain.ThrottlingStatus
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.theme.AmberWarning
import com.speedwatch.app.ui.theme.BrightBlue
import com.speedwatch.app.ui.theme.CoralRed
import com.speedwatch.app.ui.theme.DeepIndigo
import com.speedwatch.app.ui.theme.ElectricCyan
import com.speedwatch.app.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiagnosticsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: DiagnosticsViewModel = viewModel {
        DiagnosticsViewModel(app.repository, app.speedMeasurer, app.networkInfoProvider, app.notificationHelper, context)
    }

    val labState by viewModel.labState.collectAsState()
    val dataUsage by viewModel.dataUsage.collectAsState()
    val recentAudits by viewModel.recentAudits.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val runWithAdCheck = { action: () -> Unit ->
        if (settings?.isPremium == true) {
            action()
        } else {
            val activity = context as? android.app.Activity
            if (activity != null) {
                app.adManager.showRewardedAd(
                    activity = activity,
                    onRewardGranted = action,
                    onAdFailed = action
                )
            } else {
                action()
            }
        }
    }

    Scaffold(
        topBar = { SpeedWatchTopBar(stringResource(R.string.lab)) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                DataUsageCard(
                    bytes = dataUsage,
                    capMB = settings?.dataUsageCapMB ?: 0,
                    isPremium = settings?.isPremium == true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Technical Lab Tests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }

            item {
                LabTestCard(
                    title = "ISP Throttling Check",
                    description = "Compares hardware Wi-Fi/Cell link vs actual throughput.",
                    icon = Icons.Default.Speed,
                    accentColor = ElectricCyan,
                    onRun = { runWithAdCheck { viewModel.runThrottlingTest() } }
                )
            }

            item {
                LabTestCard(
                    title = "Bufferbloat Analysis",
                    description = "Measures 90th percentile latency spike under heavy load.",
                    icon = Icons.Default.NetworkCheck,
                    accentColor = BrightBlue,
                    onRun = { runWithAdCheck { viewModel.runBufferbloatTest() } }
                )
            }

            item {
                LabTestCard(
                    title = "DNS Resolution Audit",
                    description = "Fires raw UDP queries to Cloudflare & Google resolvers.",
                    icon = Icons.Default.Dns,
                    accentColor = DeepIndigo,
                    onRun = { runWithAdCheck { viewModel.runDnsAudit() } }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Activity Benchmarks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan
                )
            }

            item {
                LabTestCard(
                    title = "Streaming Quality Audit",
                    description = "Validates 4K / 8K video streaming capacity.",
                    icon = Icons.Default.Movie,
                    accentColor = NeonGreen,
                    onRun = { viewModel.runStreamingAudit() }
                )
            }

            item {
                LabTestCard(
                    title = "Video Call Health",
                    description = "Assesses stability for Zoom, Teams & Google Meet.",
                    icon = Icons.Default.VideoCall,
                    accentColor = AmberWarning,
                    onRun = { viewModel.runVideoCallAudit() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audit History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (recentAudits.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAuditHistory() }) {
                            Text("Clear All", color = CoralRed, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (recentAudits.isEmpty()) {
                item {
                    Text(
                        text = "No audits performed yet. Run a test above to generate a signed certificate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recentAudits) { audit ->
                    AuditHistoryItem(audit)
                }
            }
        }
    }

    if (labState is LabUiState.Running) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Lab Test Running", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = ElectricCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text((labState as LabUiState.Running).message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }

    if (labState is LabUiState.RequireMobileData) {
        AlertDialog(
            onDismissRequest = { viewModel.reset() },
            confirmButton = {
                TextButton(onClick = { viewModel.reset() }) { Text("OK") }
            },
            title = { Text("Mobile Data Required", fontWeight = FontWeight.Bold) },
            text = { Text("Advanced Lab tests are designed for Cellular networks. Please disable Wi-Fi and enable Mobile Data to continue.") }
        )
    }

    if (labState !is LabUiState.Idle && labState !is LabUiState.Running && labState !is LabUiState.RequireMobileData) {
        ResultDialog(state = labState, onDismiss = { viewModel.reset() })
    }
}

@Composable
fun LabTestCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onRun: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRun,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color(0xFF00363D)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("Start", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AuditHistoryItem(audit: LabAudit) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = audit.testType,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = audit.mainResult,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sdf.format(Date(audit.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Audit Certificate",
                tint = NeonGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun DataUsageCard(bytes: Long, capMB: Int, isPremium: Boolean) {
    val usedMB = bytes / (1024.0 * 1024.0)
    val hasCap = isPremium && capMB > 0
    val progress = if (hasCap) (usedMB / capMB).toFloat().coerceIn(0f, 1f) else 0f

    val progressColor = when {
        progress >= 1f -> CoralRed
        progress >= 0.9f -> AmberWarning
        else -> NeonGreen
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, BrightBlue.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mobile Data Impact",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrightBlue,
                    fontWeight = FontWeight.Bold
                )
                if (hasCap) {
                    Text(
                        text = "Cap: $capMB MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.2f MB used for testing".format(usedMB),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )

            if (hasCap) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f),
                )
            } else if (!isPremium) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Upgrade to Pro to set a data usage cap and alerts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ResultDialog(state: LabUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = NeonGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audit Certificate", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            when (state) {
                is LabUiState.ThrottlingComplete -> ThrottlingResultView(state.status)
                is LabUiState.BufferbloatComplete -> BufferbloatResultView(state.result)
                is LabUiState.DnsComplete -> DnsResultView(state.result)
                is LabUiState.QoEComplete -> QoEResultView(state.activity, state.grade, state.details)
                is LabUiState.Error -> Text(state.message, color = CoralRed)
                else -> {}
            }
        }
    )
}

@Composable
fun QoEResultView(activity: String, grade: String, details: String) {
    Column {
        Text(
            text = "$activity Grade: $grade",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = ElectricCyan
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(details, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SpeedWatch analyzed latency, jitter, and throughput to determine if your connection supports high-quality $activity.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ThrottlingResultView(status: ThrottlingStatus) {
    Column {
        val (text, color) = when (status) {
            is ThrottlingStatus.Optimal -> "Optimal" to NeonGreen
            is ThrottlingStatus.HardwareLimited -> "Hardware Limited" to BrightBlue
            is ThrottlingStatus.Suspicious -> "Suspicious" to AmberWarning
            is ThrottlingStatus.HighlyLikely -> "Likely Throttled" to CoralRed
            else -> "Unknown" to Color.Gray
        }
        Text("Stability Grade: $text", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        val link = when (status) {
            is ThrottlingStatus.Optimal -> status.linkSpeed
            is ThrottlingStatus.HardwareLimited -> status.linkSpeed
            is ThrottlingStatus.Suspicious -> status.linkSpeed
            is ThrottlingStatus.HighlyLikely -> status.linkSpeed
            else -> 0.0
        }
        Text(
            text = "Your hardware link allows up to %.1f Mbps. SpeedWatch analyzed actual throughput against this link quality.".format(link),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BufferbloatResultView(result: BufferbloatResult) {
    Column {
        Text(
            text = "Stability Grade: ${result.grade}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = if (result.grade.startsWith("A")) NeonGreen else AmberWarning
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Load Increase: ${result.increaseMs}ms", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "This test simulated heavy traffic and measured lag spikes. A grade of ${result.grade} indicates how well your router manages congestion.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DnsResultView(result: DnsAuditResult) {
    Column {
        DnsRow("Cloudflare (1.1.1.1)", result.cloudflareMs)
        DnsRow("Google (8.8.8.8)", result.googleMs)
        DnsRow("Current Network", result.systemMs)
        Spacer(modifier = Modifier.height(12.dp))
        val fastest = listOf(result.cloudflareMs, result.googleMs, result.systemMs).minOrNull() ?: 0
        val recommendation = when (fastest) {
            result.cloudflareMs -> "Cloudflare"
            result.googleMs -> "Google"
            else -> "System Default"
        }
        Text(
            text = "Recommendation: Switch to $recommendation for faster web browsing.",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = ElectricCyan
        )
    }
}

@Composable
fun DnsRow(name: String, ms: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodySmall)
        Text("$ms ms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

