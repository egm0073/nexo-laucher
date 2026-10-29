package com.nexo.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexo.launcher.data.AlignChoice
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.AppRepository
import com.nexo.launcher.data.IconStyle

@Composable
fun AppIcon(app: AppInfo, repo: AppRepository, style: IconStyle, size: Dp, tint: Color) {
    if (style == IconStyle.NONE) return
    val mono = style == IconStyle.SILHOUETTE
    val bitmap by produceState<ImageBitmap?>(initialValue = null, app.key, mono) {
        value = repo.icon(app.key, mono)
    }
    val filter = remember(style, tint) {
        when (style) {
            IconStyle.GRAYSCALE -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            IconStyle.SILHOUETTE -> ColorFilter.tint(tint, BlendMode.SrcIn)
            else -> null
        }
    }
    Box(Modifier.size(size)) {
        bitmap?.let {
            Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize(), colorFilter = filter)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppInfo,
    repo: AppRepository,
    iconStyle: IconStyle,
    iconSize: Dp,
    textStyle: TextStyle,
    align: AlignChoice,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = app.label,
) {
    val haptic = LocalHapticFeedback.current
    val tint = textStyle.color
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hasIcon = iconStyle != IconStyle.NONE
        if (align != AlignChoice.END) {
            AppIcon(app, repo, iconStyle, iconSize, tint)
            if (hasIcon) Spacer(Modifier.width(14.dp))
            Text(label, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            Text(label, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (hasIcon) Spacer(Modifier.width(14.dp))
            AppIcon(app, repo, iconStyle, iconSize, tint)
        }
    }
}

@Composable
fun SheetHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

@Composable
fun SheetItem(text: String, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    )
}

@Composable
fun NameDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nombre") },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
