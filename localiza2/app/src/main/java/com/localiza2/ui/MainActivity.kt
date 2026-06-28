package com.localiza2.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.localiza2.api.RetrofitClient
import com.localiza2.databinding.ActivityMainBinding
import com.localiza2.models.CreateShareLinkDto
import com.localiza2.models.SetSharingDto
import com.localiza2.services.LocationService
import com.localiza2.ui.auth.AuthActivity
import com.localiza2.ui.help.HelpBottomSheet
import com.localiza2.ui.suggestions.SuggestionsBottomSheet
import com.localiza2.utils.BatteryOptimizationHelper
import com.localiza2.utils.SessionManager
import com.localiza2.workers.WatchdogWorker
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private var sharingEnabled = true

    private val foregroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) {
            requestBackgroundLocation()
        } else {
            Toast.makeText(this, "La app necesita acceso a la ubicación para funcionar", Toast.LENGTH_LONG).show()
        }
    }

    private val backgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        startLocationService()
        checkBatteryRestrictions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = bars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(com.localiza2.R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                com.localiza2.R.id.action_help -> {
                    HelpBottomSheet.newInstance().show(supportFragmentManager, "help")
                    true
                }
                com.localiza2.R.id.action_toggle_sharing -> {
                    toggleSharing()
                    true
                }
                com.localiza2.R.id.action_suggestions -> {
                    SuggestionsBottomSheet.newInstance().show(supportFragmentManager, "suggestions")
                    true
                }
                com.localiza2.R.id.action_share_link -> {
                    createAndShareLink()
                    true
                }
                com.localiza2.R.id.action_logout -> {
                    confirmLogout()
                    true
                }
                com.localiza2.R.id.action_delete_account -> {
                    confirmDeleteAccount()
                    true
                }
                else -> false
            }
        }

        requestPermissionsAndStartService()
        loadSharingStatus()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { moveTaskToBack(true) }
        })
    }

    override fun onResume() {
        super.onResume()
        BatteryOptimizationHelper.showIfServiceKilledTooOften(this)
    }

    private fun loadSharingStatus() {
        val api = RetrofitClient.create(sessionManager)
        lifecycleScope.launch {
            runCatching { api.getSharingStatus() }.getOrNull()?.body()?.let { status ->
                sharingEnabled = status.sharingEnabled
                updateSharingMenuItem()
            }
        }
    }

    private fun toggleSharing() {
        val api = RetrofitClient.create(sessionManager)
        val newValue = !sharingEnabled
        lifecycleScope.launch {
            runCatching { api.setSharing(SetSharingDto(newValue)) }.getOrNull()?.body()?.let { status ->
                sharingEnabled = status.sharingEnabled
                updateSharingMenuItem()
                val msg = if (sharingEnabled) "Compartición reanudada" else "Compartición pausada"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSharingMenuItem() {
        val item = binding.toolbar.menu.findItem(com.localiza2.R.id.action_toggle_sharing) ?: return
        item.isChecked = sharingEnabled
        item.title = if (sharingEnabled) "Pausar compartición" else "Reanudar compartición"
    }

    private fun createAndShareLink() {
        val api = RetrofitClient.create(sessionManager)
        lifecycleScope.launch {
            try {
                val resp = api.createShareLink(CreateShareLinkDto(60))
                val link = resp.body()
                if (resp.isSuccessful && link != null) {
                    startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link.url)
                            putExtra(Intent.EXTRA_SUBJECT, "Mi ubicación en tiempo real")
                        },
                        "Compartir enlace"
                    ))
                } else {
                    Toast.makeText(this@MainActivity, "Error al crear el enlace", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Sin conexión. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissionsAndStartService() {
        val foregroundGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        when {
            foregroundGranted && backgroundGranted -> {
                startLocationService()
                checkBatteryRestrictions()
            }
            foregroundGranted -> requestBackgroundLocation()
            else -> showForegroundLocationRationale()
        }
    }

    private fun showForegroundLocationRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Acceso a la ubicación")
            .setMessage(
                "localiza2 necesita acceder a tu ubicación para compartirla en tiempo real " +
                "con los contactos que tú autorices. Tu posición se envía al servidor de la app " +
                "y solo es visible para dichos contactos. No se comparte con terceros ni se usa " +
                "con fines publicitarios. Puedes consultar nuestra política de privacidad en " +
                "localiza2.app/privacidad."
            )
            .setPositiveButton("Continuar") { _, _ ->
                val needed = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    needed += Manifest.permission.POST_NOTIFICATIONS
                }
                foregroundPermissionLauncher.launch(needed.toTypedArray())
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(this, "La app necesita acceso a la ubicación para funcionar", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun requestBackgroundLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationService()
            checkBatteryRestrictions()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Ubicación en segundo plano")
            .setMessage("Para compartir tu posición aunque la app esté cerrada, selecciona «Permitir siempre» en la siguiente pantalla.")
            .setPositiveButton("Continuar") { _, _ ->
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Ahora no") { _, _ ->
                startLocationService()
                checkBatteryRestrictions()
            }
            .show()
    }

    private fun checkBatteryRestrictions() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            requestBatteryOptimizationExemption()
            return
        }
        BatteryOptimizationHelper.showOnceIfNeeded(this)
    }

    private fun requestBatteryOptimizationExemption() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Mantener en segundo plano")
            .setMessage("Para que localiza2 siga compartiendo tu ubicación cuando cierres la app, desactiva la optimización de batería para esta aplicación.")
            .setPositiveButton("Configurar") { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
            .setNegativeButton("Ahora no") { _, _ -> BatteryOptimizationHelper.showOnceIfNeeded(this) }
            .show()
    }

    private fun startLocationService() {
        startForegroundService(Intent(this, LocationService::class.java))
        WatchdogWorker.schedulePeriodicWatch(this)
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Seguro que quieres cerrar sesión?")
            .setPositiveButton("Cerrar sesión") { _, _ ->
                stopService(Intent(this, LocationService::class.java))
                sessionManager.clearSession()
                startActivity(Intent(this, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDeleteAccount() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("Se borrarán tu cuenta, todos tus contactos y todo el historial de ubicación. Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> doDeleteAccount() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun doDeleteAccount() {
        val api = RetrofitClient.create(sessionManager)
        lifecycleScope.launch {
            try {
                val response = api.deleteAccount()
                if (response.isSuccessful) {
                    stopService(Intent(this@MainActivity, LocationService::class.java))
                    sessionManager.clearSession()
                    startActivity(Intent(this@MainActivity, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                } else {
                    Toast.makeText(this@MainActivity, "Error al eliminar la cuenta. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Sin conexión. Inténtalo de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
