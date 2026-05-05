package com.autodoc.billing

import com.autodoc.ui.localizedText
import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val context: Context) {

    companion object {
        // Acesta este ID-ul produsului creat in Google Play Console
        // -> Monetizare -> Produse in-app -> Produs nou (one-time)
        const val PRODUCT_ID_PRO = "autodoc_pro_lifetime"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPurchased = MutableStateFlow(false)
    val isPurchased: StateFlow<Boolean> = _isPurchased.asStateFlow()

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingState.value = BillingState.Cancelled
            }
            else -> {
                _billingState.value = BillingState.Error(billingResult.debugMessage)
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        connectAndQueryPurchases()
    }

    private fun connectAndQueryPurchases() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        queryExistingPurchases()
                        loadProductDetails()
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // BillingClient se va reconecta automat la urmatorul apel
            }
        })
    }

    private fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchaseList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActivePro = purchaseList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PRO) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPurchased.value = hasActivePro

                // Acknowledge orice achizitie neconfirmata
                purchaseList.forEach { purchase ->
                    if (!purchase.isAcknowledged) {
                        handlePurchase(purchase)
                    }
                }
            }
        }
    }

    private fun loadProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = productDetails

        if (details == null) {
            // Reconecteaza si incearca din nou
            _billingState.value = BillingState.Error(localizedText("Produsul nu este disponibil momentan. Încearcă din nou.", "The product is currently unavailable. Try again."))
            if (!billingClient.isReady) {
                connectAndQueryPurchases()
            } else {
                scope.launch { loadProductDetails() }
            }
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _billingState.value = BillingState.Launching
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingState.value = BillingState.Error("Nu s-a putut deschide fereastra de cumparare.")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRODUCT_ID_PRO)) {
                _isPurchased.value = true
                _billingState.value = BillingState.PurchaseSuccess
            }

            // Acknowledge - OBLIGATORIU pentru a nu rambursa automat cumparatura
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    // Acknowledge reusit - produsul ramane activ
                }
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _billingState.value = BillingState.Pending
        }
    }

    fun consumeBillingState() {
        _billingState.value = BillingState.Idle
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}

sealed class BillingState {
    object Idle : BillingState()
    object Launching : BillingState()
    object PurchaseSuccess : BillingState()
    object Cancelled : BillingState()
    object Pending : BillingState()
    data class Error(val message: String) : BillingState()
}
