package com.nexo.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nexo.launcher.KidsLock
import com.nexo.launcher.data.AppInfo
import kotlinx.coroutines.withTimeoutOrNull

/** Segundos que hay que mantener pulsado «Terminar» en la pantalla de bloqueo. */
private const val HOLD_TO_EXIT_MS = 3000L

/**
 * Prepara el modo cuentos: app elegida y «Fijar aplicaciones» activado.
 * El ajuste se vuelve a comprobar al volver de la pantalla del sistema.
 */
@Composable
fun KidsModeDialog(
    kidsApp: AppInfo?,
    lock: KidsLock,
    onPickApp: () -> Unit,
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val pinning = remember(refresh) { lock.pinningEnabled() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modo cuentos") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Step(
                    done = kidsApp != null,
                    title = "1. App de cuentos",
                    detail = kidsApp?.label ?: "Elige la app que podrá usar la niña.",
                    action = if (kidsApp == null) "Elegir" else "Cambiar",
                    onAction = onPickApp,
                )
                Step(
                    done = pinning == true,
                    title = "2. Fijar aplicaciones",
                    detail = when (pinning) {
                        true -> "Activado. Comprueba que también está «Pedir PIN para dejar de fijar»."
                        false -> "Actívalo en Ajustes → Seguridad → Fijar aplicaciones, junto con «Pedir PIN para dejar de fijar»."
                        null -> "Comprueba en Ajustes → Seguridad que «Fijar aplicaciones» y «Pedir PIN para dejar de fijar» están activados."
                    },
                    action = if (pinning == true) null else "Abrir Ajustes",
                    onAction = lock::openPinningSettings,
                )
                Spacer(Modifier.height(12.dp))
                Text("Al empezar", fontWeight = FontWeight.Bold)
                Text(
                    "Se abrirá la app de cuentos. Abre entonces las apps recientes (desliza desde abajo y " +
                        "mantén, o el botón cuadrado), toca el icono de la app de cuentos y elige «Fijar». " +
                        "Para salir: mantén pulsados Atrás y Recientes (o desliza hacia arriba y mantén) " +
                        "y escribe el PIN del móvil.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onStart, enabled = kidsApp != null) { Text("Empezar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun Step(done: Boolean, title: String, detail: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text((if (done) "✓  " else "") + title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        if (action != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidsAppPickerSheet(apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        SheetHeader("App de cuentos")
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(bottom = 24.dp)) {
            items(apps, key = { it.key }) { app -> SheetItem(app.label) { onPick(app) } }
        }
    }
}

/**
 * Tapa el launcher mientras el modo cuentos está activo: si la niña llega aquí (porque no se fijó
 * la app), solo puede volver a los cuentos. Terminar exige mantener pulsado varios segundos.
 */
@Composable
fun KidsModeOverlay(kidsApp: AppInfo?, onBack: () -> Unit, onEnd: () -> Unit) {
    var holding by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101014))
            // Se tragan todos los toques para que no lleguen a la pantalla de inicio.
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Modo cuentos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(48.dp))
        if (kidsApp != null) {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Text("Volver a ${kidsApp.label}", fontSize = 22.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (holding) "Sigue pulsando…" else "Terminar modo cuentos (mantén pulsado 3 s)",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        holding = true
                        val released = withTimeoutOrNull(HOLD_TO_EXIT_MS) { tryAwaitRelease() }
                        holding = false
                        if (released == null) onEnd()
                    })
                }
                .padding(16.dp),
        )
    }
}
