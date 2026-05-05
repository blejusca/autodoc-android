package com.autodoc.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autodoc.MainActivity
import com.autodoc.R
import com.autodoc.ui.localizedText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class DocumentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val type = inputData.getString("documentType").orEmpty().ifBlank { localizedText("Document auto", "Vehicle document") }
            val carName = inputData.getString("carName").orEmpty().ifBlank { localizedText("Mașină", "Vehicle") }
            val expiryMillis = inputData.getLong("expiryDateMillis", 0L)
            val rawNotificationId = inputData.getInt("notificationId", 0)

            val notificationId = if (rawNotificationId > 0) {
                rawNotificationId
            } else {
                createSafeNotificationId()
            }

            if (expiryMillis <= 0L) {
                return Result.success()
            }

            if (!hasNotificationPermission(applicationContext)) {
                return Result.success()
            }

            val expiryDate = Instant.ofEpochMilli(expiryMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val daysLeft = ChronoUnit.DAYS.between(
                LocalDate.now(),
                expiryDate
            ).toInt()

            val statusText = when {
                daysLeft < 0 -> {
                    val days = abs(daysLeft)
                    if (days == 1) localizedText("a expirat de 1 zi", "expired 1 day ago") else localizedText("a expirat de $days zile", "expired $days days ago")
                }
                daysLeft == 0 -> localizedText("expiră azi", "expires today")
                daysLeft == 1 -> localizedText("expiră mâine", "expires tomorrow")
                else -> localizedText("expiră în $daysLeft zile", "expires in $daysLeft days")
            }

            val title = "$type $statusText"
            val message = "$carName • ${localizedText("Verifică documentul în aplicație", "Check the document in the app")}"

            showNotification(
                context = applicationContext,
                notificationId = notificationId,
                title = title,
                message = message
            )

            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        private const val CHANNEL_ID = "autodoc_document_notifications"

        fun showNotification(
            context: Context,
            title: String,
            message: String
        ) {
            try {
                showNotification(
                    context = context,
                    notificationId = createSafeNotificationId(),
                    title = title,
                    message = message
                )
            } catch (_: Exception) {
            }
        }

        private fun showNotification(
            context: Context,
            notificationId: Int,
            title: String,
            message: String
        ) {
            try {
                if (!hasNotificationPermission(context)) {
                    return
                }

                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        ?: return

                createNotificationChannel(manager)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title.ifBlank { localizedText("Document auto", "Vehicle document") })
                    .setContentText(message.ifBlank { localizedText("Ai un document auto de verificat.", "You have a vehicle document to check.") })
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                manager.notify(notificationId, notification)
            } catch (_: Exception) {
            }
        }

        private fun createNotificationChannel(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    localizedText("Documente Auto", "Vehicle Documents"),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = localizedText(
                        "Notificări pentru documente auto expirate sau aproape de expirare",
                        "Notifications for expired or soon-to-expire vehicle documents"
                    )
                }

                manager.createNotificationChannel(channel)
            }
        }

        private fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        private fun createSafeNotificationId(): Int {
            return abs((System.currentTimeMillis() % Int.MAX_VALUE).toInt()).coerceAtLeast(1)
        }
    }
}