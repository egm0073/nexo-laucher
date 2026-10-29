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
import com.nexo.launcher.data.AppInfo
import com.nexo.launcher.data.AppRepository
import com.nexo.launcher.data.LauncherSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface DrawerRow {
    data class Header(val letter: Char) : DrawerRow
    data class Item(val app: AppInfo) : DrawerRow
}

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
    val rows: List<DrawerRow> = remember(filtered, q) {
        if (q.isNotEmpty()) filtered.map { DrawerRow.Item(it) }
        else buildList<DrawerRow> {
            var last: Char? = null
            for (app in filtered) {
                val s = sectionOf(app.label)
                if (s != last) {
                    add(DrawerRow.Header(s))
                    last = s
                }
                add(DrawerRow.Item(app))
            }
        }
    }
    val letterIndex: Map<Char, Int> = remember(rows) {
        buildMap<Char, Int> {
            rows.forEachIndexed { i, r -> if (r is DrawerRow.Header && r.letter !in this) put(r.letter, i) }
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

    val itemStyle = TextStyle(
        color = Color.White,
        fontSize = (settings.textSizeSp * 0.75f).sp,
        fontFamily = settings.font.family(),
    )
    val accent = MaterialTheme.colorScheme.primary

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF00C0C10)),
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
                        focusedContainerColor = Color.White.copy(alpha = 0.09f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.09f),
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
                    Icon(Icons.Filled.Settings, contentDescription = "Ajustes", tint = Color.White)
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
                        key = { i, r -> if (r is DrawerRow.Item) r.app.key else "h$i" },
                    ) { _, row ->
                        when (row) {
                            is DrawerRow.Header -> Text(
                                row.letter.toString(),
                                color = accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp),
                            )

                            is DrawerRow.Item -> AppRow(
                                app = row.app,
                                repo = repo,
                                iconStyle = settings.drawerIconStyle,
                                iconSize = settings.iconSizeDp.dp,
                                textStyle = itemStyle,
                                align = AlignChoice.START,
                                onClick = { launch(row.app) },
                                onLongClick = { onLongPress(row.app) },
                                modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun FastScroller(
    letters: List<Char>,
    accent: Color,
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
                color = if (l == active) accent else Color.White.copy(alpha = 0.55f),
                fontSize = if (l == active) 15.sp else 11.sp,
                fontWeight = if (l == active) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
