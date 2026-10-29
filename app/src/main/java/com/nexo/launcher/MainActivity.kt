package com.nexo.launcher

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.nexo.launcher.ui.LauncherActions
import com.nexo.launcher.ui.LauncherApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity(), LauncherActions {

    private val vm: LauncherViewModel by viewModels()
    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private var pendingWidgetId = -1

    /** Se incrementa cada vez que se pulsa "Inicio" estando ya en el launcher. */
    private val homeSignal = MutableStateFlow(0)

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        if (id == -1) return@registerForActivityResult
        if (result.resultCode == RESULT_OK) configureOrCommit(id)
        else {
            widgetHost.deleteAppWidgetId(id)
            pendingWidgetId = -1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        widgetManager = AppWidgetManager.getInstance(this)
        widgetHost = AppWidgetHost(applicationContext, WIDGET_HOST_ID)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING, -1) ?: -1

        // En la pantalla principal el botón "atrás" no hace nada.
        onBackPressedDispatcher.addCallback(this) { }

        setContent {
            LauncherApp(
                vm = vm,
                widgetHost = widgetHost,
                homeSignal = homeSignal,
                actions = this@MainActivity,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeSignal.value = homeSignal.value + 1
    }

    override fun onStart() {
        super.onStart()
        runCatching { widgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { widgetHost.stopListening() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING, pendingWidgetId)
    }

    // ---------- Widgets ----------

    override fun addWidget(info: AppWidgetProviderInfo) {
        val id = widgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        val allowed = runCatching {
            widgetManager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)
        }.getOrDefault(false)
        if (allowed) {
            configureOrCommit(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
            bindWidget.launch(intent)
        }
    }

    private fun configureOrCommit(id: Int) {
        val info = widgetManager.getAppWidgetInfo(id)
        if (info == null) {
            widgetHost.deleteAppWidgetId(id)
            pendingWidgetId = -1
            return
        }
        if (info.configure != null) {
            pendingWidgetId = id
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(this, id, 0, REQ_CONFIGURE, null)
            } catch (e: Exception) {
                commitWidget(id, info)
            }
        } else {
            commitWidget(id, info)
        }
    }

    private fun commitWidget(id: Int, info: AppWidgetProviderInfo) {
        val density = resources.displayMetrics.density
        val heightDp = (info.minHeight / density).toInt().coerceIn(80, 420) + 8
        vm.addWidget(id, heightDp)
        pendingWidgetId = -1
    }

    @Deprecated("Necesario para la configuración de widgets del sistema")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_CONFIGURE) return
        val id = pendingWidgetId
        if (id == -1) return
        val info = widgetManager.getAppWidgetInfo(id)
        if (resultCode == RESULT_OK && info != null) commitWidget(id, info)
        else {
            widgetHost.deleteAppWidgetId(id)
            pendingWidgetId = -1
        }
    }

    override fun deleteWidget(id: Int) {
        runCatching { widgetHost.deleteAppWidgetId(id) }
        vm.removeWidget(id)
    }

    // ---------- Sistema ----------

    @SuppressLint("WrongConstant")
    override fun expandNotifications() {
        try {
            val service = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        } catch (_: Throwable) {
        }
    }

    override fun openDefaultLauncherSettings() {
        val ok = runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }.isSuccess
        if (!ok) runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    override fun openWallpaperPicker() {
        runCatching {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Fondo de pantalla"))
        }
    }

    companion object {
        private const val WIDGET_HOST_ID = 1024
        private const val REQ_CONFIGURE = 4210
        private const val KEY_PENDING = "pending_widget"
    }
}
