package com.speedwatch.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.domain.PdfReportManager
import com.speedwatch.app.ui.components.PreferenceCategoryHeader
import com.speedwatch.app.ui.components.PreferenceRow
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.theme.ElectricCyan
import com.speedwatch.app.ui.theme.BrightBlue
import com.speedwatch.app.ui.theme.CoralRed
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(onNavigateToPremium: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(app.repository)
    }

    val savedSettings by viewModel.settings.collectAsState()
    val allLogs by app.repository.allLogs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var ispName by remember { mutableStateOf("") }
    var planSpeed by remember { mutableStateOf("") }
    var planSpeedError by remember { mutableStateOf<String?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(savedSettings) {
        savedSettings?.let {
            ispName = it.ispName
            planSpeed = it.promisedDownloadMbps.toString()
        }
    }

    Scaffold(
        topBar = { SpeedWatchTopBar(stringResource(R.string.settings)) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // PRO PROMO / STATUS
            if (savedSettings?.isPremium != true) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = ElectricCyan.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
                    onClick = onNavigateToPremium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(stringResource(R.string.go_pro), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            Text(stringResource(R.string.pro_feature_ads_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                PreferenceRow(
                    title = stringResource(R.string.pro_active),
                    summary = "Enjoy all premium benefits",
                    icon = Icons.Default.Diamond,
                    onClick = onNavigateToPremium
                )
            }

            // REAL-TIME MONITOR (PRO)
            if (savedSettings?.isPremium == true) {
                PreferenceCategoryHeader("Real-time Monitor (Pro)")

                PreferenceRow(
                    title = "Status Bar Monitor",
                    summary = "Show live speed in notification bar",
                    icon = Icons.Default.Speed,
                    widget = {
                        Switch(
                            checked = savedSettings?.statusBarMonitorEnabled == true,
                            onCheckedChange = { viewModel.setStatusBarMonitor(it) }
                        )
                    }
                )

                if (savedSettings?.statusBarMonitorEnabled == true) {
                    PreferenceRow(
                        title = "Show Download Speed",
                        widget = {
                            Checkbox(
                                checked = savedSettings?.showDownloadSpeed == true,
                                onCheckedChange = { viewModel.setShowDownload(it) }
                            )
                        }
                    )
                    PreferenceRow(
                        title = "Show Upload Speed",
                        widget = {
                            Checkbox(
                                checked = savedSettings?.showUploadSpeed == true,
                                onCheckedChange = { viewModel.setShowUpload(it) }
                            )
                        }
                    )
                    PreferenceRow(
                        title = "Show Real-time Ping",
                        widget = {
                            Checkbox(
                                checked = savedSettings?.showPing == true,
                                onCheckedChange = { viewModel.setShowPing(it) }
                            )
                        }
                    )
                }
            }

            // ISP & MONITORING
            PreferenceCategoryHeader("ISP & Monitoring")

            PreferenceRow(
                title = stringResource(R.string.isp_name),
                summary = ispName.ifEmpty { "Not set" },
                icon = Icons.Default.Business
            )

            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                OutlinedTextField(
                    value = ispName,
                    onValueChange = { ispName = it },
                    label = { Text("Provider Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = planSpeed,
                    onValueChange = {
                        planSpeed = it
                        planSpeedError = null
                    },
                    label = { Text(stringResource(R.string.plan_speed)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    isError = planSpeedError != null,
                    supportingText = {
                        if (planSpeedError != null) {
                            Text(text = planSpeedError!!, color = CoralRed)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val speed = planSpeed.toDoubleOrNull()
                        if (speed != null && speed > 0 && speed <= 10000) {
                            viewModel.saveSettings(ispName, speed, speed * 0.1)
                            scope.launch {
                                snackbarHostState.showSnackbar("Plan details updated successfully")
                            }
                        } else {
                            planSpeedError = "Invalid speed"
                        }
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color(0xFF00363D)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text("Update Plan", fontWeight = FontWeight.Bold)
                }
            }

            if (savedSettings?.isPremium == true) {
                var sliderPosition by remember { mutableStateOf(savedSettings?.checkFrequencyHours?.toFloat() ?: 6f) }
                PreferenceRow(
                    title = "Check Frequency",
                    summary = "Every ${sliderPosition.toInt()} hours",
                    icon = Icons.Default.Timer,
                    widget = {
                        Slider(
                            value = sliderPosition,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = { viewModel.setFrequency(sliderPosition.toInt()) },
                            valueRange = 1f..12f,
                            steps = 11,
                            modifier = Modifier.width(120.dp)
                        )
                    }
                )
            }

            PreferenceRow(
                title = "Mobile Background Tests",
                summary = "Run tests on cellular data",
                icon = Icons.Default.SignalCellularAlt,
                widget = {
                    Switch(
                        checked = savedSettings?.allowMobileBackgroundTests == true,
                        onCheckedChange = { viewModel.setAllowMobileBackground(it) }
                    )
                }
            )

            // NOTIFICATIONS
            PreferenceCategoryHeader("Notifications")

            PreferenceRow(
                title = "Speed Drop Alerts",
                summary = "Notify when speed is below 80%",
                icon = Icons.Default.NotificationsActive,
                widget = {
                    Switch(
                        checked = savedSettings?.speedDropAlertsEnabled == true,
                        onCheckedChange = { viewModel.setSpeedDropAlerts(it) }
                    )
                }
            )

            PreferenceRow(
                title = "Monthly Reports",
                summary = "Notify when monthly PDF is ready",
                icon = Icons.Default.Assessment,
                widget = {
                    Switch(
                        checked = savedSettings?.reportAlertsEnabled == true,
                        onCheckedChange = { viewModel.setReportAlerts(it) }
                    )
                }
            )

            PreferenceRow(
                title = "Data Usage Alerts",
                summary = "Notify at 90% of data cap",
                icon = Icons.Default.DataUsage,
                widget = {
                    Switch(
                        checked = savedSettings?.dataUsageAlertEnabled == true,
                        onCheckedChange = { viewModel.setUsageAlerts(it) }
                    )
                }
            )

            // APPEARANCE
            PreferenceCategoryHeader("Appearance")

            PreferenceRow(
                title = "App Theme",
                summary = savedSettings?.themePreference ?: "SYSTEM",
                icon = Icons.Default.Palette,
                onClick = { showThemeDialog = true }
            )

            // DATA & PRIVACY
            PreferenceCategoryHeader("Data & Privacy")

            PreferenceRow(
                title = "Monthly Data Cap (MB)",
                summary = if (savedSettings?.dataUsageCapMB == 0) "Unlimited" else "${savedSettings?.dataUsageCapMB} MB",
                icon = Icons.Default.Storage
            )

            var capValue by remember { mutableStateOf(savedSettings?.dataUsageCapMB?.toString() ?: "0") }
            OutlinedTextField(
                value = capValue,
                onValueChange = {
                    capValue = it
                    it.toIntOrNull()?.let { cap -> viewModel.setDataUsageCap(cap) }
                },
                label = { Text("Monthly Budget (0 = Unlimited)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                shape = RoundedCornerShape(14.dp)
            )

            val hasEnoughData = allLogs.size >= 5
            PreferenceRow(
                title = "Generate PDF Report",
                summary = if (hasEnoughData) "Create detailed analysis (Also in Reports tab)" else "Needs 5+ logs",
                icon = Icons.Default.PictureAsPdf,
                onClick = {
                    if (hasEnoughData) {
                        scope.launch {
                            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            val uri = PdfReportManager(context).generateReport(savedSettings, allLogs, sdf.format(Date()))
                            uri?.let {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    type = "application/pdf"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Report"))
                            }
                        }
                    }
                }
            )

            var showClearAllDialog by remember { mutableStateOf(false) }

            if (showClearAllDialog) {
                AlertDialog(
                    onDismissRequest = { showClearAllDialog = false },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "Delete All Data?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Text(
                            text = "This will permanently wipe all your saved speed logs, network audits, and data usage statistics. This cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearHistory()
                                showClearAllDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("All history logs and metrics deleted")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Delete Everything", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showClearAllDialog = false },
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("Cancel")
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                )
            }

            PreferenceRow(
                title = "Clear All Data",
                summary = "Logs, audits, and usage stats",
                icon = Icons.Default.DeleteForever,
                onClick = { showClearAllDialog = true }
            )

            // SUPPORT & ABOUT
            PreferenceCategoryHeader("Support & About")

            PreferenceRow(
                title = "Rate SpeedWatch",
                summary = "Leave a Play Store rating or review",
                icon = Icons.Default.Star,
                onClick = {
                    val activity = context as? android.app.Activity
                    if (activity != null) {
                        app.inAppReviewManager.launchInAppReview(activity) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            try { context.startActivity(intent) } catch (e: Exception) { /* Fallback */ }
                        }
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        try { context.startActivity(intent) } catch (e: Exception) { /* Fallback */ }
                    }
                }
            )

            PreferenceRow(
                title = "Privacy Policy",
                icon = Icons.Default.PrivacyTip,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://speedwatch-app.vercel.app/privacy.html"))
                    context.startActivity(intent)
                }
            )

            PreferenceRow(
                title = "Terms of Service",
                icon = Icons.Default.Description,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://speedwatch-app.vercel.app/terms.html"))
                    context.startActivity(intent)
                }
            )

            PreferenceRow(
                title = "Contact Support",
                summary = "Report a bug or suggest a feature",
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@speedwatch.app"))
                    context.startActivity(intent)
                }
            )

            val packageInfo = remember(context) {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (e: Exception) {
                    null
                }
            }
            val versionName = packageInfo?.versionName ?: "1.2.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode ?: 3L
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode?.toLong() ?: 3L
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SpeedWatch v$versionName (Build $versionCode)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            confirmButton = {},
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = savedSettings?.themePreference == theme, onClick = null)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(theme, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        )
    }
}

