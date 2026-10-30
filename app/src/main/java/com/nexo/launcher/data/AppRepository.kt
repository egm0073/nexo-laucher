package com.nexo.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
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
import android.provider.MediaStore
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
    val category: AppCategory = AppCategory.OTHER,
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
                result += AppInfo(
                    key, info.label.toString().trim(), cn.packageName, cn, user, user != me,
                    category = autoCategory(info.applicationInfo, cn.packageName),
                )
            }
        }
        return result.sortedWith(compareBy(collator) { it.label })
    }

    /** Icono de la app. Con [monochrome] devuelve una silueta (capa monocroma o primer plano). */
    suspend fun icon(key: String, monochrome: Boolean): ImageBitmap? {
        val cacheKey = if (monochrome) "$key|m2" else key
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
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val mono = if (Build.VERSION.SDK_INT >= 33 && d is AdaptiveIconDrawable) d.monochrome else null
        if (mono != null) {
            // Capa monocroma oficial (Android 13+). Las capas adaptativas miden 108dp con una zona útil de 72dp.
            val pad = size / 4
            mono.setBounds(-pad, -pad, size + pad, size + pad)
            mono.draw(canvas)
            return bmp
        }
        d.setBounds(0, 0, size, size)
        d.draw(canvas)
        return extractGlyph(bmp)
    }

    /**
     * Sin capa monocroma, la forma del icono suele ser un cuadrado o círculo opaco. Se busca el color
     * de fondo (el más repetido en el borde) y se deja solo lo que contrasta con él: el dibujo.
     */
    private fun extractGlyph(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        val band = (minOf(w, h) * 0.2f).toInt()
        val counts = HashMap<Int, Int>()
        for (y in 0 until h) for (x in 0 until w) {
            if (minOf(x, y, w - 1 - x, h - 1 - y) >= band) continue
            val c = px[y * w + x]
            if ((c ushr 24) < 200) continue
            // Colores agrupados a 5 bits por canal para tolerar degradados suaves.
            val q = ((c shr 19) and 0x1F shl 10) or ((c shr 11) and 0x1F shl 5) or ((c shr 3) and 0x1F)
            counts[q] = (counts[q] ?: 0) + 1
        }
        val bgKey = counts.maxByOrNull { it.value }?.key ?: return src
        val bgR = (bgKey shr 10 and 0x1F) * 8 + 4
        val bgG = (bgKey shr 5 and 0x1F) * 8 + 4
        val bgB = (bgKey and 0x1F) * 8 + 4

        val out = IntArray(w * h)
        var glyph = 0
        var opaque = 0
        for (i in px.indices) {
            val c = px[i]
            val a = c ushr 24
            if (a < 32) continue
            opaque++
            val dr = (c shr 16 and 0xFF) - bgR
            val dg = (c shr 8 and 0xFF) - bgG
            val db = (c and 0xFF) - bgB
            val dist = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat())
            // Borde suave entre 40 y 90 de distancia de color.
            val k = ((dist - 40f) / 50f).coerceIn(0f, 1f)
            val alpha = (k * a).toInt()
            if (alpha > 0) {
                out[i] = alpha shl 24 // negro con transparencia; el color final lo pone el tinte
                if (alpha > 128) glyph++
            }
        }
        // Si casi no hay contraste (icono de un solo color) se usa la forma completa.
        if (opaque == 0 || glyph < opaque * 0.02f) return src
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Apps de la barra de inicio: teléfono, WhatsApp, mensajes y cámara, en ese orden (null si falta alguna).
     * Se busca qué app abre cada acción y, dentro de su paquete, la entrada del cajón que le corresponde.
     */
    fun dockApps(apps: List<AppInfo>): List<AppInfo?> {
        val pm = context.packageManager
        val me = Process.myUserHandle()
        fun find(vararg intents: Intent): AppInfo? {
            for (intent in intents) {
                val activity = runCatching { pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) }
                    .getOrNull()?.activityInfo ?: continue
                // "android" es el selector del sistema: hay varias apps y ninguna es la predeterminada.
                if (activity.packageName == "android") continue
                val inPackage = apps.filter { it.packageName == activity.packageName && it.user == me }
                // Algunos paquetes tienen varias entradas (Teléfono y Contactos en Samsung).
                val app = inPackage.firstOrNull { it.component.className == activity.name }
                    ?: inPackage.firstOrNull { !it.label.lowercase().startsWith("contact") }
                    ?: inPackage.firstOrNull()
                if (app != null) return app
            }
            return null
        }
        return listOf(
            find(Intent(Intent.ACTION_DIAL)),
            byPackage(apps, "com.whatsapp", "com.whatsapp.w4b"),
            find(
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            ),
            find(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), Intent(MediaStore.ACTION_IMAGE_CAPTURE)),
        )
    }

    /** Primera app del perfil personal que pertenece a alguno de los paquetes, por orden de preferencia. */
    fun byPackage(apps: List<AppInfo>, vararg packages: String): AppInfo? {
        val me = Process.myUserHandle()
        return packages.firstNotNullOfOrNull { pkg -> apps.firstOrNull { it.packageName == pkg && it.user == me } }
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
