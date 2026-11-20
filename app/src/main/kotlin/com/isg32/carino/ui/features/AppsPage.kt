package com.isg32.carino.ui.features

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.AppsView
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.common.AppTile
import com.isg32.carino.ui.components.VChip
import com.isg32.carino.ui.components.VIconButton
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed interface AppsListItem {
    data class Header(val title: String, val paused: Boolean = false) : AppsListItem
    data class RowApp(val app: AppEntry, val showDivider: Boolean) : AppsListItem
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsPage(
    vm: CarinoViewModel,
    onAppMenu: (AppEntry) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ink = LocalVInk.current
    val pal = LocalVPalette.current
    val apps by vm.visibleApps.collectAsState()
    val settings by vm.settings.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }

    val filteredApps = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
    }

    val personal = remember(filteredApps) { filteredApps.filter { !it.work } }
    val work = remember(filteredApps) { filteredApps.filter { it.work } }
    val workPaused = remember(work) { work.any { it.paused } }

    val items = remember(personal, work, workPaused, query) {
        buildList {
            if (query.isNotBlank()) {
                filteredApps.forEachIndexed { idx, app ->
                    add(AppsListItem.RowApp(app, showDivider = idx < filteredApps.lastIndex))
                }
            } else {
                var currentSection: String? = null
                personal.forEachIndexed { idx, app ->
                    val sec = app.section
                    if (sec != currentSection) {
                        currentSection = sec
                        add(AppsListItem.Header(sec))
                    }
                    val nextSameSec = personal.getOrNull(idx + 1)?.section == sec
                    add(AppsListItem.RowApp(app, showDivider = nextSameSec))
                }
                if (work.isNotEmpty()) {
                    add(AppsListItem.Header("Work", paused = workPaused))
                    work.forEachIndexed { idx, app ->
                        add(AppsListItem.RowApp(app, showDivider = idx < work.lastIndex))
                    }
                }
            }
        }
    }

    val alphabet = remember { ('A'..'Z').map { it.toString() } + "#" }
    val sectionIndexByLetter = remember(items) {
        buildMap {
            items.forEachIndexed { idx, item ->
                if (item is AppsListItem.Header) {
                    val key = if (item.title == "Work") "W" else item.title
                    putIfAbsent(key, idx)
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var activeLetter by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // Liquid Glass App Library Search Capsule + Cancel / Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .clip(RoundedCornerShape(VRadius.pill))
                    .background(pal.liquidSearchBrush)
                    .border(1.3.dp, pal.liquidBorderBrush, RoundedCornerShape(VRadius.pill))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VIconView(
                        icon = VIcon.search,
                        size = 18.dp,
                        strokeWidth = 1.9f,
                        color = ink.copy(alpha = 0.85f),
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = VText.inter(16f, weight = 500).copy(color = ink),
                        cursorBrush = SolidColor(ink),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apps_library_search_input"),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "App Library",
                                    style = VText.inter(16f, weight = 500).copy(color = ink.copy(alpha = 0.68f)),
                                )
                            }
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(ink.copy(alpha = 0.16f))
                                .clickable { query = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            VIconView(VIcon.close, size = 13.dp, strokeWidth = 2.0f, color = ink)
                        }
                    }
                }
            }

            if (query.isNotEmpty()) {
                Text(
                    text = "Cancel",
                    style = VText.inter(15f, weight = 600).copy(color = ink),
                    modifier = Modifier
                        .clip(RoundedCornerShape(VRadius.pill))
                        .clickable {
                            query = ""
                            focusManager.clearFocus()
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag("apps_search_cancel"),
                )
            } else {
                VIconButton(
                    icon = if (settings.appsView == AppsView.list) VIcon.apps else VIcon.list,
                    contentDescription = "Switch view",
                    onClick = {
                        vm.updateSettings { s ->
                            s.copy(appsView = if (s.appsView == AppsView.list) AppsView.grid else AppsView.list)
                        }
                    },
                    outlined = true,
                    size = 44.dp,
                    iconSize = 18.dp,
                    tag = "apps_toggle_view",
                )
                VIconButton(
                    icon = VIcon.gear,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    outlined = true,
                    size = 44.dp,
                    iconSize = 18.dp,
                    tag = "apps_settings_button",
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (settings.appsView == AppsView.list || query.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 40.dp,
                        top = 4.dp,
                        bottom = VSpace.tabBarClearance,
                    ),
                ) {
                    items(items, key = { item ->
                        when (item) {
                            is AppsListItem.Header -> "h_${item.title}"
                            is AppsListItem.RowApp -> item.app.key
                        }
                    }) { item ->
                        when (item) {
                            is AppsListItem.Header -> SectionRow(item.title, item.paused)
                            is AppsListItem.RowApp -> AppListRow(
                                app = item.app,
                                vm = vm,
                                showNew = settings.newBadges && item.app.isNew(),
                                showDivider = item.showDivider,
                                iconStyle = layout.icons,
                                onLongPress = { onAppMenu(item.app) },
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 40.dp,
                        top = 4.dp,
                        bottom = VSpace.tabBarClearance,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(
                        items = items,
                        span = { item ->
                            when (item) {
                                is AppsListItem.Header -> GridItemSpan(maxLineSpan)
                                is AppsListItem.RowApp -> GridItemSpan(1)
                            }
                        },
                        key = { item ->
                            when (item) {
                                is AppsListItem.Header -> "gh_${item.title}"
                                is AppsListItem.RowApp -> item.app.key
                            }
                        },
                    ) { item ->
                        when (item) {
                            is AppsListItem.Header -> SectionRow(item.title, item.paused)
                            is AppsListItem.RowApp -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (item.app.paused) 0.45f else 1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppTile(
                                    app = item.app,
                                    vm = vm,
                                    size = 52.dp,
                                    style = layout.icons,
                                    onLongPress = { onAppMenu(item.app) },
                                )
                            }
                        }
                    }
                }
            }

            // Liquid Glass A-Z Scrubber with circular frosted lens highlight
            if (query.isEmpty() && sectionIndexByLetter.isNotEmpty()) {
                LiquidLetterScrubber(
                    letters = alphabet,
                    availableLetters = sectionIndexByLetter.keys,
                    activeLetter = activeLetter,
                    onLetter = { letter ->
                        if (letter != activeLetter) {
                            activeLetter = letter
                            if (settings.scrubberHaptics) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            val targetIdx = sectionIndexByLetter[letter]
                            if (targetIdx != null) {
                                scope.launch {
                                    if (settings.appsView == AppsView.list) {
                                        listState.scrollToItem(targetIdx)
                                    } else {
                                        gridState.scrollToItem(targetIdx)
                                    }
                                }
                            }
                        }
                    },
                    onRelease = { activeLetter = null },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp, bottom = VSpace.tabBarClearance / 2),
                )
            }

            if (activeLetter != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(pal.liquidSearchBrush)
                        .border(1.3.dp, pal.liquidBorderBrush, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activeLetter!!,
                        style = VText.doto(32f, weight = 700).copy(color = ink),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRow(title: String, paused: Boolean) {
    val ink = LocalVInk.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = VText.eyebrow.copy(color = ink.copy(alpha = 0.62f)),
        )
        if (paused) {
            VChip(label = "Paused")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListRow(
    app: AppEntry,
    vm: CarinoViewModel,
    showNew: Boolean,
    showDivider: Boolean,
    iconStyle: com.isg32.carino.data.IconStyle,
    onLongPress: () -> Unit,
) {
    val ink = LocalVInk.current
    val haptic = LocalHapticFeedback.current
    var boundsRect: android.graphics.Rect? = null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (app.paused) 0.45f else 1f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    boundsRect = android.graphics.Rect(
                        b.left.roundToInt(),
                        b.top.roundToInt(),
                        b.right.roundToInt(),
                        b.bottom.roundToInt(),
                    )
                }
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.launchApp(app.key, boundsRect)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                )
                .padding(vertical = 10.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppGlyph(app = app, vm = vm, size = 46.dp, style = iconStyle)
            Spacer(Modifier.width(16.dp))
            Text(
                text = app.label,
                style = VText.inter(16f, weight = 500).copy(color = ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showNew) {
                VChip(label = "New")
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 62.dp)
                    .height(0.6.dp)
                    .background(ink.copy(alpha = 0.16f)),
            )
        }
    }
}

@Composable
private fun LiquidLetterScrubber(
    letters: List<String>,
    availableLetters: Set<String>,
    activeLetter: String?,
    onLetter: (String) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalVInk.current
    val pal = LocalVPalette.current

    Column(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight(0.84f)
            .pointerInput(letters) {
                detectTapGestures { offset ->
                    val i = ((offset.y / size.height.coerceAtLeast(1)) * letters.size)
                        .toInt()
                        .coerceIn(0, letters.lastIndex)
                    onLetter(letters[i])
                    onRelease()
                }
            }
            .pointerInput(letters) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val i = ((offset.y / size.height.coerceAtLeast(1)) * letters.size)
                            .toInt()
                            .coerceIn(0, letters.lastIndex)
                        onLetter(letters[i])
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val i = ((change.position.y / size.height.coerceAtLeast(1)) * letters.size)
                            .toInt()
                            .coerceIn(0, letters.lastIndex)
                        onLetter(letters[i])
                    },
                    onDragEnd = onRelease,
                    onDragCancel = onRelease,
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val isSelected = letter == activeLetter
            val hasApps = letter in availableLetters
            Box(
                modifier = Modifier
                    .size(if (isSelected) 24.dp else 18.dp)
                    .let {
                        if (isSelected) {
                            it.clip(CircleShape)
                                .background(pal.liquidSearchBrush)
                                .border(1.dp, pal.liquidBorderBrush, CircleShape)
                        } else {
                            it
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = VText.inter(
                        size = if (isSelected) 11f else 9.5f,
                        weight = if (isSelected || hasApps) 700 else 400,
                    ).copy(
                        color = when {
                            isSelected -> ink
                            hasApps -> ink.copy(alpha = 0.85f)
                            else -> ink.copy(alpha = 0.35f)
                        },
                    ),
                )
            }
        }
    }
}
