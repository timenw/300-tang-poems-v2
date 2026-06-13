package com.poem300.billing

import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val application: Application) : PurchasesUpdatedListener {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // Test mode: when true, simulates premium without real purchase
    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    private var billingClient: BillingClient? = null

    companion object {
        private const val TAG = "BillingManager"
        const val SKU_PREMIUM = "poem_premium_lifetime"

        // Debug build auto-enables test mode (no real purchase needed)
        val isDebugBuild: Boolean = try {
            val clazz = Class.forName("com.poem300.BuildConfig")
            clazz.getField("DEBUG").get(null) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    init {
        // Debug build: do NOT auto-enable test mode or premium
        // User must manually toggle test mode in Settings to test premium features
        if (isDebugBuild) {
            _isTestMode.value = false
            _isPremium.value = false
            Log.d(TAG, "Debug build detected - test mode OFF, premium OFF")
        }
    }

    fun enableTestMode() {
        _isTestMode.value = true
        _isPremium.value = true
        Log.d(TAG, "Test mode enabled - premium activated")
    }

    fun disableTestMode() {
        _isTestMode.value = false
        _isPremium.value = false
        Log.d(TAG, "Test mode disabled")
    }

    fun toggleTestMode() {
        if (_isTestMode.value) disableTestMode() else enableTestMode()
    }

    fun startConnection() {
        // In test mode, simulate connected but don't auto-grant premium
        // Premium is only granted when user explicitly toggles test mode ON
        if (_isTestMode.value) {
            Log.d(TAG, "Test mode: skipping real billing connection")
            _isConnected.value = true
            // Check if premium was explicitly enabled via toggle
            // (init sets test mode ON but premium OFF for debug builds)
            return
        }

        // Real billing connection for release builds
        if (isDebugBuild) {
            Log.d(TAG, "Debug build without test mode: skipping real billing connection")
            _isConnected.value = true
            return
        }

        try {
            billingClient = BillingClient.newBuilder(application)
                .setListener(this)
                .enablePendingPurchases()
                .build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isConnected.value = true
                        Log.d(TAG, "Billing connected")
                        queryPurchases()
                    } else {
                        Log.e(TAG, "Billing setup failed: ${result.responseCode} - ${result.debugMessage}")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isConnected.value = false
                    Log.d(TAG, "Billing disconnected")
                    retryConnection()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Billing startConnection error: ${e.message}")
        }
    }

    private fun retryConnection() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(3000)
            if (!_isConnected.value) {
                startConnection()
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${result.responseCode} - ${result.debugMessage}")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isPremium.value = true
                        Log.d(TAG, "Purchase acknowledged")
                    }
                }
            } else {
                _isPremium.value = true
            }
        }
    }

    private fun queryPurchases() {
        try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            billingClient?.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasPremium = purchases.any {
                        it.products.contains(SKU_PREMIUM) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    _isPremium.value = hasPremium
                    Log.d(TAG, "Premium status: $hasPremium")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "queryPurchases error: ${e.message}")
        }
    }

    fun launchPurchaseFlow(activity: android.app.Activity) {
        try {
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(SKU_PREMIUM)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

            billingClient?.queryProductDetailsAsync(queryParams) { result, productDetailsList ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                    val productDetails = productDetailsList[0]
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .build()
                            )
                        )
                        .build()
                    billingClient?.launchBillingFlow(activity, billingFlowParams)
                } else {
                    Log.e(TAG, "Product details query failed: ${result.debugMessage}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "launchPurchaseFlow error: ${e.message}")
        }
    }

    fun endConnection() {
        try {
            billingClient?.endConnection()
        } catch (e: Exception) {
            Log.e(TAG, "endConnection error: ${e.message}")
        }
    }
}
