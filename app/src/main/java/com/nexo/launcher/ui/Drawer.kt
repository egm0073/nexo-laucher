package com.nexo.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexo.launcher.data.AlignChoice
import com.nexo.launcher.data.AppCategory
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.AppRepository
import com.nexo.launcher.data.DrawerRecent
import com.nexo.launcher.data.LauncherSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface DrawerRow {
    data class Header(val title: String) : DrawerRow
    data class Item(val app: AppInfo) : DrawerRow

    /** Despliega o pliega las apps de una categoría que no están entre las más recientes. */
    data class More(val category: AppCategory, val count: Int, val expanded: Boolean) : DrawerRow
}

/** Apps que se ven sin desplegar en cada categoría. */
private const val RECENT_PER_CATEGORY = 10

@Composable
fun AppDrawer(
    settings: LauncherSettings,
    apps: List<AppInfo>,
    repo: AppRepository,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    var query by rememberSaveable { mutableStateOf("") }

    val visible = remember(apps, settings.hidden) { apps.filter { it.key !in settings.hidden } }
    val q = query.trim().normalized()
    val filtered = remember(visible, q) {
        if (q.isEmpty()) visible
        else visible.filter { it.label.normalized().contains(q) }
            .sortedBy { if (it.label.normalized().startsWith(q)) 0 else 1 }
    }
    // El cajón se vuelve a crear al abrirlo, así que las categorías empiezan siempre plegadas.
    var expanded by remember { mutableStateOf(emptySet<String>()) }
    val rows: List<DrawerRow> = remember(filtered, q, settings.drawerByCategory, settings.drawerRecent, settings.categoryOverrides, settings.lastUsed, expanded) {
        when {
            q.isNotEmpty() -> filtered.map { DrawerRow.Item(it) }
            settings.drawerByCategory -> categoryRows(filtered, settings, expanded)
            else -> buildList<DrawerRow> {
                var last: Char? = null
                for (app in filtered) {
                    val s = sectionOf(app.label)
                    if (s != last) {
                        add(DrawerRow.Header(s.toString()))
                        last = s
                    }
                    add(DrawerRow.Item(app))
                }
            }
        }
    }
    val letterIndex: Map<Char, Int> = remember(rows, settings.drawerByCategory) {
        if (settings.drawerByCategory) emptyMap()
        else buildMap<Char, Int> {
            rows.forEachIndexed { i, r -> if (r is DrawerRow.Header && r.title[0] !in this) put(r.title[0], i) }
        }
    }

    val close: () -> Unit = {
        keyboard?.hide()
        onClose()
    }
    val launch: (AppInfo) -> Unit = {
        keyboard?.hide()
        onLaunch(it)
    }

    // Deslizar hacia abajo estando arriba del todo cierra el cajón.
    val closeLatest by rememberUpdatedState(close)
    val nested = remember {
        object : NestedScrollConnection {
            var acc = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    acc += available.y
                    if (acc > 220f) {
                        acc = 0f
                        closeLatest()
                    }
                } else {
                    acc = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        if (settings.drawerAutoKeyboard) {
            delay(200)
            runCatching { focus.requestFocus() }
        }
    }

    // El cajón es transparente sobre el fondo de pantalla: usa los mismos colores que la pantalla de inicio.
    val fg = settings.foreground()
    val itemStyle = TextStyle(
        color = fg,
        fontSize = (settings.textSizeSp * 0.75f).sp,
        fontFamily = settings.font.family(),
    )
    val accent = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = settings.dim)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus),
                    placeholder = { Text("Buscar apps") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Borrar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = fg.copy(alpha = 0.09f),
                        unfocusedContainerColor = fg.copy(alpha = 0.09f),
                        focusedTextColor = fg,
                        unfocusedTextColor = fg,
                        focusedPlaceholderColor = fg.copy(alpha = 0.6f),
                        unfocusedPlaceholderColor = fg.copy(alpha = 0.6f),
                        focusedLeadingIconColor = fg.copy(alpha = 0.8f),
                        unfocusedLeadingIconColor = fg.copy(alpha = 0.8f),
                        focusedTrailingIconColor = fg.copy(alpha = 0.8f),
                        unfocusedTrailingIconColor = fg.copy(alpha = 0.8f),
                        cursorColor = fg,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        val first = filtered.firstOrNull()
                        if (first != null) launch(first)
                        else if (query.isNotBlank()) {
                            webSearch(ctx, query)
                            close()
                        }
                    }),
                )
                IconButton(onClick = {
                    keyboard?.hide()
                    onOpenSettings()
                }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = fg)
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nested),
                    contentPadding = PaddingValues(start = 22.dp, end = 44.dp, top = 4.dp, bottom = 24.dp),
                ) {
                    itemsIndexed(
                        rows,
                        key = { i, r ->
                            when (r) {
                                is DrawerRow.Item -> r.app.key
                                is DrawerRow.More -> "more:${r.category.name}"
                                is DrawerRow.Header -> "h$i"
                            }
                        },
                    ) { _, row ->
                        when (row) {
                            is DrawerRow.Header -> Text(
                                row.title,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp),
                            )

                            is DrawerRow.Item -> AppRow(
                                app = row.app,
                                repo = repo,
                                iconStyle = settings.drawerIconStyle,
                                iconTint = settings.silhouetteTint(),
                                iconSize = settings.iconSizeDp.dp,
                                textStyle = itemStyle,
                                align = AlignChoice.START,
                                onClick = { launch(row.app) },
                                onLongClick = { onLongPress(row.app) },
                                modifier = Modifier.fillMaxWidth(),
                                verticalPadding = 10.dp,
                            )

                            is DrawerRow.More -> Text(
                                if (row.expanded) "Mostrar menos  ▴" else "${row.count} más  ▾",
                                color = fg.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val name = row.category.name
                                        expanded = if (row.expanded) expanded - name else expanded + name
                                    }
                                    .padding(horizontal = 6.dp, vertical = 12.dp),
                            )
                        }
                    }
                    if (q.isNotEmpty()) {
                        item(key = "web") {
                            Text(
                                "Buscar «${query.trim()}» en la web",
                                color = accent,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        webSearch(ctx, query.trim())
                                        close()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 16.dp),
                            )
                        }
                    }
                }

                if (q.isEmpty() && letterIndex.size > 1) {
                    FastScroller(
                        letters = letterIndex.keys.toList(),
                        accent = accent,
                        color = fg,
                        onLetter = { letter ->
                            letterIndex[letter]?.let { idx -> scope.launch { listState.scrollToItem(idx) } }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .fillMaxHeight(0.85f),
                    )
                }
            }
        }
    }
}

/**
 * Una cabecera por categoría con sus [RECENT_PER_CATEGORY] apps más recientes debajo; el resto,
 * por orden alfabético, queda plegado tras una fila «N más».
 */
private fun categoryRows(apps: List<AppInfo>, settings: LauncherSettings, expanded: Set<String>): List<DrawerRow> {
    val recency: (AppInfo) -> Long = when (settings.drawerRecent) {
        DrawerRecent.USED -> { app -> settings.lastUsed[app.key] ?: 0L }
        DrawerRecent.INSTALLED -> { app -> app.installedAt }
    }
    val byCategory = apps.groupBy { settings.categoryOverrides[it.key] ?: it.category }
    return buildList {
        for (category in AppCategory.entries) {
            val list = byCategory[category] ?: continue
            add(DrawerRow.Header(category.label))
            // Las apps sin abrir todavía se ordenan por fecha de instalación; la lista de entrada ya es alfabética.
            val sorted = list.sortedWith(compareByDescending(recency).thenByDescending { it.installedAt })
            val recent = sorted.take(RECENT_PER_CATEGORY)
            recent.forEach { add(DrawerRow.Item(it)) }
            val rest = list.filter { it !in recent }
            if (rest.isEmpty()) continue
            val open = category.name in expanded
            if (open) rest.forEach { add(DrawerRow.Item(it)) }
            add(DrawerRow.More(category, rest.size, open))
        }
    }
}

@Composable
private fun FastScroller(
    letters: List<Char>,
    accent: Color,
    color: Color,
    onLetter: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var active by remember { mutableStateOf<Char?>(null) }
    var heightPx by remember { mutableIntStateOf(1) }
    val onLetterLatest by rememberUpdatedState(onLetter)

    fun pick(y: Float) {
        if (letters.isEmpty()) return
        val i = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.lastIndex)
        val l = letters[i]
        if (l != active) {
            active = l
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterLatest(l)
        }
    }

    Column(
        modifier
            .width(28.dp)
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { pick(it.y) },
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        pick(change.position.y)
                    },
                )
            }
            .pointerInput(letters) {
                detectTapGestures(onTap = {
                    pick(it.y)
                    active = null
                })
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { l ->
            Text(
                l.toString(),
                color = if (l == active) accent else color.copy(alpha = 0.55f),
                fontSize = if (l == active) 15.sp else 11.sp,
                fontWeight = if (l == active) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
