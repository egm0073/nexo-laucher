package com.nexo.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.concurrent.ConcurrentHashMap

data class AppInfo(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val isWork: Boolean,
)

class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val infos = ConcurrentHashMap<String, LauncherActivityInfo>()
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private var loadJob: Job? = null

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh(packageName)
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh(packageName)
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh(null)
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh(null)
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh(null)
    }

    fun refresh(changedPackage: String?) {
        if (changedPackage == null) iconCache.clear()
        else iconCache.keys.removeIf { it.startsWith("$changedPackage/") }
        loadJob?.cancel()
        loadJob = scope.launch {
            val list = loadApps()
            ensureActive()
            _apps.value = list
        }
    }

    private fun loadApps(): List<AppInfo> {
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        val me = Process.myUserHandle()
        val result = mutableListOf<AppInfo>()
        for (user in userManager.userProfiles) {
            val serial = userManager.getSerialNumberForUser(user)
            val list = runCatching { launcherApps.getActivityList(null, user) }.getOrNull().orEmpty()
            for (info in list) {
                val cn = info.componentName
                if (cn.packageName == context.packageName) continue
                val key = "${cn.flattenToString()}#$serial"
                infos[key] = info
                result += AppInfo(key, info.label.toString().trim(), cn.packageName, cn, user, user != me)
            }
        }
        return result.sortedWith(compareBy(collator) { it.label })
    }

    /** Icono de la app. Con [monochrome] devuelve una silueta (capa monocroma o primer plano). */
    suspend fun icon(key: String, monochrome: Boolean): ImageBitmap? {
        val cacheKey = if (monochrome) "$key|m" else key
        iconCache[cacheKey]?.let { return it }
        return withContext(Dispatchers.IO) {
            val info = infos[key] ?: return@withContext null
            runCatching {
                val bmp = if (monochrome) renderMono(info.getIcon(0), ICON_PX)
                else info.getBadgedIcon(0).toBitmap(ICON_PX, ICON_PX)
                bmp.asImageBitmap()
            }.getOrNull()?.also { iconCache[cacheKey] = it }
        }
    }

    private fun renderMono(drawable: Drawable, size: Int): Bitmap {
        val d = drawable.mutate()
        val layer: Drawable? = if (d is AdaptiveIconDrawable) {
            if (Build.VERSION.SDK_INT >= 33) d.monochrome ?: d.foreground else d.foreground
        } else null
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (layer != null) {
            // Las capas adaptativas miden 108dp con una zona útil de 72dp.
            val pad = size / 4
            layer.setBounds(-pad, -pad, size + pad, size + pad)
            layer.draw(canvas)
        } else {
            d.setBounds(0, 0, size, size)
            d.draw(canvas)
        }
        return bmp
    }

    fun launch(app: AppInfo) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, null, null) }
    }

    fun openAppInfo(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppInfo) {
        runCatching {
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                .putExtra(Intent.EXTRA_USER, app.user)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    companion object {
        const val ICON_PX = 144
    }
}
