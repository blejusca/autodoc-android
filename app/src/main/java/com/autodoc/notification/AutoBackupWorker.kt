package com.autodoc.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.autodoc.data.BackupManager

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            BackupManager.saveBackupToFile(applicationContext)
            Result.success()
        } catch (e: Exception) {
            // 🔒 NU mai retry → evitam loop + crash
            Result.success()
        }
    }
}