package com.speedwatch.app.ui.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.domain.PdfReportManager
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.navigation.NavKey
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    onNavigateToPremium: () -> Unit,
    onRunTest: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: ReportsViewModel = viewModel {
        ReportsViewModel(app.repository)
    }
    
    val reportData by viewModel.reportState.collectAsState()
    val settings by viewModel.settings.collectAsState(initial = null)
    val allLogs by app.repository.allLogs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { 
            SpeedWatchTopBar(
                title = stringResource(R.string.reports),
                actions = {
                    if (allLogs.size >= 5) {
                        IconButton(onClick = {
                            scope.launch {
                                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                val uri = PdfReportManager(context).generateReport(settings, allLogs, sdf.format(Date()))
                                uri?.let {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        type = "application/pdf"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Report"))
                                }
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export Report")
                        }
                    }
                }
            ) 
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            reportData?.let { data ->
                if (!data.isDataAvailable) {
                    EmptyReportView(onRunTest = onRunTest)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                            StatusHeader(data.overallStatus)
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Performance Summaries", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        data.dayReport?.let { SummaryCardAnimated(it, 100) }
                        data.weekReport?.let { SummaryCardAnimated(it, 200) }
                        data.monthReport?.let { SummaryCardAnimated(it, 300) }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text("Advanced Insights", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        Box {
                            Column {
                                GamingGradeCard(data.gamingStatus)
                                Spacer(modifier = Modifier.height(16.dp))
                                StreamingGradeCard(data.streamingStatus)
                            }
                            
                            if (settings?.isPremium != true) {
                                Surface(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .blur(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    onClick = onNavigateToPremium
                                ) {}
                                
                                Column(
                                    modifier = Modifier.matchParentSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "Unlock Pro for Detailed Insights",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun EmptyReportView(onRunTest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No Network Data Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Run your first speed test or wait for background monitoring to see your professional network health report.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        
        Button(
            onClick = onRunTest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Run Speed Test")
        }
    }
}

@Composable
fun SummaryCardAnimated(summary: PeriodSummary, delay: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn() + expandVertically()) {
        SummaryCard(summary)
    }
}

@Composable
fun StatusHeader(status: OverallStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = status.color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status.title) {
                    "Healthy" -> Icons.Default.CheckCircle
                    "Issues Detected" -> Icons.Default.Warning
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                tint = status.color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = status.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = status.color)
                Text(text = status.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SummaryCard(summary: PeriodSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = summary.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Badge(
                    containerColor = when (summary.status) {
                        "Excellent" -> Color(0xFF4CAF50)
                        "Good" -> Color(0xFF8BC34A)
                        "Fair" -> Color(0xFFFFC107)
                        "Poor" -> Color(0xFFF44336)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(summary.status, modifier = Modifier.padding(horizontal = 4.dp), color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem("Download", summary.avgDownload, "Mbps")
                MetricItem("Upload", summary.avgUpload, "Mbps")
                MetricItem("Avg Ping", summary.avgPing.toDouble(), "ms")
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: Double, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (unit == "ms") value.toInt().toString() else "%.1f".format(Locale.getDefault(), value),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit, 
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun GamingGradeCard(status: GamingStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Gaming Health",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = status.grade,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = status.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StreamingGradeCard(status: StreamingStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Streaming Quality",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = status.grade,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = status.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}
