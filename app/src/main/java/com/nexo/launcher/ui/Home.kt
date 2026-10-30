package com.nexo.launcher.ui

import android.app.AlarmManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.BatteryManager
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.AppRepository
import com.nexo.launcher.data.Folder
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
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
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
                .padding(top = 28.dp, bottom = 8.dp),
            horizontalAlignment = settings.align.horizontal(),
        ) {
            if (settings.showClock || settings.showDate) ClockHeader(settings)
            InfoLine(settings)

            if (homeWidgets.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                homeWidgets.forEach { entry ->
                    key(entry.id) {
                        HostedWidget(widgetHost, entry, Modifier.fillMaxWidth().height(entry.heightDp.dp))
                        Spacer(Modifier.height(12.dp))
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
                    if (settings.homeItems.isEmpty()) {
                        Text(
                            "Desliza hacia arriba para ver tus apps y mantén pulsada una para añadirla aquí.",
                            style = settings.homeTextStyle(scale = 0.6f, alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                    settings.homeItems.forEach { item ->
                        key(item) {
                            if (item.startsWith("app:")) {
                                val app = byKey[item.removePrefix("app:")]
                                if (app != null) {
                                    AppRow(
                                        app = app,
                                        repo = repo,
                                        iconStyle = settings.homeIconStyle,
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
                    .padding(8.dp),
            )
        }
    }
}

/** Deslizar arriba/abajo y mantener pulsado el fondo, común a las dos pantallas de inicio. */
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
    return this
        .pointerInput(Unit) {
            val threshold = 64.dp.toPx()
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    if (total < -threshold) swipeUp()
                    else if (total > threshold && downEnabled) swipeDown()
                },
                onVerticalDrag = { change, dy ->
                    total += dy
                    change.consume()
                },
            )
        }
        .pointerInput(Unit) { detectTapGestures(onLongPress = { longPress() }) }
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
fun HostedWidget(host: AppWidgetHost, entry: WidgetEntry, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val info = remember(entry.id) { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(entry.id) }
    val viewRef = remember { AtomicReference<AppWidgetHostView?>(null) }

    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        if (info == null) {
            Text(
                "Widget no disponible",
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AndroidView(
                factory = { c -> host.createView(c, entry.id, info).also { viewRef.set(it) } },
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
