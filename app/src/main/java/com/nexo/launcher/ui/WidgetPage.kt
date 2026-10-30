package com.nexo.launcher.ui

import android.appwidget.AppWidgetHost
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexo.launcher.data.LauncherSettings
import com.nexo.launcher.data.WidgetEntry
import kotlin.math.roundToInt

const val MIN_WIDGET_DP = 48f

/**
 * Segunda pantalla de inicio: widgets colocados libremente. En modo edición cada widget
 * se arrastra para moverlo y se redimensiona desde la esquina inferior derecha.
 */
@Composable
fun WidgetPage(
    settings: LauncherSettings,
    widgetHost: AppWidgetHost,
    editing: Boolean,
    onOpenDrawer: () -> Unit,
    onSwipeDown: () -> Unit,
    onLongPressBackground: () -> Unit,
    onPlace: (id: Int, x: Int, y: Int, widthDp: Int, heightDp: Int) -> Unit,
    onDelete: (Int) -> Unit,
    onStartEditing: () -> Unit,
    onDoneEditing: () -> Unit,
) {
    val widgets = settings.widgets.filter { it.page == 1 }

    Box(
        Modifier
            .fillMaxSize()
            .then(
                if (editing) Modifier
                else Modifier.homeGestures(
                    onSwipeUp = onOpenDrawer,
                    onSwipeDown = onSwipeDown,
                    swipeDownEnabled = settings.swipeDownNotifications,
                    onLongPress = onLongPressBackground,
                ),
            ),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (editing) 72.dp else 16.dp),
        ) {
            val areaW = maxWidth.value
            val areaH = maxHeight.value
            if (widgets.isEmpty()) {
                Text(
                    "Mantén pulsado el fondo para añadir un widget a esta pantalla. Después, mantén pulsado el widget para moverlo o cambiar su tamaño.",
                    style = settings.homeTextStyle(scale = 0.6f, alpha = 0.8f).copy(textAlign = TextAlign.Center),
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            widgets.forEach { entry ->
                key(entry.id) {
                    PlacedWidget(widgetHost, entry, editing, areaW, areaH, onPlace, onDelete, onStartEditing)
                }
            }
        }

        if (editing) {
            EditWidgetsBar("Arrastra para mover · tira de la esquina para cambiar el tamaño", onDoneEditing)
        }
    }
}

/** Barra inferior del modo de ajuste de widgets. */
@Composable
fun BoxScope.EditWidgetsBar(hint: String, onDone: () -> Unit) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(hint, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Button(onClick = onDone) { Text("Listo") }
    }
}

/** Esquina para cambiar el tamaño de un widget arrastrándola; dx y dy en dp. */
@Composable
fun BoxScope.ResizeHandle(key: Any, onDrag: (dx: Float, dy: Float) -> Unit, onDragEnd: () -> Unit) {
    Box(
        Modifier
            .align(Alignment.BottomEnd)
            .size(40.dp)
            .clip(RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .pointerInput(key) {
                detectDragGestures(onDragEnd = onDragEnd) { change, d ->
                    change.consume()
                    onDrag(d.x.toDp().value, d.y.toDp().value)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("⤡", color = Color.Black, fontSize = 20.sp)
    }
}

/** Botón para quitar un widget en el modo de ajuste. */
@Composable
fun BoxScope.DeleteWidgetButton(onClick: () -> Unit) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun PlacedWidget(
    host: AppWidgetHost,
    entry: WidgetEntry,
    editing: Boolean,
    areaW: Float,
    areaH: Float,
    onPlace: (Int, Int, Int, Int, Int) -> Unit,
    onDelete: (Int) -> Unit,
    onStartEditing: () -> Unit,
) {
    // Valores en dp; se guardan al soltar el dedo para no escribir en disco en cada movimiento.
    var w by remember(entry, areaW) {
        mutableFloatStateOf((if (entry.widthDp > 0) entry.widthDp.toFloat() else areaW).coerceIn(MIN_WIDGET_DP, areaW))
    }
    var h by remember(entry, areaH) { mutableFloatStateOf(entry.heightDp.toFloat().coerceIn(MIN_WIDGET_DP, areaH)) }
    var x by remember(entry, areaW) { mutableFloatStateOf(entry.x.toFloat().coerceIn(0f, areaW - w)) }
    var y by remember(entry, areaH) { mutableFloatStateOf(entry.y.toFloat().coerceIn(0f, areaH - h)) }
    val commit = { onPlace(entry.id, x.roundToInt(), y.roundToInt(), w.roundToInt(), h.roundToInt()) }
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(20.dp)

    Box(Modifier.offset(x.dp, y.dp).size(w.dp, h.dp)) {
        // Mantener pulsado el widget entra en el modo de mover y cambiar tamaño.
        HostedWidget(host, entry, Modifier.fillMaxSize(), onLongPress = if (editing) null else onStartEditing)
        if (editing) {
            // Capa encima del widget: recibe los toques para que el widget no los procese.
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(accent.copy(alpha = 0.15f))
                    .border(2.dp, accent, shape)
                    .pointerInput(entry, areaW, areaH) {
                        detectDragGestures(onDragEnd = commit) { change, d ->
                            change.consume()
                            x = (x + d.x.toDp().value).coerceIn(0f, areaW - w)
                            y = (y + d.y.toDp().value).coerceIn(0f, areaH - h)
                        }
                    },
            )
            ResizeHandle(
                key = Triple(entry, areaW, areaH),
                onDrag = { dx, dy ->
                    w = (w + dx).coerceIn(MIN_WIDGET_DP, areaW - x)
                    h = (h + dy).coerceIn(MIN_WIDGET_DP, areaH - y)
                },
                onDragEnd = commit,
            )
            DeleteWidgetButton { onDelete(entry.id) }
        }
    }
}
