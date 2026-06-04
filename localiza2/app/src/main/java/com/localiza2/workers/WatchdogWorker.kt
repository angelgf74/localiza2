package com.localiza2.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.localiza2.R
import com.localiza2.services.LocationService
import java.util.concurrent.TimeUnit

class WatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!LocationService.isRunning) {
            setForeground(createForegroundInfo())
            applicationContext.startForegroundService(
                Intent(applicationContext, LocationService::class.java)
            )
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "localiza2_watchdog"
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "localiza2", NotificationManager.IMPORTANCE_MIN)
        )
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("localiza2")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        return ForegroundInfo(3, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
    }

    companion object {
        private const val PERIODIC_WORK = "localiza2_watchdog_periodic"
        private const val IMMEDIATE_WORK = "localiza2_watchdog_restart"

        fun schedulePeriodicWatch(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleImmediateRestart(context: Context) {
            val request = OneTimeWorkRequestBuilder<WatchdogWorker>()
                .setInitialDelay(3, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
