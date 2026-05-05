package com.autodoc.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.autodoc.data.AppPlanManager
import com.autodoc.data.dao.CarDao
import com.autodoc.data.dao.DocumentDao
import com.autodoc.notification.AutoDocNotificationScheduler

class AutoDocViewModelFactory(
    private val carDao: CarDao,
    private val documentDao: DocumentDao,
    private val scheduler: AutoDocNotificationScheduler,
    private val appPlanManager: AppPlanManager,
    private val getActivity: () -> Activity?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AutoDocViewModel::class.java)) {
            return AutoDocViewModel(
                carDao = carDao,
                documentDao = documentDao,
                scheduler = scheduler,
                appPlanManager = appPlanManager,
                getActivity = getActivity
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
