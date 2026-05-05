package com.autodoc.data

import android.content.Context
import com.autodoc.billing.BillingManager
import kotlinx.coroutines.flow.StateFlow

/**
 * AppPlanManager - verifica statusul Pro exclusiv prin BillingManager.
 * Nu mai stocheaza starea in SharedPreferences - aceasta era vulnerabila
 * la manipulare pe dispozitive root-ate si nu reflecta realitatea platii.
 */
class AppPlanManager(context: Context) {

    val billingManager = BillingManager(context)

    /**
     * Flow reactiv cu statusul Pro - true doar daca achizitia este confirmata
     * de Google Play si a fost acknowledged corect.
     */
    val isProFlow: StateFlow<Boolean> = billingManager.isPurchased

    fun isProPlan(): Boolean {
        return billingManager.isPurchased.value
    }

    fun getFreePlanMaxCars(): Int {
        return FREE_PLAN_MAX_CARS
    }

    companion object {
        const val PLAN_FREE = "Free"
        const val PLAN_PRO = "Pro"
        const val FREE_PLAN_MAX_CARS = 3
    }
}
