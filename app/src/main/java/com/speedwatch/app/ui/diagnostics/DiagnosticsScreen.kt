package com.speedwatch.app.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.data.model.LabAudit
import com.speedwatch.app.domain.BufferbloatResult
import com.speedwatch.app.domain.DnsAuditResult
import com.speedwatch.app.domain.ThrottlingStatus
import com.speedwatch.app.ui.components.SpeedWatchTopBar
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

    Scaffold(
        topBar = { SpeedWatchTopBar(stringResource(R.string.lab)) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                DataUsageCard(
                    bytes = dataUsage, 
                    capMB = settings?.dataUsageCapMB ?: 0,
                    isPremium = settings?.isPremium == true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Technical Lab Tests", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            item {
                LabTestCard(
                    title = "ISP Throttling Check",
                    description = "Compares hardware link vs real throughput.",
                    icon = Icons.Default.Speed,
                    onRun = { viewModel.runThrottlingTest() }
                )
            }

            item {
                LabTestCard(
                    title = "Bufferbloat Analysis",
                    description = "Measures 90th percentile lag under load.",
                    icon = Icons.Default.NetworkCheck,
                    onRun = { viewModel.runBufferbloatTest() }
                )
            }

            item {
                LabTestCard(
                    title = "DNS Resolution Audit",
                    description = "Raw UDP queries to Google & Cloudflare.",
                    icon = Icons.Default.Dns,
                    onRun = { viewModel.runDnsAudit() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audit History", style = MaterialTheme.typography.titleMedium)
                    if (recentAudits.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAuditHistory() }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recentAudits.isEmpty()) {
                item {
                    Text(
                        "No audits performed yet. Run a test to generate a certificate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(recentAudits) { audit ->
                    AuditHistoryItem(audit)
                }
            }
        }
    }

    // Modal Results
    if (labState is LabUiState.Running) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Lab Test in Progress") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text((labState as LabUiState.Running).message)
                }
            }
        )
    }

    if (labState is LabUiState.RequireMobileData) {
        AlertDialog(
            onDismissRequest = { viewModel.reset() },
            confirmButton = { TextButton(onClick = { viewModel.reset() }) { Text("OK") } },
            title = { Text("Mobile Data Required") },
            text = { Text("Advanced Lab tests are designed for Cellular networks. Please disable Wi-Fi and enable Mobile Data to continue.") }
        )
    }

    if (labState !is LabUiState.Idle && labState !is LabUiState.Running && labState !is LabUiState.RequireMobileData) {
        ResultDialog(state = labState, onDismiss = { viewModel.reset() })
    }
}

@Composable
fun LabTestCard(title: String, description: String, icon: ImageVector, onRun: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onRun) {
                Text("Start")
            }
        }
    }
}

@Composable
fun AuditHistoryItem(audit: LabAudit) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(audit.testType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(audit.mainResult, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(sdf.format(Date(audit.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Icon(Icons.Default.Verified, contentDescription = "Audit Certificate", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun DataUsageCard(bytes: Long, capMB: Int, isPremium: Boolean) {
    val usedMB = bytes / (1024.0 * 1024.0)
    val hasCap = isPremium && capMB > 0
    val progress = if (hasCap) (usedMB / capMB).toFloat().coerceIn(0f, 1f) else 0f
    
    val progressColor = when {
        progress >= 1f -> Color(0xFFF44336)
        progress >= 0.9f -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mobile Data Impact", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                if (hasCap) {
                    Text("Cap: $capMB MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Text("%.2f MB used for testing".format(usedMB), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            
            if (hasCap) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else if (!isPremium) {
                Text("Upgrade to Pro to set a data usage cap and alerts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            } else {
                Text("Speed tests consume data. Monitor this to stay under your cap.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ResultDialog(state: LabUiState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audit Certificate")
            }
        },
        text = {
            when (state) {
                is LabUiState.ThrottlingComplete -> ThrottlingResultView(state.status)
                is LabUiState.BufferbloatComplete -> BufferbloatResultView(state.result)
                is LabUiState.DnsComplete -> DnsResultView(state.result)
                is LabUiState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
    )
}

@Composable
fun ThrottlingResultView(status: ThrottlingStatus) {
    Column {
        val (text, color) = when (status) {
            is ThrottlingStatus.Optimal -> "Optimal" to Color(0xFF4CAF50)
            is ThrottlingStatus.HardwareLimited -> "Hardware Limited" to Color(0xFF2196F3)
            is ThrottlingStatus.Suspicious -> "Suspicious" to Color(0xFFFFC107)
            is ThrottlingStatus.HighlyLikely -> "Likely Throttled" to Color(0xFFF44336)
            else -> "Unknown" to Color.Gray
        }
        Text("Stability Grade: $text", fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(8.dp))
        val link = when(status) {
            is ThrottlingStatus.Optimal -> status.linkSpeed
            is ThrottlingStatus.HardwareLimited -> status.linkSpeed
            is ThrottlingStatus.Suspicious -> status.linkSpeed
            is ThrottlingStatus.HighlyLikely -> status.linkSpeed
            else -> 0.0
        }
        Text("Your hardware link allows up to %.1f Mbps. SpeedWatch analyzed actual throughput against this link quality.".format(link), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun BufferbloatResultView(result: BufferbloatResult) {
    Column {
        Text("Stability Grade: ${result.grade}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = if (result.grade.startsWith("A")) Color(0xFF4CAF50) else Color(0xFFFFC107))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Load Increase: ${result.increaseMs}ms", fontWeight = FontWeight.Bold)
        Text("This test simulated heavy traffic and measured lag spikes. A grade of ${result.grade} indicates how well your router manages congestion.", style = MaterialTheme.typography.bodySmall)
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
        val recommendation = when(fastest) {
            result.cloudflareMs -> "Cloudflare"
            result.googleMs -> "Google"
            else -> "System Default"
        }
        Text("Recommendation: Switch to $recommendation for faster web browsing.", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun DnsRow(name: String, ms: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodySmall)
        Text("$ms ms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}
