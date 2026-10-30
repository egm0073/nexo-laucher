package com.nexo.launcher

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Ayudas del sistema para el modo cuentos. El bloqueo de verdad lo hace la función «Fijar aplicación»
 * de Android (con PIN para dejar de fijar); aquí solo se comprueba que esté activada y se silencian
 * las llamadas con No molestar mientras dura el modo.
 */
class KidsLock(private val context: Context) {

    private val notifications = context.getSystemService(NotificationManager::class.java)

    /** true/false si se puede saber; null si el sistema no deja leer el ajuste. */
    fun pinningEnabled(): Boolean? = runCatching {
        Settings.System.getInt(context.contentResolver, "lock_to_app_enabled")
    }.getOrNull()?.let { it != 0 }

    fun openPinningSettings() {
        start(Intent(Settings.ACTION_SECURITY_SETTINGS)) || start(Intent(Settings.ACTION_SETTINGS))
    }

    fun hasDndAccess(): Boolean = notifications.isNotificationPolicyAccessGranted

    fun openDndAccessSettings() {
        start(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    /** Activa No molestar dejando solo las alarmas. Devuelve el modo anterior para restaurarlo, o -1. */
    fun silenceCalls(): Int {
        if (!hasDndAccess()) return -1
        val previous = notifications.currentInterruptionFilter
        runCatching { notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS) }
        return previous
    }

    fun restore(previousFilter: Int) {
        if (previousFilter <= 0 || !hasDndAccess()) return
        runCatching { notifications.setInterruptionFilter(previousFilter) }
    }

    private fun start(intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
