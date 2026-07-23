package com.speedwatch.app.ui.history

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    // Pro users see up to 50 points in the graph, free users see 10
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
                            Icon(Icons.Default.Description, contentDescription = "Export CSV")
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
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Network Performance Trend",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (logs.isNotEmpty()) {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Logs", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear All")
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLogs) { log ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { visible = true }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically { it / 2 } + fadeIn(animationSpec = tween(500)),
                    ) {
                        LogItem(log, onClick = { onLogClick(log.id) })
                    }
                }
                
                if (appSettings?.isPremium != true && logs.size > 10) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = onNavigateToPremium
                        ) {
                            Text(
                                "Unlock full history and CSV export in Pro version",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
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
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = log.networkType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "D: %.1f Mbps".format(Locale.getDefault(), log.downloadSpeedMbps),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "U: %.1f Mbps".format(Locale.getDefault(), log.uploadSpeedMbps),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
