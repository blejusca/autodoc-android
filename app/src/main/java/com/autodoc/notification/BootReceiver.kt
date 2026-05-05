package com.autodoc.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.autodoc.data.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleDailyCheck(appContext)
                rescheduleDocumentReminders(appContext)
            } finally {
                withContext(Dispatchers.Main) {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun rescheduleDocumentReminders(context: Context) {
        val database = DatabaseProvider.getDatabase(context)
        val scheduler = AutoDocNotificationScheduler(context)

        val cars = database.carDao().observeCars().first()
        val documents = database.documentDao().observeDocuments().first()

        documents.forEach { document ->
            val car = cars.firstOrNull { it.id == document.carId }

            val carName = if (car != null) {
                "${car.brand} ${car.model} - ${car.plate}"
            } else {
                "Masina necunoscuta"
            }

            scheduler.schedule(
                documentId = document.id,
                type = document.type,
                carName = carName,
                expiry = document.expiryDate,
                daysBefore = document.reminderDaysBefore
            )
        }
    }

    private fun rescheduleDailyCheck(context: Context) {
        val now = LocalDateTime.now()

        val todayAtNine = now
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val nextRun = if (todayAtNine.isAfter(now)) {
            todayAtNine
        } else {
            todayAtNine.plusDays(1)
        }

        val delayMillis = Duration.between(now, nextRun).toMillis()

        val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(
            1,
            TimeUnit.DAYS
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_document_check",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}