package com.nexo.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexo.launcher.LauncherViewModel
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.Folder
import kotlinx.coroutines.flow.StateFlow

interface LauncherActions {
    fun addWidget(info: AppWidgetProviderInfo)
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
    data object WidgetPicker : SheetTarget
}

private sealed interface DialogTarget {
    data class NewFolder(val appKey: String?) : DialogTarget
    data class Rename(val folder: Folder) : DialogTarget
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

    // Pulsar "Inicio" vuelve siempre a la pantalla principal.
    LaunchedEffect(signal) {
        if (signal > 0) {
            screen = Screen.HOME
            sheet = null
            dialog = null
        }
    }

    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    val launch: (AppInfo) -> Unit = { app ->
        vm.repo.launch(app)
        screen = Screen.HOME
    }

    LauncherTheme(accent = Color(settings.accent)) {
        Box(Modifier.fillMaxSize()) {
            if (settings.loaded) {
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
                )
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
                    onAddWidget = { sheet = SheetTarget.WidgetPicker },
                    onNewFolder = { dialog = DialogTarget.NewFolder(null) },
                    onRenameFolder = { dialog = DialogTarget.Rename(it) },
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
                onAddWidget = { sheet = SheetTarget.WidgetPicker },
                onNewFolder = {
                    sheet = null
                    dialog = DialogTarget.NewFolder(null)
                },
                onWallpaper = {
                    sheet = null
                    actions.openWallpaperPicker()
                },
                onSettings = {
                    sheet = null
                    screen = Screen.SETTINGS
                },
            )

            SheetTarget.WidgetPicker -> WidgetPickerSheet(
                onPick = {
                    sheet = null
                    actions.addWidget(it)
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
    }
}
