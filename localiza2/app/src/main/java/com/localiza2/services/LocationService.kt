package com.localiza2.services

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.localiza2.R
import com.localiza2.api.RetrofitClient
import com.localiza2.db.AppDatabase
import com.localiza2.db.PendingLocation
import com.localiza2.models.UpdateLocationDto
import com.localiza2.utils.BatteryOptimizationHelper
import com.localiza2.utils.SessionManager
import com.localiza2.workers.WatchdogWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var sessionManager: SessionManager
    private lateinit var apiService: com.localiza2.api.ApiService
    private lateinit var db: AppDatabase
    private lateinit var connectivityManager: ConnectivityManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Intervalo adaptativo ──────────────────────────────────────────────────
    private var lastLocation: Location? = null
    private var stillCount = 0
    private var isSlowMode = false

    // ── Cola offline ─────────────────────────────────────────────────────────
    private val flushMutex = Mutex()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            serviceScope.launch { flushPendingLocations() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        apiService = RetrofitClient.create(sessionManager)
        db = AppDatabase.getInstance(this)
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRunning) BatteryOptimizationHelper.recordServiceRestart(this)
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        startLocationUpdates()
        WatchdogWorker.schedulePeriodicWatch(this)
        runCatching { connectivityManager.registerDefaultNetworkCallback(networkCallback) }
        return START_STICKY
    }

    // ── Batería ───────────────────────────────────────────────────────────────

    private fun readBatteryLevel(): Int? {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return null
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) (level * 100 / scale) else null
    }

    // ── Callback de localización ──────────────────────────────────────────────

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                updateAdaptiveInterval(location)
                val battery = readBatteryLevel()
                serviceScope.launch {
                    sendOrQueue(
                        UpdateLocationDto(location.latitude, location.longitude,
                            location.accuracy.toDouble(), battery)
                    )
                }
            }
        }
    }

    // ── Intervalo adaptativo ──────────────────────────────────────────────────

    private fun updateAdaptiveInterval(newLocation: Location) {
        val prev = lastLocation
        lastLocation = newLocation
        if (prev == null) return

        if (newLocation.distanceTo(prev) > STILL_THRESHOLD_METERS) {
            // Se ha movido: volver a modo rápido
            stillCount = 0
            if (isSlowMode) switchMode(slow = false)
        } else {
            // Sin movimiento significativo
            stillCount++
            if (!isSlowMode && stillCount >= STILL_CHECKS_TO_SLOW) switchMode(slow = true)
        }
    }

    private fun switchMode(slow: Boolean) {
        isSlowMode = slow
        if (!slow) stillCount = 0
        restartLocationUpdates()
        updateNotification()
    }

    private fun startLocationUpdates() {
        val interval = if (isSlowMode) INTERVAL_SLOW_MS else INTERVAL_FAST_MS
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval / 2)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    private fun restartLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        startLocationUpdates()
    }

    // ── Cola offline ──────────────────────────────────────────────────────────

    private suspend fun sendOrQueue(dto: UpdateLocationDto) {
        val ok = runCatching { apiService.updateLocation(dto) }
            .map { it.isSuccessful }
            .getOrDefault(false)

        if (ok) {
            serviceScope.launch { flushPendingLocations() }
        } else {
            db.pendingLocationDao().insert(
                PendingLocation(
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    accuracy = dto.accuracy,
                    batteryLevel = dto.batteryLevel
                )
            )
        }
    }

    private suspend fun flushPendingLocations() {
        if (!flushMutex.tryLock()) return
        try {
            val pending = db.pendingLocationDao().getOldest()
            if (pending.isEmpty()) return
            val sentIds = mutableListOf<Int>()
            for (loc in pending) {
                val ok = runCatching {
                    apiService.updateLocation(
                        UpdateLocationDto(loc.latitude, loc.longitude, loc.accuracy, loc.batteryLevel)
                    )
                }.map { it.isSuccessful }.getOrDefault(false)

                if (ok) sentIds.add(loc.id) else break   // cortar al primer fallo
            }
            if (sentIds.isNotEmpty()) db.pendingLocationDao().deleteByIds(sentIds)
        } finally {
            flushMutex.unlock()
        }
    }

    // ── Notificación ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val text = if (isSlowMode) "Compartiendo ubicación — modo ahorro" else "Compartiendo tu ubicación"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("localiza2")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "localiza2 ubicación", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onTaskRemoved(rootIntent: Intent?) {
        WatchdogWorker.scheduleImmediateRestart(this)
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        isRunning = false
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID          = "localiza2_location"
        private const val NOTIFICATION_ID     = 1
        const val INTERVAL_FAST_MS            = 60_000L    // 1 min en movimiento
        const val INTERVAL_SLOW_MS            = 300_000L   // 5 min en reposo
        private const val STILL_THRESHOLD_METERS = 30f     // < 30 m = sin movimiento
        private const val STILL_CHECKS_TO_SLOW  = 2        // 2 min quieto → modo ahorro

        @Volatile
        var isRunning: Boolean = false
            private set
    }
}
