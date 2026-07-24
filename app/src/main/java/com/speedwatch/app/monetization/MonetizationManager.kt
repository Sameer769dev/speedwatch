package com.speedwatch.app.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.speedwatch.app.data.repository.SpeedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlanTier(val id: String, val productType: String) {
    WEEKLY("speedwatch_pro_weekly", BillingClient.ProductType.SUBS),
    YEARLY("speedwatch_pro_yearly", BillingClient.ProductType.SUBS),
    LIFETIME("speedwatch_pro_lifetime", BillingClient.ProductType.INAPP)
}

class MonetizationManager(
    context: Context,
    private val repository: SpeedRepository
) : PurchasesUpdatedListener {

    private val TAG = "MonetizationManager"
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val _productsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productsMap: StateFlow<Map<String, ProductDetails>> = _productsMap.asStateFlow()

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    init {
        startBillingConnection()
    }

    private fun startBillingConnection() {
        _billingState.value = BillingState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Play Store Billing setup finished successfully")
                    queryAllProducts()
                    queryPurchases()
                } else {
                    _billingState.value = BillingState.Error("Play Store Error: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.Error("Play Store Disconnected")
            }
        })
    }

    private fun queryAllProducts() {
        val subProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlanTier.WEEKLY.id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlanTier.YEARLY.id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inAppProducts = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PlanTier.LIFETIME.id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val collectedMap = mutableMapOf<String, ProductDetails>()

        val subParams = QueryProductDetailsParams.newBuilder().setProductList(subProducts).build()
        billingClient.queryProductDetailsAsync(subParams) { subResult, subDetailsList ->
            if (subResult.responseCode == BillingClient.BillingResponseCode.OK && subDetailsList != null) {
                subDetailsList.forEach { collectedMap[it.productId] = it }
            }

            val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppProducts).build()
            billingClient.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsList ->
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK && inAppDetailsList != null) {
                    inAppDetailsList.forEach { collectedMap[it.productId] = it }
                }

                _productsMap.value = collectedMap
                _billingState.value = BillingState.Ready
            }
        }
    }

    fun queryPurchases() {
        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subResult, subPurchases ->
            val activeSub = subResult.responseCode == BillingClient.BillingResponseCode.OK &&
                    subPurchases.any { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                (purchase.products.contains(PlanTier.WEEKLY.id) || purchase.products.contains(PlanTier.YEARLY.id))
                    }

            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                val activeInApp = inAppResult.responseCode == BillingClient.BillingResponseCode.OK &&
                        inAppPurchases.any { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                    purchase.products.contains(PlanTier.LIFETIME.id)
                        }

                val isPro = activeSub || activeInApp
                scope.launch {
                    repository.setPremium(isPro)
                }

                (subPurchases + inAppPurchases).forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            }
        }
    }

    fun restorePurchases(onComplete: (Boolean, String) -> Unit) {
        if (!billingClient.isReady) {
            onComplete(false, "Play Store Service not ready. Please try again.")
            return
        }
        queryPurchases()
        scope.launch {
            repository.ispSettings.collect { settings ->
                if (settings?.isPremium == true) {
                    onComplete(true, "SpeedWatch Pro status restored successfully!")
                } else {
                    onComplete(false, "No active SpeedWatch Pro purchases found on this Google account.")
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken != null) {
                productDetailsParamsBuilder.setOfferToken(offerToken)
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            queryPurchases()
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryPurchases()
            }
        }
    }
}

sealed interface BillingState {
    data object Idle : BillingState
    data object Connecting : BillingState
    data object Ready : BillingState
    data class Error(val message: String) : BillingState
}

