package com.speedwatch.app.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.speedwatch.app.ui.settings.SettingsViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(app.repository)
    }

    var currentStep by remember { mutableIntStateOf(1) }
    var ispName by remember { mutableStateOf("") }
    var planSpeed by remember { mutableStateOf("") }
    var planSpeedError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
            },
            label = "StepTransition"
        ) { step ->
            if (step == 1) {
                StepIspDetails(
                    ispName = ispName,
                    onIspNameChange = { ispName = it },
                    planSpeed = planSpeed,
                    onPlanSpeedChange = { 
                        planSpeed = it
                        planSpeedError = null // Clear error on change
                    },
                    planSpeedError = planSpeedError,
                    onContinue = {
                        val speed = planSpeed.toDoubleOrNull()
                        when {
                            speed == null -> planSpeedError = "Please enter a valid number"
                            speed <= 0 -> planSpeedError = "Speed must be greater than 0"
                            speed > 10000 -> planSpeedError = "Speed seems too high (max 10,000 Mbps)"
                            else -> {
                                viewModel.saveSettings(ispName, speed, speed * 0.1)
                                currentStep = 2
                            }
                        }
                    }
                )
            } else {
                StepProUpsell(
                    onComplete = onComplete,
                    onGoPremium = onNavigateToPremium
                )
            }
        }
    }
}

@Composable
fun StepIspDetails(
    ispName: String,
    onIspNameChange: (String) -> Unit,
    planSpeed: String,
    onPlanSpeedChange: (String) -> Unit,
    planSpeedError: String?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.welcome),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = ispName,
            onValueChange = onIspNameChange,
            label = { Text(stringResource(R.string.isp_name)) },
            placeholder = { Text("e.g. Google Fiber, AT&T") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = planSpeed,
            onValueChange = onPlanSpeedChange,
            label = { Text(stringResource(R.string.plan_speed)) },
            placeholder = { Text("e.g. 100, 200, 1000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = planSpeedError != null,
            supportingText = {
                if (planSpeedError != null) {
                    Text(text = planSpeedError, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large,
            enabled = planSpeed.isNotEmpty()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun StepProUpsell(
    onComplete: () -> Unit,
    onGoPremium: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Diamond,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.upgrade_to_pro),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.pro_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        UpsellItem(stringResource(R.string.pro_feature_ads))
        UpsellItem(stringResource(R.string.pro_feature_freq))
        UpsellItem(stringResource(R.string.pro_feature_history))
        UpsellItem(stringResource(R.string.pro_feature_analytics))

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onGoPremium,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("See Pro Options")
        }

        TextButton(
            onClick = onComplete,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Continue with Free Version", color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun UpsellItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
