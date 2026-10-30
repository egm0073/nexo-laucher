package com.nexo.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Ayudas del sistema para el modo cuentos. El bloqueo de verdad lo hace la función «Fijar aplicación»
 * de Android (con PIN para dejar de fijar); aquí solo se comprueba que esté activada.
 */
class KidsLock(private val context: Context) {

    /** true/false si se puede saber; null si el sistema no deja leer el ajuste. */
    fun pinningEnabled(): Boolean? = runCatching {
        Settings.System.getInt(context.contentResolver, "lock_to_app_enabled")
    }.getOrNull()?.let { it != 0 }

    fun openPinningSettings() {
        start(Intent(Settings.ACTION_SECURITY_SETTINGS)) || start(Intent(Settings.ACTION_SETTINGS))
    }

    private fun start(intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}
