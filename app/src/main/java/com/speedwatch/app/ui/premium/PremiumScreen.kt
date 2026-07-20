package com.speedwatch.app.ui.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.monetization.BillingState
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.settings.SettingsViewModel

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(app.repository)
    }
    
    val settings by viewModel.settings.collectAsState()
    val productDetails by app.monetizationManager.productDetails.collectAsState()
    val billingState by app.monetizationManager.billingState.collectAsState()

    Scaffold(
        topBar = { SpeedWatchTopBar(stringResource(R.string.premium), onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Diamond,
                contentDescription = "Debug Unlock",
                modifier = Modifier
                    .size(80.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                // Developer Bypass: Long press icon to toggle premium
                                viewModel.setPremium(settings?.isPremium == false)
                            }
                        )
                    },
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.upgrade_to_pro),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.pro_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PremiumFeatureItem(stringResource(R.string.pro_feature_ads), stringResource(R.string.pro_feature_ads_desc))
            PremiumFeatureItem(stringResource(R.string.pro_feature_freq), stringResource(R.string.pro_feature_freq_desc))
            PremiumFeatureItem(stringResource(R.string.pro_feature_history), stringResource(R.string.pro_feature_history_desc))
            PremiumFeatureItem(stringResource(R.string.pro_feature_analytics), stringResource(R.string.pro_feature_analytics_desc))
            
            Spacer(modifier = Modifier.height(48.dp))
            
            if (settings?.isPremium == true) {
                Button(
                    onClick = { viewModel.setPremium(false) }, // Allow resetting in dev mode
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("Pro Active (Tap to Reset for Testing)")
                }
            } else {
                val buttonText = when (val state = billingState) {
                    is BillingState.Connecting -> "Connecting to Play Store..."
                    is BillingState.Ready -> stringResource(R.string.unlock_everything, productDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "")
                    is BillingState.Error -> state.message
                    else -> "Play Store Unavailable"
                }

                Button(
                    onClick = { 
                        productDetails?.let {
                            app.monetizationManager.launchBillingFlow(activity, it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    enabled = billingState is BillingState.Ready
                ) {
                    Text(buttonText)
                }

                if (billingState is BillingState.Error) {
                    Text(
                        text = "Tip: Long-press the Diamond icon to bypass for development testing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                TextButton(onClick = { onBack() }) {
                    Text(stringResource(R.string.maybe_later))
                }
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
