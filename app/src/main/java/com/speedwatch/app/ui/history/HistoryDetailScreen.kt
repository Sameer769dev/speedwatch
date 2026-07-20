package com.speedwatch.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.data.model.SpeedLog
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDetailScreen(logId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    var log by remember { mutableStateOf<SpeedLog?>(null) }
    
    LaunchedEffect(logId) {
        // Simple fetch for detail
        log = app.repository.allLogs.firstOrNull()?.find { it.id == logId }
    }

    Scaffold(
        topBar = { SpeedWatchTopBar("Test Details") }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            log?.let {
                val sdf = remember { SimpleDateFormat("MMMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
                
                DetailItem("Time", sdf.format(Date(it.timestamp)))
                DetailItem("Download Speed", "%.2f Mbps".format(it.downloadSpeedMbps))
                DetailItem("Upload Speed", "%.2f Mbps".format(it.uploadSpeedMbps))
                DetailItem("Network Type", it.networkType)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to History")
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
