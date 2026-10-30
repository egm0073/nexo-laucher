package com.nexo.launcher.ui

import android.app.AlarmManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.BatteryManager
import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexo.launcher.LongPressWidgetView
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.AppRepository
import com.nexo.launcher.data.Folder
import com.nexo.launcher.data.IconStyle
import com.nexo.launcher.data.LauncherSettings
import com.nexo.launcher.data.WidgetEntry
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

@Composable
fun HomeScreen(
    settings: LauncherSettings,
    apps: List<AppInfo>,
    repo: AppRepository,
    widgetHost: AppWidgetHost,
    onOpenDrawer: () -> Unit,
    onSwipeDown: () -> Unit,
    onLongPressBackground: () -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo, String?) -> Unit,
    onFolderLongPress: (Folder) -> Unit,
    editingWidgets: Boolean,
    onStartEditing: () -> Unit,
    onDoneEditing: () -> Unit,
    onResizeWidget: (WidgetEntry, widthDp: Int, heightDp: Int) -> Unit,
    onDeleteWidget: (Int) -> Unit,
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
    // YouTube y YouTube Music están siempre al principio de la lista y no se pueden quitar.
    val fixedApps = remember(apps) { FIXED_HOME_PACKAGES.mapNotNull { repo.byPackage(apps, it) } }
    val fixedKeys = remember(fixedApps) { fixedApps.map { "app:${it.key}" }.toSet() }
    val foldersById = remember(settings.folders) { settings.folders.associateBy { it.id } }
    var expandedFolder by rememberSaveable { mutableStateOf<String?>(null) }

    val homeWidgets = settings.widgets.filter { it.page == 0 }
    val itemStyle = settings.homeTextStyle()
    val folderAppStyle = settings.homeTextStyle(scale = 0.78f, alpha = 0.9f)
    val iconSize = settings.iconSizeDp.dp
    val scroll = rememberScrollState()

    Box(
        Modifier
            .fillMaxSize()
            .homeGestures(
                onSwipeUp = onOpenDrawer,
                onSwipeDown = onSwipeDown,
                swipeDownEnabled = settings.swipeDownNotifications,
                onLongPress = onLongPressBackground,
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 28.dp),
            horizontalAlignment = settings.align.horizontal(),
        ) {
            if (settings.showClock || settings.showDate) ClockHeader(settings)
            InfoLine(settings)

            if (homeWidgets.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val maxW = maxWidth.value
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = settings.align.horizontal()) {
                        homeWidgets.forEach { entry ->
                            key(entry.id) {
                                HomeWidget(
                                    host = widgetHost,
                                    entry = entry,
                                    editing = editingWidgets,
                                    maxW = maxW,
                                    onStartEditing = onStartEditing,
                                    onResize = onResizeWidget,
                                    onDelete = onDeleteWidget,
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = settings.align.bottom(),
            ) {
                Column(
                    Modifier.verticalScroll(scroll, enabled = scroll.maxValue > 0),
                    horizontalAlignment = settings.align.horizontal(),
                ) {
                    fixedApps.forEach { app ->
                        key("fixed:${app.key}") {
                            AppRow(
                                app = app,
                                repo = repo,
                                iconStyle = settings.homeIconStyle,
                                iconTint = settings.silhouetteTint(),
                                iconSize = iconSize,
                                textStyle = itemStyle,
                                align = settings.align,
                                onClick = { onLaunch(app) },
                                onLongClick = { onAppLongPress(app, null) },
                            )
                        }
                    }
                    if (settings.homeItems.isEmpty() && fixedApps.isEmpty()) {
                        Text(
                            "Desliza hacia arriba para ver tus apps y mantén pulsada una para añadirla aquí.",
                            style = settings.homeTextStyle(scale = 0.6f, alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                    settings.homeItems.filterNot { it in fixedKeys }.forEach { item ->
                        key(item) {
                            if (item.startsWith("app:")) {
                                val app = byKey[item.removePrefix("app:")]
                                if (app != null) {
                                    AppRow(
                                        app = app,
                                        repo = repo,
                                        iconStyle = settings.homeIconStyle,
                                        iconTint = settings.silhouetteTint(),
                                        iconSize = iconSize,
                                        textStyle = itemStyle,
                                        align = settings.align,
                                        onClick = { onLaunch(app) },
                                        onLongClick = { onAppLongPress(app, null) },
                                    )
                                }
                            } else if (item.startsWith("folder:")) {
                                val folder = foldersById[item.removePrefix("folder:")]
                                if (folder != null) {
                                    val expanded = expandedFolder == folder.id
                                    FolderRow(
                                        folder = folder,
                                        expanded = expanded,
                                        settings = settings,
                                        onClick = { expandedFolder = if (expanded) null else folder.id },
                                        onLongClick = { onFolderLongPress(folder) },
                                    )
                                    if (expanded) {
                                        folder.apps.mapNotNull { byKey[it] }.forEach { app ->
                                            AppRow(
                                                app = app,
                                                repo = repo,
                                                iconStyle = settings.homeIconStyle,
                                                iconTint = settings.silhouetteTint(),
                                                iconSize = iconSize * 0.8f,
                                                textStyle = folderAppStyle,
                                                align = settings.align,
                                                onClick = {
                                                    expandedFolder = null
                                                    onLaunch(app)
                                                },
                                                onLongClick = { onAppLongPress(app, folder.id) },
                                                modifier = Modifier.padding(start = 20.dp),
                                            )
                                        }
                                        if (folder.apps.isEmpty()) {
                                            Text(
                                                "Carpeta vacía: mantén pulsada una app para añadirla",
                                                style = settings.homeTextStyle(scale = 0.5f, alpha = 0.7f),
                                                modifier = Modifier.padding(start = 26.dp, bottom = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "︿",
                style = settings.homeTextStyle(scale = 0.7f, alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onOpenDrawer() }
                    .padding(horizontal = 24.dp, vertical = 2.dp),
            )

            // La barra va lo más abajo posible, justo encima de la barra de navegación del sistema.
            Dock(
                apps = remember(apps) { repo.dockApps(apps) },
                repo = repo,
                onLaunch = onLaunch,
                onLongPress = { onAppLongPress(it, null) },
            )
        }

        if (editingWidgets) EditWidgetsBar("Tira de la esquina para cambiar el ancho y el alto", onDoneEditing)
    }
}

private val FIXED_HOME_PACKAGES = listOf("com.google.android.youtube", "com.google.android.apps.youtube.music")

private val DOCK_ICON_SIZE = 40.dp

/** Siluetas negras semitransparentes, para que la barra se funda con el fondo y no llame la atención. */
private val DOCK_TINT = Color.Black.copy(alpha = 0.7f)

/** Teléfono, WhatsApp, mensajes y cámara como siluetas, siempre al pie de la pantalla de inicio. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Dock(
    apps: List<AppInfo?>,
    repo: AppRepository,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    if (apps.all { it == null }) return
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        apps.forEach { app ->
            if (app == null) return@forEach
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = { onLaunch(app) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress(app)
                        },
                    )
                    .padding(vertical = 6.dp)
                    .semantics { contentDescription = app.label },
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(app, repo, IconStyle.SILHOUETTE, DOCK_ICON_SIZE, DOCK_TINT)
            }
        }
    }
}

/** Widget de la pantalla principal: ancho (0 = completo) y alto ajustables en el modo de ajuste. */
@Composable
private fun HomeWidget(
    host: AppWidgetHost,
    entry: WidgetEntry,
    editing: Boolean,
    maxW: Float,
    onStartEditing: () -> Unit,
    onResize: (WidgetEntry, Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var w by remember(entry, maxW) {
        mutableFloatStateOf(if (entry.widthDp > 0) entry.widthDp.toFloat().coerceIn(MIN_WIDGET_DP, maxW) else maxW)
    }
    var h by remember(entry) { mutableFloatStateOf(entry.heightDp.toFloat()) }
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(20.dp)

    Box(Modifier.size(w.dp, h.dp)) {
        HostedWidget(host, entry, Modifier.fillMaxSize(), onLongPress = if (editing) null else onStartEditing)
        if (editing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(accent.copy(alpha = 0.15f))
                    .border(2.dp, accent, shape)
                    .pointerInput(Unit) { detectTapGestures { } },
            )
            ResizeHandle(
                key = Pair(entry, maxW),
                onDrag = { dx, dy ->
                    w = (w + dx).coerceIn(MIN_WIDGET_DP, maxW)
                    h = (h + dy).coerceIn(MIN_WIDGET_DP, 1200f)
                },
                // Si ocupa casi todo el ancho se guarda como "ancho completo".
                onDragEnd = { onResize(entry, if (w >= maxW - 1f) 0 else w.roundToInt(), h.roundToInt()) },
            )
            DeleteWidgetButton { onDelete(entry.id) }
        }
    }
}

/** Zonas de pantalla ocupadas por widgets (coordenadas de la raíz). */
class WidgetAreas {
    private val rects = mutableMapOf<Int, Rect>()
    fun set(id: Int, rect: Rect) { rects[id] = rect }
    fun remove(id: Int) { rects.remove(id) }
    operator fun contains(point: Offset): Boolean = rects.values.any { it.contains(point) }
}

val LocalWidgetAreas = staticCompositionLocalOf { WidgetAreas() }

/**
 * Deslizar arriba/abajo y mantener pulsado el fondo, común a las dos pantallas de inicio.
 * Los gestos que empiezan encima de un widget se dejan al widget, para que pueda desplazarse.
 */
@Composable
fun Modifier.homeGestures(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    swipeDownEnabled: Boolean,
    onLongPress: () -> Unit,
): Modifier {
    val swipeUp by rememberUpdatedState(onSwipeUp)
    val swipeDown by rememberUpdatedState(onSwipeDown)
    val downEnabled by rememberUpdatedState(swipeDownEnabled)
    val longPress by rememberUpdatedState(onLongPress)
    val widgets = LocalWidgetAreas.current
    val origin = remember { floatArrayOf(0f, 0f) }
    fun overWidget(local: Offset) = Offset(local.x + origin[0], local.y + origin[1]) in widgets

    return this
        .onGloballyPositioned { c ->
            val p = c.positionInRoot()
            origin[0] = p.x
            origin[1] = p.y
        }
        .pointerInput(Unit) {
            val threshold = 64.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (overWidget(down.position)) return@awaitEachGesture
                var total = 0f
                val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                    total += over
                    change.consume()
                } ?: return@awaitEachGesture
                verticalDrag(drag.id) { change ->
                    total += change.positionChange().y
                    change.consume()
                }
                if (total < -threshold) swipeUp()
                else if (total > threshold && downEnabled) swipeDown()
            }
        }
        .pointerInput(Unit) { detectTapGestures(onLongPress = { if (!overWidget(it)) longPress() }) }
}

@Composable
private fun FolderRow(
    folder: Folder,
    expanded: Boolean,
    settings: LauncherSettings,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(folder.id) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(folder.name, style = settings.homeTextStyle(weight = FontWeight.Medium))
        Spacer(Modifier.width(10.dp))
        Text(
            (if (expanded) "▾ " else "▸ ") + folder.apps.size,
            style = settings.homeTextStyle(scale = 0.55f, alpha = 0.7f),
        )
    }
}

@Composable
private fun ClockHeader(settings: LauncherSettings) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L - System.currentTimeMillis() % 1000L)
        }
    }
    val timeFmt = remember(settings.clock24) { DateTimeFormatter.ofPattern(if (settings.clock24) "HH:mm" else "h:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val noRipple = remember { MutableInteractionSource() }

    Column(horizontalAlignment = settings.align.horizontal()) {
        if (settings.showClock) {
            Text(
                now.format(timeFmt),
                style = settings.homeTextStyle(scale = 2.6f, weight = FontWeight.Light),
                modifier = Modifier.clickable(interactionSource = noRipple, indication = null) { openAlarms(ctx) },
            )
        }
        if (settings.showDate) {
            Text(
                now.format(dateFmt).replaceFirstChar { it.titlecase(Locale.getDefault()) },
                style = settings.homeTextStyle(scale = 0.7f, alpha = 0.85f),
                modifier = Modifier.clickable(interactionSource = noRipple, indication = null) { openCalendar(ctx) },
            )
        }
    }
}

@Composable
private fun InfoLine(settings: LauncherSettings) {
    if (!settings.showBattery && !settings.showNextAlarm) return
    val ctx = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    var alarm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(settings.clock24) {
        val battery = ctx.getSystemService(BatteryManager::class.java)
        val alarms = ctx.getSystemService(AlarmManager::class.java)
        val fmt = DateTimeFormatter.ofPattern(if (settings.clock24) "EEE HH:mm" else "EEE h:mm a", Locale.getDefault())
        while (true) {
            level = battery?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            charging = battery?.isCharging ?: false
            alarm = alarms?.nextAlarmClock?.triggerTime?.let {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()).format(fmt)
            }
            delay(30_000)
        }
    }

    val parts = buildList {
        if (settings.showBattery && level >= 0) add((if (charging) "⚡ " else "🔋 ") + "$level %")
        if (settings.showNextAlarm && alarm != null) add("⏰ $alarm")
    }
    if (parts.isNotEmpty()) {
        Text(
            parts.joinToString("   ·   "),
            style = settings.homeTextStyle(scale = 0.55f, alpha = 0.8f),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun HostedWidget(
    host: AppWidgetHost,
    entry: WidgetEntry,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val info = remember(entry.id) { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(entry.id) }
    val viewRef = remember { AtomicReference<AppWidgetHostView?>(null) }

    val areas = LocalWidgetAreas.current
    DisposableEffect(entry.id) { onDispose { areas.remove(entry.id) } }

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .onGloballyPositioned { areas.set(entry.id, it.boundsInRoot()) },
    ) {
        if (info == null) {
            Text(
                "Widget no disponible",
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AndroidView(
                factory = { c -> host.createView(c, entry.id, info).also { viewRef.set(it) } },
                update = { view -> (view as? LongPressWidgetView)?.onLongPress = onLongPress },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        val w = with(density) { size.width.toDp().value.toInt() }
                        val h = with(density) { size.height.toDp().value.toInt() }
                        @Suppress("DEPRECATION")
                        viewRef.get()?.updateAppWidgetSize(Bundle(), w, h, w, h)
                    },
            )
        }
    }
}
