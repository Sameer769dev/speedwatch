package com.speedwatch.app.ui.history

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.domain.ExportManager
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.theme.BrightBlue
import com.speedwatch.app.ui.theme.ElectricCyan
import com.speedwatch.app.ui.theme.NeonGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    onLogClick: (Long) -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: HistoryViewModel = viewModel {
        HistoryViewModel(app.repository)
    }
    val logs by viewModel.logs.collectAsState()
    val appSettings by app.repository.ispSettings.collectAsState(initial = null)

    val filteredLogs = remember(logs, appSettings) {
        if (appSettings?.isPremium == true) logs else logs.take(10)
    }

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(filteredLogs) {
        if (filteredLogs.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    val graphPoints = if (appSettings?.isPremium == true) {
                        logs.take(50).reversed()
                    } else {
                        logs.take(10).reversed()
                    }
                    series(graphPoints.map { it.downloadSpeedMbps })
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SpeedWatchTopBar(
                title = stringResource(R.string.history),
                actions = {
                    if (appSettings?.isPremium == true) {
                        IconButton(onClick = {
                            val uri = ExportManager(context).exportToCsv(logs)
                            uri?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export History"))
                            }
                        }) {
                            Icon(Icons.Default.Description, contentDescription = "Export CSV", tint = ElectricCyan)
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
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Network Performance Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (logs.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = VerticalAxis.rememberStart(),
                                bottomAxis = HorizontalAxis.rememberBottom(),
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .height(180.dp)
                                .fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .height(180.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No test logs yet. Run a speed test to view trends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (logs.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { log ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically { it / 2 } + fadeIn(animationSpec = tween(400)),
                    ) {
                        LogItem(log, onClick = { onLogClick(log.id) })
                    }
                }

                if (appSettings?.isPremium != true && logs.size > 10) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = ElectricCyan.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f)),
                            onClick = onNavigateToPremium
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = ElectricCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Unlock full history log and CSV export with SpeedWatch Pro",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ElectricCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogItem(log: com.speedwatch.app.data.model.SpeedLog, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = sdf.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.networkType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "↓ %.1f Mbps".format(Locale.getDefault(), log.downloadSpeedMbps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "↑ %.1f Mbps".format(Locale.getDefault(), log.uploadSpeedMbps),
                        style = MaterialTheme.typography.bodySmall,
                        color = BrightBlue
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

