package com.nexo.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexo.launcher.KidsLock
import com.nexo.launcher.LauncherViewModel
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.Folder
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface LauncherActions {
    fun addWidget(info: AppWidgetProviderInfo, page: Int)
    fun deleteWidget(id: Int)
    fun expandNotifications()
    fun openDefaultLauncherSettings()
    fun openWallpaperPicker()
}

enum class Screen { HOME, DRAWER, SETTINGS }

private sealed interface SheetTarget {
    data class App(val app: AppInfo, val folderId: String?) : SheetTarget
    data class FolderMenu(val folder: Folder) : SheetTarget
    data object HomeMenu : SheetTarget
    data object WidgetPageMenu : SheetTarget
    data class WidgetPicker(val page: Int) : SheetTarget
    data object KidsAppPicker : SheetTarget
}

private sealed interface DialogTarget {
    data class NewFolder(val appKey: String?) : DialogTarget
    data class Rename(val folder: Folder) : DialogTarget
    data object KidsMode : DialogTarget
}

@Composable
fun LauncherApp(
    vm: LauncherViewModel,
    widgetHost: AppWidgetHost,
    homeSignal: StateFlow<Int>,
    actions: LauncherActions,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    val signal by homeSignal.collectAsStateWithLifecycle()

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }
    var dialog by remember { mutableStateOf<DialogTarget?>(null) }
    var editingWidgets by rememberSaveable { mutableStateOf(false) }
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val kidsLock = remember { KidsLock(ctx.applicationContext) }
    val kidsApp = remember(apps, settings.kidsApp) { apps.firstOrNull { it.key == settings.kidsApp } }

    // Pulsar "Inicio" vuelve siempre a la pantalla principal.
    LaunchedEffect(signal) {
        if (signal > 0) {
            screen = Screen.HOME
            sheet = null
            dialog = null
            editingWidgets = false
            pager.animateScrollToPage(0)
        }
    }

    // Al añadir un widget se entra directamente en el modo de ajuste.
    val widgetPageCount = settings.widgets.size
    var lastWidgetPageCount by remember { mutableStateOf(-1) }
    LaunchedEffect(widgetPageCount, settings.loaded) {
        if (!settings.loaded) return@LaunchedEffect
        if (lastWidgetPageCount >= 0 && widgetPageCount > lastWidgetPageCount) editingWidgets = true
        lastWidgetPageCount = widgetPageCount
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    BackHandler(enabled = screen == Screen.HOME && editingWidgets) { editingWidgets = false }
    BackHandler(enabled = screen == Screen.HOME && !editingWidgets && pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }

    val launch: (AppInfo) -> Unit = { app ->
        vm.repo.launch(app)
        vm.recordLaunch(app.key)
        screen = Screen.HOME
    }

    val widgetAreas = remember { WidgetAreas() }
    val homeAlpha by animateFloatAsState(if (screen == Screen.HOME) 1f else 0f, label = "homeAlpha")

    LauncherTheme(accent = Color(settings.accent)) {
        CompositionLocalProvider(LocalWidgetAreas provides widgetAreas) {
            Box(Modifier.fillMaxSize()) {
                if (settings.loaded) {
                    // Deslizar a la izquierda en inicio abre la segunda pantalla, la de widgets.
                    HorizontalPager(
                        state = pager,
                        userScrollEnabled = !editingWidgets,
                        beyondViewportPageCount = 1,
                        modifier = Modifier
                            .fillMaxSize()
                            // El cajón es transparente: la pantalla de inicio se oculta mientras está abierto.
                            .graphicsLayer { alpha = homeAlpha }
                            .background(Color.Black.copy(alpha = settings.dim)),
                    ) { page ->
                        if (page == 0) {
                            HomeScreen(
                                settings = settings,
                                apps = apps,
                                repo = vm.repo,
                                widgetHost = widgetHost,
                                onOpenDrawer = { screen = Screen.DRAWER },
                                onSwipeDown = { actions.expandNotifications() },
                                onLongPressBackground = { sheet = SheetTarget.HomeMenu },
                                onLaunch = launch,
                                onAppLongPress = { app, folderId -> sheet = SheetTarget.App(app, folderId) },
                                onFolderLongPress = { sheet = SheetTarget.FolderMenu(it) },
                                editingWidgets = editingWidgets && pager.currentPage == 0,
                                onStartEditing = { editingWidgets = true },
                                onDoneEditing = { editingWidgets = false },
                                onResizeWidget = { e, w, h -> vm.placeWidget(e.id, e.x, e.y, w, h) },
                                onDeleteWidget = { actions.deleteWidget(it) },
                            )
                        } else {
                            WidgetPage(
                                settings = settings,
                                widgetHost = widgetHost,
                                editing = editingWidgets && pager.currentPage == 1,
                                onOpenDrawer = { screen = Screen.DRAWER },
                                onSwipeDown = { actions.expandNotifications() },
                                onLongPressBackground = { sheet = SheetTarget.WidgetPageMenu },
                                onPlace = vm::placeWidget,
                                onDelete = { actions.deleteWidget(it) },
                                onStartEditing = { editingWidgets = true },
                                onDoneEditing = { editingWidgets = false },
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = screen == Screen.DRAWER,
                    enter = slideInVertically { it / 3 } + fadeIn(),
                    exit = slideOutVertically { it / 3 } + fadeOut(),
                ) {
                    AppDrawer(
                        settings = settings,
                        apps = apps,
                        repo = vm.repo,
                        onLaunch = launch,
                        onLongPress = { sheet = SheetTarget.App(it, null) },
                        onClose = { screen = Screen.HOME },
                        onOpenSettings = { screen = Screen.SETTINGS },
                    )
                }

                AnimatedVisibility(
                    visible = screen == Screen.SETTINGS,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut(),
                ) {
                    SettingsScreen(
                        settings = settings,
                        apps = apps,
                        vm = vm,
                        actions = actions,
                        onBack = { screen = Screen.HOME },
                        onAddWidget = { sheet = SheetTarget.WidgetPicker(0) },
                        onNewFolder = { dialog = DialogTarget.NewFolder(null) },
                        onRenameFolder = { dialog = DialogTarget.Rename(it) },
                        onKidsMode = { dialog = DialogTarget.KidsMode },
                    )
                }
            }

            when (val t = sheet) {
                is SheetTarget.App -> AppActionsSheet(
                    app = t.app,
                    folderId = t.folderId,
                    settings = settings,
                    vm = vm,
                    onDismiss = { sheet = null },
                    onNewFolder = {
                        sheet = null
                        dialog = DialogTarget.NewFolder(t.app.key)
                    },
                )

                is SheetTarget.FolderMenu -> FolderActionsSheet(
                    folder = t.folder,
                    vm = vm,
                    onDismiss = { sheet = null },
                    onRename = {
                        sheet = null
                        dialog = DialogTarget.Rename(t.folder)
                    },
                )

                SheetTarget.HomeMenu -> HomeMenuSheet(
                    onDismiss = { sheet = null },
                    onAddWidget = { sheet = SheetTarget.WidgetPicker(0) },
                    onNewFolder = {
                        sheet = null
                        dialog = DialogTarget.NewFolder(null)
                    },
                    onWallpaper = {
                        sheet = null
                        actions.openWallpaperPicker()
                    },
                    onKidsMode = {
                        sheet = null
                        dialog = DialogTarget.KidsMode
                    },
                    onSettings = {
                        sheet = null
                        screen = Screen.SETTINGS
                    },
                )

                SheetTarget.WidgetPageMenu -> WidgetPageMenuSheet(
                    hasWidgets = settings.widgets.any { it.page == 1 },
                    onDismiss = { sheet = null },
                    onAddWidget = { sheet = SheetTarget.WidgetPicker(1) },
                    onEdit = {
                        sheet = null
                        editingWidgets = true
                    },
                    onSettings = {
                        sheet = null
                        screen = Screen.SETTINGS
                    },
                )

                SheetTarget.KidsAppPicker -> KidsAppPickerSheet(
                    apps = apps.filter { it.key !in settings.hidden },
                    onPick = {
                        vm.setKidsApp(it.key)
                        sheet = null
                    },
                    onDismiss = { sheet = null },
                )

                is SheetTarget.WidgetPicker -> WidgetPickerSheet(
                    onPick = {
                        sheet = null
                        actions.addWidget(it, t.page)
                    },
                    onDismiss = { sheet = null },
                )

                null -> Unit
            }

            when (val d = dialog) {
                is DialogTarget.NewFolder -> NameDialog(
                    title = "Nueva carpeta",
                    initial = "",
                    onConfirm = { name ->
                        vm.createFolder(name, d.appKey)
                        dialog = null
                    },
                    onDismiss = { dialog = null },
                )

                DialogTarget.KidsMode -> KidsModeDialog(
                    kidsApp = kidsApp,
                    lock = kidsLock,
                    onPickApp = { sheet = SheetTarget.KidsAppPicker },
                    onStart = {
                        val app = kidsApp
                        if (app != null) {
                            vm.startKidsMode(kidsLock.silenceCalls())
                            dialog = null
                            screen = Screen.HOME
                            vm.repo.launch(app)
                        }
                    },
                    onDismiss = { dialog = null },
                )

                is DialogTarget.Rename -> NameDialog(
                    title = "Renombrar carpeta",
                    initial = d.folder.name,
                    onConfirm = { name ->
                        vm.renameFolder(d.folder.id, name)
                        dialog = null
                    },
                    onDismiss = { dialog = null },
                )

                null -> Unit
            }

            // Mientras dura el modo cuentos el launcher queda tapado: solo se puede volver a los cuentos.
            if (settings.kidsMode) {
                BackHandler { }
                KidsModeOverlay(
                    kidsApp = kidsApp,
                    onBack = { kidsApp?.let { vm.repo.launch(it) } },
                    onEnd = {
                        kidsLock.restore(settings.kidsPrevFilter)
                        vm.endKidsMode()
                    },
                )
            }
        }
    }
}
