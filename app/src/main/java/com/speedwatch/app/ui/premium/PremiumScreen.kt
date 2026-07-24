package com.speedwatch.app.ui.premium

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speedwatch.app.R
import com.speedwatch.app.SpeedWatchApplication
import com.speedwatch.app.monetization.BillingState
import com.speedwatch.app.monetization.PlanTier
import com.speedwatch.app.ui.components.SpeedWatchTopBar
import com.speedwatch.app.ui.settings.SettingsViewModel
import com.speedwatch.app.ui.theme.BrightBlue
import com.speedwatch.app.ui.theme.ElectricCyan
import com.speedwatch.app.ui.theme.NeonGreen

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val app = context.applicationContext as SpeedWatchApplication
    val viewModel: SettingsViewModel = viewModel {
        SettingsViewModel(app.repository)
    }

    val settings by viewModel.settings.collectAsState()
    val productsMap by app.monetizationManager.productsMap.collectAsState()
    val billingState by app.monetizationManager.billingState.collectAsState()

    var selectedTier by remember { mutableStateOf(PlanTier.YEARLY) }

    Scaffold(
        topBar = { SpeedWatchTopBar(stringResource(R.string.premium), onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Header with Electric Cyan Gradient
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ElectricCyan.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier
                                .size(88.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {
                                            viewModel.setPremium(settings?.isPremium == false)
                                            Toast.makeText(context, "Debug Premium toggled!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                            shape = CircleShape,
                            color = ElectricCyan.copy(alpha = 0.12f),
                            border = BorderStroke(2.dp, ElectricCyan)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Diamond,
                                    contentDescription = "Pro Icon",
                                    modifier = Modifier.size(52.dp),
                                    tint = ElectricCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = ElectricCyan,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "SPEEDWATCH PRO PASS",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF00363D)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Unlock Maximum Performance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Proactive SLA audits, live overlay, and ad-free experience",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }

            // Plan Tier Selector Cards
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Yearly (Best Value)
                    val yearlyProduct = productsMap[PlanTier.YEARLY.id]
                    val yearlyPrice = yearlyProduct?.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$19.99 / yr"

                    PlanCard(
                        tierName = "Yearly Pro Pass",
                        priceText = yearlyPrice,
                        subText = "Equivalent to $1.66 / mo • Billed Annually",
                        badgeText = "BEST VALUE — SAVE 60%",
                        badgeColor = ElectricCyan,
                        isSelected = selectedTier == PlanTier.YEARLY,
                        onClick = { selectedTier = PlanTier.YEARLY }
                    )

                    // Weekly (Trial)
                    val weeklyProduct = productsMap[PlanTier.WEEKLY.id]
                    val weeklyPrice = weeklyProduct?.subscriptionOfferDetails?.firstOrNull()
                        ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$1.99 / wk"

                    PlanCard(
                        tierName = "Weekly Access",
                        priceText = weeklyPrice,
                        subText = "3-Day Free Trial included • Cancel anytime",
                        badgeText = "3-DAY FREE TRIAL",
                        badgeColor = NeonGreen,
                        isSelected = selectedTier == PlanTier.WEEKLY,
                        onClick = { selectedTier = PlanTier.WEEKLY }
                    )

                    // Lifetime (One-time)
                    val lifetimeProduct = productsMap[PlanTier.LIFETIME.id]
                    val lifetimePrice = lifetimeProduct?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$39.99"

                    PlanCard(
                        tierName = "Lifetime Pass",
                        priceText = lifetimePrice,
                        subText = "One-time payment • Pay once, own forever",
                        badgeText = "ONE-TIME OWNERSHIP",
                        badgeColor = BrightBlue,
                        isSelected = selectedTier == PlanTier.LIFETIME,
                        onClick = { selectedTier = PlanTier.LIFETIME }
                    )
                }
            }

            // Features Breakdown
            item {
                Text(
                    text = "Everything Included in Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            val features = listOf(
                R.string.pro_feature_ads to R.string.pro_feature_ads_desc,
                R.string.pro_feature_monitor to R.string.pro_feature_monitor_desc,
                R.string.pro_feature_lab to R.string.pro_feature_lab_desc,
                R.string.pro_feature_benchmarks to R.string.pro_feature_benchmarks_desc,
                R.string.pro_feature_analytics to R.string.pro_feature_analytics_desc,
                R.string.pro_feature_freq to R.string.pro_feature_freq_desc
            )

            items(features) { (titleRes, descRes) ->
                PremiumFeatureItem(stringResource(titleRes), stringResource(descRes))
            }

            // CTA Purchase Button & Compliance Footer
            item {
                Spacer(modifier = Modifier.height(24.dp))

                if (settings?.isPremium == true) {
                    Button(
                        onClick = { viewModel.setPremium(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("Pro Active (Tap to Reset for Testing)", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val activeProduct = productsMap[selectedTier.id]
                    val ctaText = when {
                        selectedTier == PlanTier.WEEKLY -> "Start 3-Day Free Trial"
                        selectedTier == PlanTier.YEARLY -> "Subscribe for Yearly Pass"
                        else -> "Unlock Lifetime Access"
                    }

                    Button(
                        onClick = {
                            if (activeProduct != null) {
                                app.monetizationManager.launchBillingFlow(activity, activeProduct)
                            } else {
                                Toast.makeText(context, "Connecting to Play Store... Please check internet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF00363D)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ctaText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            app.monetizationManager.restorePurchases { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Purchases", style = MaterialTheme.typography.labelMedium, color = ElectricCyan)
                        }
                    }

                    // Security & Play Store Guarantee
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Secured by Google Play • Cancel anytime in Play Store settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { onBack() }) {
                        Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PlanCard(
    tierName: String,
    priceText: String,
    subText: String,
    badgeText: String,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) ElectricCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tierName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = priceText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                )

                RadioButton(selected = isSelected, onClick = null)
            }
        }
    }
}

@Composable
fun PremiumFeatureItem(title: String, description: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

