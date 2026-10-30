package com.nexo.launcher.ui

import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.nexo.launcher.LauncherViewModel
import com.nexo.launcher.data.AlignChoice
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.Folder
import com.nexo.launcher.data.FontChoice
import com.nexo.launcher.data.SilhouetteColor
import com.nexo.launcher.data.IconStyle
import com.nexo.launcher.data.LauncherSettings
import com.nexo.launcher.data.WidgetEntry
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    apps: List<AppInfo>,
    vm: LauncherViewModel,
    actions: LauncherActions,
    onBack: () -> Unit,
    onAddWidget: () -> Unit,
    onNewFolder: () -> Unit,
    onRenameFolder: (Folder) -> Unit,
) {
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val hiddenApps = remember(byKey, settings.hidden) { settings.hidden.mapNotNull { byKey[it] }.sortedBy { it.label.lowercase() } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                Text("Ajustes", style = MaterialTheme.typography.titleLarge)
            }

            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                // ---------- Apariencia ----------
                item { SectionTitle("Apariencia") }
                item {
                    ChoiceRow("Color del texto", listOf("Claro" to true, "Oscuro" to false), settings.lightText) { v ->
                        vm.update { it.copy(lightText = v) }
                    }
                }
                item { AccentRow(settings.accent) { c -> vm.update { it.copy(accent = c) } } }
                item {
                    ChoiceRow("Fuente", FontChoice.entries.map { it.label to it }, settings.font, fontPreview = { it.family() }) { v ->
                        vm.update { it.copy(font = v) }
                    }
                }
                item {
                    SliderRow("Tamaño del texto", settings.textSizeSp.toFloat(), 16f..40f, steps = 23, format = { "${it.roundToInt()} sp" }) { v ->
                        vm.update { it.copy(textSizeSp = v.roundToInt()) }
                    }
                }
                item {
                    ChoiceRow("Alineación", AlignChoice.entries.map { it.label to it }, settings.align) { v ->
                        vm.update { it.copy(align = v) }
                    }
                }
                item {
                    SliderRow("Oscurecer fondo", settings.dim, 0f..0.85f, steps = 0, format = { "${(it * 100).roundToInt()} %" }) { v ->
                        vm.update { it.copy(dim = v) }
                    }
                }

                // ---------- Iconos ----------
                item { SectionTitle("Iconos") }
                item {
                    ChoiceRow("En la pantalla principal", IconStyle.entries.map { it.label to it }, settings.homeIconStyle) { v ->
                        vm.update { it.copy(homeIconStyle = v) }
                    }
                }
                item {
                    ChoiceRow("En el cajón de apps", IconStyle.entries.map { it.label to it }, settings.drawerIconStyle) { v ->
                        vm.update { it.copy(drawerIconStyle = v) }
                    }
                }
                item {
                    SliderRow("Tamaño de iconos", settings.iconSizeDp.toFloat(), 18f..56f, steps = 18, format = { "${it.roundToInt()} dp" }) { v ->
                        vm.update { it.copy(iconSizeDp = v.roundToInt()) }
                    }
                }
                item {
                    ChoiceRow("Color de la silueta", SilhouetteColor.entries.map { it.label to it }, settings.silhouetteColor) { v ->
                        vm.update { it.copy(silhouetteColor = v) }
                    }
                }
                item { HintText("«Grises» quita el color al icono; «Silueta» usa la forma monocroma del icono con el color elegido. El cajón de apps es transparente y usa los mismos colores que la pantalla principal.") }

                // ---------- Pantalla principal ----------
                item { SectionTitle("Pantalla principal") }
                item { SwitchRow("Reloj", settings.showClock) { v -> vm.update { it.copy(showClock = v) } } }
                item { SwitchRow("Formato 24 horas", settings.clock24) { v -> vm.update { it.copy(clock24 = v) } } }
                item { SwitchRow("Fecha", settings.showDate) { v -> vm.update { it.copy(showDate = v) } } }
                item { SwitchRow("Batería", settings.showBattery) { v -> vm.update { it.copy(showBattery = v) } } }
                item { SwitchRow("Próxima alarma", settings.showNextAlarm) { v -> vm.update { it.copy(showNextAlarm = v) } } }

                // ---------- Widgets ----------
                item { SectionTitle("Widgets") }
                if (settings.widgets.isEmpty()) item { HintText("No hay widgets. Añade cualquiera de los widgets de tus apps. Desliza a la izquierda en inicio para la pantalla de widgets.") }
                items(settings.widgets, key = { "w${it.id}" }) { w ->
                    WidgetSettingRow(
                        entry = w,
                        onUp = { vm.moveWidget(w.id, -1) },
                        onDown = { vm.moveWidget(w.id, 1) },
                        onSmaller = { vm.resizeWidget(w.id, -20) },
                        onBigger = { vm.resizeWidget(w.id, 20) },
                        onDelete = { actions.deleteWidget(w.id) },
                    )
                }
                item { ActionRow("+  Añadir widget", onAddWidget) }

                // ---------- Carpetas ----------
                item { SectionTitle("Carpetas") }
                if (settings.folders.isEmpty()) item { HintText("Las carpetas aparecen en la pantalla principal y se despliegan al tocarlas.") }
                items(settings.folders, key = { "f${it.id}" }) { f ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📁  ${f.name}  (${f.apps.size})", modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRenameFolder(f) }) { Text("Renombrar") }
                        TextButton(onClick = { vm.deleteFolder(f.id) }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    }
                }
                item { ActionRow("+  Nueva carpeta", onNewFolder) }

                // ---------- Apps ocultas ----------
                item { SectionTitle("Apps ocultas") }
                if (hiddenApps.isEmpty()) item { HintText("Mantén pulsada una app y elige «Ocultar app».") }
                items(hiddenApps, key = { "h${it.key}" }) { app ->
                    Row(
                        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(app.label, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.setHidden(app.key, false) }) { Text("Mostrar") }
                    }
                }

                // ---------- Cajón y gestos ----------
                item { SectionTitle("Cajón y gestos") }
                item { SwitchRow("Abrir el teclado al abrir el cajón", settings.drawerAutoKeyboard) { v -> vm.update { it.copy(drawerAutoKeyboard = v) } } }
                item { SwitchRow("Deslizar abajo abre notificaciones", settings.swipeDownNotifications) { v -> vm.update { it.copy(swipeDownNotifications = v) } } }
                item { HintText("Deslizar arriba: todas las apps · Mantener pulsado el fondo: menú · Tocar la hora: alarmas · Tocar la fecha: calendario") }

                // ---------- Sistema ----------
                item { SectionTitle("Sistema") }
                item { ActionRow("Elegir launcher predeterminado") { actions.openDefaultLauncherSettings() } }
                item { ActionRow("Cambiar fondo de pantalla") { actions.openWallpaperPicker() } }
                item { HintText("Nexo Launcher 1.0") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp),
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    var v by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(format(v), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
        }
        Slider(
            value = v,
            onValueChange = { v = it },
            valueRange = range,
            steps = steps,
            onValueChangeFinished = { onCommit(v) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    fontPreview: ((T) -> FontFamily)? = null,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (label, value) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label, fontFamily = fontPreview?.invoke(value)) },
                )
            }
        }
    }
}

@Composable
private fun AccentRow(selected: Long, onSelect: (Long) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Color de acento", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccentPalette.forEach { c ->
                val isSel = c == selected
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(if (isSel) 3.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onSelect(c) },
                )
            }
        }
    }
}

@Composable
private fun WidgetSettingRow(
    entry: WidgetEntry,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onSmaller: () -> Unit,
    onBigger: () -> Unit,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    val label = remember(entry.id) {
        AppWidgetManager.getInstance(ctx).getAppWidgetInfo(entry.id)?.loadLabel(ctx.packageManager) ?: "Widget no disponible"
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        val where = if (entry.page == 1) "  ·  pantalla 2" else ""
        Text("$label  ·  ${entry.heightDp} dp$where", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (entry.page == 0) {
                TextButton(onClick = onUp) { Text("↑ Subir") }
                TextButton(onClick = onDown) { Text("↓ Bajar") }
            }
            TextButton(onClick = onSmaller) { Text("−") }
            TextButton(onClick = onBigger) { Text("+") }
            TextButton(onClick = onDelete) { Text("Quitar", color = MaterialTheme.colorScheme.error) }
        }
    }
}
