package com.localiza2.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity

/**
 * Detecta fabricantes con gestión agresiva de batería y guía al usuario
 * a la pantalla de ajustes específica de su ROM para desactivar las restricciones.
 */
object BatteryOptimizationHelper {

    private const val PREFS_NAME = "localiza2_battery"
    private const val KEY_DIALOG_SHOWN = "oem_dialog_shown"
    private const val KEY_RESTART_COUNT = "service_restart_count"
    private const val RESTART_THRESHOLD = 3  // Avisar si el servicio se reinicia ≥3 veces

    // ── Detección de fabricante ────────────────────────────────────────────────

    enum class OEM {
        XIAOMI, HUAWEI, SAMSUNG, ONEPLUS, OPPO, VIVO, SONY, UNKNOWN
    }

    private fun detectOEM(): OEM {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            "xiaomi" in manufacturer || "redmi" in brand || "poco" in brand -> OEM.XIAOMI
            "huawei" in manufacturer || "honor" in brand                    -> OEM.HUAWEI
            "samsung" in manufacturer                                        -> OEM.SAMSUNG
            "oneplus" in manufacturer                                        -> OEM.ONEPLUS
            "oppo" in manufacturer || "realme" in brand                     -> OEM.OPPO
            "vivo" in manufacturer || "iqoo" in brand                       -> OEM.VIVO
            "sony" in manufacturer                                           -> OEM.SONY
            else                                                             -> OEM.UNKNOWN
        }
    }

    fun isAggressiveOEM(): Boolean = detectOEM() != OEM.UNKNOWN

    // ── Conteo de reinicios del servicio ──────────────────────────────────────

    fun recordServiceRestart(context: Context) {
        val prefs = prefs(context)
        val count = prefs.getInt(KEY_RESTART_COUNT, 0) + 1
        prefs.edit().putInt(KEY_RESTART_COUNT, count).apply()
    }

    fun getRestartCount(context: Context): Int =
        prefs(context).getInt(KEY_RESTART_COUNT, 0)

    fun resetRestartCount(context: Context) =
        prefs(context).edit().putInt(KEY_RESTART_COUNT, 0).apply()

    // ── Diálogos de aviso ─────────────────────────────────────────────────────

    /**
     * Muestra el diálogo de OEM una sola vez (en el primer arranque).
     * Solo aparece si el fabricante tiene restricciones conocidas.
     */
    fun showOnceIfNeeded(activity: AppCompatActivity) {
        val oem = detectOEM()
        if (oem == OEM.UNKNOWN) return
        if (prefs(activity).getBoolean(KEY_DIALOG_SHOWN, false)) return

        prefs(activity).edit().putBoolean(KEY_DIALOG_SHOWN, true).apply()
        showDialog(activity, oem, isUrgent = false)
    }

    /**
     * Muestra el diálogo cuando se detecta que el servicio está siendo matado
     * frecuentemente (umbral: RESTART_THRESHOLD reinicios).
     */
    fun showIfServiceKilledTooOften(activity: AppCompatActivity) {
        if (getRestartCount(activity) < RESTART_THRESHOLD) return
        resetRestartCount(activity)
        showDialog(activity, detectOEM(), isUrgent = true)
    }

    private fun showDialog(activity: AppCompatActivity, oem: OEM, isUrgent: Boolean) {
        val title = if (isUrgent)
            "El servicio de ubicación se está reiniciando"
        else
            "Ajuste recomendado para ${oemName(oem)}"

        val message = buildMessage(oem, isUrgent)
        val intent = resolveSettingsIntent(activity, oem)

        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Abrir ajustes") { _, _ ->
                runCatching { activity.startActivity(intent) }.onFailure {
                    // Fallback a ajustes de la app estándar
                    activity.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    )
                }
            }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    // ── Mensajes por fabricante ────────────────────────────────────────────────

    private fun buildMessage(oem: OEM, isUrgent: Boolean): String {
        val urgentPrefix = if (isUrgent)
            "Tu dispositivo está cerrando el servicio de ubicación. Para evitarlo:\n\n"
        else
            "Tu dispositivo ${oemName(oem)} tiene un sistema de ahorro de batería que puede " +
            "interrumpir el servicio de ubicación. Para evitarlo:\n\n"

        val steps = when (oem) {
            OEM.XIAOMI ->
                "1. Abre la app Seguridad\n" +
                "2. Ve a «Ahorro de batería» → busca localiza2\n" +
                "3. Selecciona «Sin restricciones»\n\n" +
                "También en Seguridad → «Permisos» → «Inicio automático»\n" +
                "activa el interruptor de localiza2."

            OEM.HUAWEI ->
                "1. Ajustes → Batería → «Inicio de aplicaciones»\n" +
                "2. Busca localiza2 y desactiva la gestión automática\n" +
                "3. Activa: «Ejecutar en segundo plano», " +
                "«Ejecutar tras encendido de pantalla» y «Ejecución automática»."

            OEM.SAMSUNG ->
                "1. Ajustes → Cuidado del dispositivo → Batería\n" +
                "2. Ve a «Límites de uso en segundo plano»\n" +
                "3. Asegúrate de que localiza2 NO aparece en «Apps en suspensión» " +
                "ni en «Apps desactivadas».\n\n" +
                "Opcionalmente: en Batería → Optimización de batería → localiza2 → " +
                "«No optimizar»."

            OEM.ONEPLUS ->
                "1. Ajustes → Batería → «Optimización de batería»\n" +
                "2. Busca localiza2 y selecciona «No optimizar»\n\n" +
                "Además en Ajustes → Aplicaciones → localiza2 → Batería\n" +
                "activa «Ejecución en segundo plano»."

            OEM.OPPO ->
                "1. Ajustes → Batería → «Ahorro de energía»\n" +
                "2. Busca localiza2 → selecciona «Sin restricciones»\n\n" +
                "También en Ajustes → Aplicaciones → localiza2\n" +
                "activa «Ejecución en segundo plano»."

            OEM.VIVO ->
                "1. Ajustes → Batería → «Alto consumo en segundo plano»\n" +
                "2. Activa localiza2 en la lista."

            OEM.SONY ->
                "1. Ajustes → Batería → «Administración de energía»\n" +
                "2. Desactiva la administración de energía para localiza2."

            OEM.UNKNOWN ->
                "Ve a Ajustes → Batería → Optimización de batería\n" +
                "y selecciona «No optimizar» para localiza2."
        }

        return urgentPrefix + steps
    }

    private fun oemName(oem: OEM) = when (oem) {
        OEM.XIAOMI  -> "Xiaomi/MIUI"
        OEM.HUAWEI  -> "Huawei/EMUI"
        OEM.SAMSUNG -> "Samsung/OneUI"
        OEM.ONEPLUS -> "OnePlus/OxygenOS"
        OEM.OPPO    -> "OPPO/ColorOS"
        OEM.VIVO    -> "Vivo/FuntouchOS"
        OEM.SONY    -> "Sony"
        OEM.UNKNOWN -> "este dispositivo"
    }

    // ── Intents de ajustes por fabricante ─────────────────────────────────────

    private fun resolveSettingsIntent(context: Context, oem: OEM): Intent {
        val candidates: List<Intent> = when (oem) {
            OEM.XIAOMI -> listOf(
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                },
                Intent().apply {
                    setClassName(
                        "com.miui.powerkeeper",
                        "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                    )
                    putExtra("package_name", context.packageName)
                    putExtra("package_label", context.getString(
                        context.applicationInfo.labelRes))
                }
            )

            OEM.HUAWEI -> listOf(
                Intent().apply {
                    setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                },
                Intent().apply {
                    setClassName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
            )

            OEM.SAMSUNG -> listOf(
                Intent().apply {
                    setClassName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                },
                Intent().apply {
                    setClassName(
                        "com.samsung.android.sm.battery",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                }
            )

            OEM.ONEPLUS -> listOf(
                Intent().apply {
                    setClassName(
                        "com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                    )
                }
            )

            OEM.OPPO -> listOf(
                Intent().apply {
                    setClassName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                },
                Intent().apply {
                    setClassName(
                        "com.oppo.safe",
                        "com.oppo.safe.permission.startup.StartupAppListActivity"
                    )
                }
            )

            OEM.VIVO -> listOf(
                Intent().apply {
                    setClassName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }
            )

            OEM.SONY -> listOf(
                Intent().apply {
                    action = "com.sonymobile.intent.action.STAMINA_APP"
                    putExtra("package_name", context.packageName)
                }
            )

            OEM.UNKNOWN -> emptyList()
        }

        val pm = context.packageManager
        return candidates.firstOrNull { pm.resolveActivity(it, 0) != null }
            ?: Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
