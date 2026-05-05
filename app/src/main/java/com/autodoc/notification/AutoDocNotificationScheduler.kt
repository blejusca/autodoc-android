package com.autodoc.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max

class AutoDocNotificationScheduler(
    private val context: Context
) {

    fun schedule(
        documentId: Int,
        type: String,
        carName: String,
        expiry: Long,
        daysBefore: Int
    ) {
        if (documentId <= 0 || expiry <= 0L) {
            return
        }

        cancel(documentId)

        val now = System.currentTimeMillis()
        val safeDaysBefore = max(daysBefore, 0)

        if (isExpiredMoreThan24Hours(expiry, now)) {
            return
        }

        val triggerTime = expiry - TimeUnit.DAYS.toMillis(safeDaysBefore.toLong())
        val delayMillis = triggerTime - now

        if (delayMillis > 0L) {
            enqueueReminder(
                documentId = documentId,
                type = type,
                carName = carName,
                expiry = expiry,
                daysBeforeExpiry = safeDaysBefore,
                delayMillis = delayMillis,
                policy = ExistingWorkPolicy.REPLACE
            )
        } else {
            enqueueReminder(
                documentId = documentId,
                type = type,
                carName = carName,
                expiry = expiry,
                daysBeforeExpiry = EXPIRED_IMMEDIATE_REMINDER,
                delayMillis = 10_000L,
                policy = ExistingWorkPolicy.REPLACE
            )
        }
    }

    fun cancel(documentId: Int) {
        if (documentId <= 0) {
            return
        }

        WorkManager.getInstance(context).cancelAllWorkByTag(
            documentTag(documentId)
        )
    }

    private fun enqueueReminder(
        documentId: Int,
        type: String,
        carName: String,
        expiry: Long,
        daysBeforeExpiry: Int,
        delayMillis: Long,
        policy: ExistingWorkPolicy
    ) {
        val notificationId = createNotificationId(
            documentId = documentId,
            daysBeforeExpiry = daysBeforeExpiry
        )

        val data = workDataOf(
            "documentType" to type,
            "carName" to carName,
            "notificationId" to notificationId,
            "expiryDateMillis" to expiry
        )

        val request = OneTimeWorkRequestBuilder<DocumentReminderWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(documentTag(documentId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(documentId, daysBeforeExpiry),
            policy,
            request
        )
    }

    private fun isExpiredMoreThan24Hours(
        expiry: Long,
        now: Long
    ): Boolean {
        return now - expiry > TimeUnit.HOURS.toMillis(24)
    }

    private fun createNotificationId(
        documentId: Int,
        daysBeforeExpiry: Int
    ): Int {
        val safeDaysBefore = if (daysBeforeExpiry < 0) {
            99
        } else {
            daysBeforeExpiry
        }

        val rawId = documentId.toLong() * 100L + safeDaysBefore.toLong()

        return abs((rawId % Int.MAX_VALUE).toInt()).coerceAtLeast(1)
    }

    private fun workName(
        documentId: Int,
        daysBeforeExpiry: Int
    ): String {
        return "autodoc_document_reminder_${documentId}_${daysBeforeExpiry}"
    }

    private fun documentTag(documentId: Int): String {
        return "autodoc_doc_$documentId"
    }

    companion object {
        private const val EXPIRED_IMMEDIATE_REMINDER = -1
    }
}