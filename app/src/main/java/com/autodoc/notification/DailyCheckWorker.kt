package com.autodoc.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autodoc.data.DatabaseProvider
import com.autodoc.ui.localizedText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DailyCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = DatabaseProvider.getDatabase(applicationContext)

            val documentDao = db.documentDao()
            val carDao = db.carDao()

            val documents = documentDao.getAllDocuments()
            val today = LocalDate.now()

            var expiredCount = 0
            var todayCount = 0
            var tomorrowCount = 0
            var soonCount = 0
            var clientsToNotify = 0

            documents.forEach { document ->
                if (document.manuallyNotified) {
                    return@forEach
                }

                val car = carDao.getCarById(document.carId) ?: return@forEach

                val expiryDate = Instant.ofEpochMilli(document.expiryDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val daysLeft = ChronoUnit.DAYS.between(today, expiryDate).toInt()

                val hasPhone = car.ownerPhone.isNotBlank()
                val isInReminderWindow = daysLeft <= document.reminderDaysBefore

                when {
                    daysLeft < 0 -> expiredCount++
                    daysLeft == 0 -> todayCount++
                    daysLeft == 1 -> tomorrowCount++
                    daysLeft in 2..document.reminderDaysBefore -> soonCount++
                }

                if (hasPhone && isInReminderWindow) {
                    clientsToNotify++
                }
            }

            val totalImportant = expiredCount + todayCount + tomorrowCount + soonCount

            if (totalImportant > 0) {
                val title = localizedText("Documente auto de verificat", "Vehicle documents to check")

                val message = buildString {
                    append("$totalImportant ${localizedText("documente necesită atenție", "documents need attention") }.")

                    if (expiredCount > 0) append(" ${localizedText("Expirate", "Expired")}: $expiredCount.")
                    if (todayCount > 0) append(" ${localizedText("Expiră azi", "Expires today")}: $todayCount.")
                    if (tomorrowCount > 0) append(" ${localizedText("Expiră mâine", "Expires tomorrow")}: $tomorrowCount.")
                    if (soonCount > 0) append(" ${localizedText("În curând", "Soon")}: $soonCount.")
                    if (clientsToNotify > 0) append(" ${localizedText("Clienți de notificat", "Clients to notify")}: $clientsToNotify.")
                }

                DocumentReminderWorker.showNotification(
                    context = applicationContext,
                    title = title,
                    message = message
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }
}