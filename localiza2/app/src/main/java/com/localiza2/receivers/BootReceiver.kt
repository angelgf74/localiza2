package com.localiza2.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.localiza2.workers.WatchdogWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Arrancar vigilante inmediato y programar el periódico
                WatchdogWorker.scheduleImmediateRestart(context)
                WatchdogWorker.schedulePeriodicWatch(context)
            }
        }
    }
}
