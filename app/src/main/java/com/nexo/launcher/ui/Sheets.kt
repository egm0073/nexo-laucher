package com.nexo.launcher.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.nexo.launcher.LauncherViewModel
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.Folder
import com.nexo.launcher.data.HomeItem
import com.nexo.launcher.data.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: AppInfo,
    folderId: String?,
    settings: LauncherSettings,
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit,
) {
    var pickFolder by remember { mutableStateOf(false) }
    val homeKey = HomeItem.app(app.key)
    val onHome = homeKey in settings.homeItems
    val hidden = app.key in settings.hidden

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SheetHeader(app.label + if (app.isWork) " (trabajo)" else "")
            if (!pickFolder) {
                SheetItem(if (onHome) "Quitar de inicio" else "Añadir a inicio") {
                    vm.toggleHome(homeKey); onDismiss()
                }
                if (onHome) {
                    SheetItem("Mover arriba") { vm.moveHome(homeKey, -1) }
                    SheetItem("Mover abajo") { vm.moveHome(homeKey, 1) }
                }
                if (folderId != null) {
                    SheetItem("Quitar de la carpeta") { vm.removeFromFolder(folderId, app.key); onDismiss() }
                }
                SheetItem("Añadir a carpeta…") { pickFolder = true }
                SheetItem(if (hidden) "Mostrar en el cajón" else "Ocultar app") {
                    vm.setHidden(app.key, !hidden); onDismiss()
                }
                SheetItem("Información de la app") { vm.repo.openAppInfo(app); onDismiss() }
                SheetItem("Desinstalar", color = MaterialTheme.colorScheme.error) { vm.repo.uninstall(app); onDismiss() }
            } else {
                settings.folders.forEach { f ->
                    SheetItem("📁  " + f.name) { vm.addToFolder(f.id, app.key); onDismiss() }
                }
                SheetItem("+  Nueva carpeta", color = MaterialTheme.colorScheme.primary) { onNewFolder() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderActionsSheet(
    folder: Folder,
    vm: LauncherViewModel,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
) {
    val item = HomeItem.folder(folder.id)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SheetHeader("📁  " + folder.name)
            SheetItem("Renombrar") { onRename() }
            SheetItem("Mover arriba") { vm.moveHome(item, -1) }
            SheetItem("Mover abajo") { vm.moveHome(item, 1) }
            SheetItem("Eliminar carpeta (las apps no se borran)", color = MaterialTheme.colorScheme.error) {
                vm.deleteFolder(folder.id); onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMenuSheet(
    onDismiss: () -> Unit,
    onAddWidget: () -> Unit,
    onNewFolder: () -> Unit,
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SheetHeader("Pantalla principal")
            SheetItem("Añadir widget") { onAddWidget() }
            SheetItem("Nueva carpeta") { onNewFolder() }
            SheetItem("Cambiar fondo de pantalla") { onWallpaper() }
            SheetItem("Ajustes del launcher") { onSettings() }
        }
    }
}

private data class ProviderGroup(val appLabel: String, val providers: List<Pair<AppWidgetProviderInfo, String>>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(onPick: (AppWidgetProviderInfo) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val groups by produceState<List<ProviderGroup>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val collator = Collator.getInstance()
            AppWidgetManager.getInstance(ctx).installedProviders
                .map { it to it.loadLabel(pm).orEmpty() }
                .groupBy { it.first.provider.packageName }
                .map { (pkg, list) ->
                    ProviderGroup(appLabel(pm, pkg), list.sortedWith(compareBy(collator) { it.second }))
                }
                .sortedWith(compareBy(collator) { it.appLabel })
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetHeader("Añadir widget")
        val list = groups
        if (list == null) {
            Text("Cargando…", modifier = Modifier.padding(24.dp))
        } else {
            LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                list.forEach { group ->
                    item(key = "g_" + group.appLabel + group.providers.first().first.provider.packageName) {
                        Text(
                            group.appLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(group.providers, key = { it.first.provider.flattenToString() }) { (info, label) ->
                        WidgetProviderRow(info, label) { onPick(info) }
                    }
                }
            }
        }
    }
}

private fun appLabel(pm: PackageManager, pkg: String): String = runCatching {
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrDefault(pkg)

@Composable
private fun WidgetProviderRow(info: AppWidgetProviderInfo, label: String, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val preview by produceState<ImageBitmap?>(initialValue = null, info.provider) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val d = info.loadPreviewImage(ctx, 0) ?: info.loadIcon(ctx, 0)
                val w = d.intrinsicWidth.takeIf { it > 0 } ?: 200
                val h = d.intrinsicHeight.takeIf { it > 0 } ?: 200
                val scale = minOf(1f, 360f / maxOf(w, h))
                d.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
            }.getOrNull()
        }
    }
    val density = ctx.resources.displayMetrics.density
    val sizeText = "${(info.minWidth / density).toInt()} × ${(info.minHeight / density).toInt()} dp"

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            preview?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.heightIn(max = 72.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(sizeText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}
