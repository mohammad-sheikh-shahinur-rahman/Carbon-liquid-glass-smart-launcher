package com.isg32.carino.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.AppsView
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.common.AppTile
import com.isg32.carino.ui.components.VChip
import com.isg32.carino.ui.theme.LiquidGlassTheme
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import com.isg32.carino.ui.theme.liquidGlassPill
import com.isg32.carino.ui.theme.liquidGlassSurface
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class AppSectionGroup(
    val title: String,
    val apps: List<AppEntry>,
    val paused: Boolean = false,
)

private sealed interface AppsGridItem {
    data class Header(val title: String, val paused: Boolean = false) : AppsGridItem
    data class Tile(val app: AppEntry) : AppsGridItem
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
    val tokens = LiquidGlassTheme.tokens
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

    // Sectioned glass groups for List mode
    val listSections = remember(personal, work, workPaused, query, filteredApps) {
        buildList {
            if (query.isNotBlank()) {
                if (filteredApps.isNotEmpty()) {
                    add(AppSectionGroup(title = "Results", apps = filteredApps))
                }
            } else {
                personal.groupBy { it.section }.forEach { (sec, list) ->
                    add(AppSectionGroup(title = sec, apps = list))
                }
                if (work.isNotEmpty()) {
                    add(AppSectionGroup(title = "Work", apps = work, paused = workPaused))
                }
            }
        }
    }

    // Flattened items for Grid mode
    val gridItems = remember(listSections) {
        buildList {
            listSections.forEach { sec ->
                add(AppsGridItem.Header(sec.title, sec.paused))
                sec.apps.forEach { add(AppsGridItem.Tile(it)) }
            }
        }
    }

    val alphabet = remember { ('A'..'Z').map { it.toString() } + "#" }
    val listSectionIndexByLetter = remember(listSections) {
        buildMap {
            listSections.forEachIndexed { idx, sec ->
                val key = if (sec.title == "Work") "W" else sec.title
                putIfAbsent(key, idx)
            }
        }
    }
    val gridSectionIndexByLetter = remember(gridItems) {
        buildMap {
            gridItems.forEachIndexed { idx, item ->
                if (item is AppsGridItem.Header) {
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
        // Liquid Glass App Library Search Capsule + Frosted Glass Orbs / Cancel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .liquidGlassPill(shape = RoundedCornerShape(VRadius.pill))
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
                        strokeWidth = 2.0f,
                        color = ink.copy(alpha = 0.88f),
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
                                    style = VText.inter(16f, weight = 500).copy(color = ink.copy(alpha = 0.72f)),
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
                                .background(ink.copy(alpha = 0.20f))
                                .clickable { query = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            VIconView(VIcon.close, size = 13.dp, strokeWidth = 2.1f, color = ink)
                        }
                    }
                }
            }

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .liquidGlassPill(shape = RoundedCornerShape(VRadius.pill))
                        .clickable {
                            query = ""
                            focusManager.clearFocus()
                        }
                        .padding(horizontal = 14.dp)
                        .testTag("apps_search_cancel"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancel",
                        style = VText.inter(14f, weight = 600).copy(color = ink),
                    )
                }
            } else {
                LiquidOrbButton(
                    icon = if (settings.appsView == AppsView.list) VIcon.apps else VIcon.list,
                    contentDescription = "Switch view",
                    onClick = {
                        vm.updateSettings { s ->
                            s.copy(appsView = if (s.appsView == AppsView.list) AppsView.grid else AppsView.list)
                        }
                    },
                    tag = "apps_toggle_view",
                )
                LiquidOrbButton(
                    icon = VIcon.gear,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
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
                        start = 16.dp,
                        end = 42.dp,
                        top = 4.dp,
                        bottom = VSpace.tabBarClearance + 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(listSections, key = { "sec_${it.title}" }) { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionPillHeader(title = section.title, paused = section.paused)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlassSurface(
                                        shape = RoundedCornerShape(26.dp),
                                        elevated = false,
                                        borderWidth = 1.dp,
                                        showSpecularSheen = true,
                                    ),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    section.apps.forEachIndexed { idx, app ->
                                        AppListRow(
                                            app = app,
                                            vm = vm,
                                            showNew = settings.newBadges && app.isNew(),
                                            showDivider = idx < section.apps.lastIndex,
                                            iconStyle = layout.icons,
                                            onLongPress = { onAppMenu(app) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 42.dp,
                        top = 4.dp,
                        bottom = VSpace.tabBarClearance + 8.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(
                        items = gridItems,
                        span = { item ->
                            when (item) {
                                is AppsGridItem.Header -> GridItemSpan(maxLineSpan)
                                is AppsGridItem.Tile -> GridItemSpan(1)
                            }
                        },
                        key = { item ->
                            when (item) {
                                is AppsGridItem.Header -> "gh_${item.title}"
                                is AppsGridItem.Tile -> item.app.key
                            }
                        },
                    ) { item ->
                        when (item) {
                            is AppsGridItem.Header -> SectionPillHeader(item.title, item.paused)
                            is AppsGridItem.Tile -> Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (item.app.paused) 0.45f else 1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppTile(
                                    app = item.app,
                                    vm = vm,
                                    size = 54.dp,
                                    style = layout.icons,
                                    blur = true,
                                    onLongPress = { onAppMenu(item.app) },
                                )
                            }
                        }
                    }
                }
            }

            // Right-edge vertical Liquid Glass A-Z Scrubber Rail
            val activeIndexMap = if (settings.appsView == AppsView.list) {
                listSectionIndexByLetter
            } else {
                gridSectionIndexByLetter
            }
            if (query.isEmpty() && activeIndexMap.isNotEmpty()) {
                LiquidLetterScrubber(
                    letters = alphabet,
                    availableLetters = activeIndexMap.keys,
                    activeLetter = activeLetter,
                    onLetter = { letter ->
                        if (letter != activeLetter) {
                            activeLetter = letter
                            if (settings.scrubberHaptics) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            val targetIdx = activeIndexMap[letter]
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
                        .padding(end = 7.dp, top = 4.dp, bottom = VSpace.tabBarClearance - 20.dp),
                )
            }

            // Floating Liquid Glass Loupe Sphere when scrubbing
            androidx.compose.animation.AnimatedVisibility(
                visible = activeLetter != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .liquidGlassPill(shape = CircleShape, smoked = true),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = activeLetter.orEmpty(),
                        style = VText.doto(34f, weight = 700).copy(color = ink),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidOrbButton(
    icon: VIcon,
    contentDescription: String,
    onClick: () -> Unit,
    tag: String,
) {
    val ink = LocalVInk.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .testTag(tag)
            .liquidGlassPill(shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        VIconView(
            icon = icon,
            size = 18.dp,
            strokeWidth = 1.8f,
            color = ink,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun SectionPillHeader(title: String, paused: Boolean) {
    val ink = LocalVInk.current
    val tokens = LiquidGlassTheme.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(VRadius.pill))
                .background(tokens.surfaceRegular)
                .border(0.9.dp, tokens.borderBrush, RoundedCornerShape(VRadius.pill))
                .padding(horizontal = 10.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.uppercase(),
                style = VText.inter(11f, weight = 700, letterSpacing = 0.8f).copy(color = ink.copy(alpha = 0.90f)),
            )
        }
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
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppGlyph(app = app, vm = vm, size = 44.dp, style = iconStyle, blur = true)
            Spacer(Modifier.width(14.dp))
            Text(
                text = app.label,
                style = VText.inter(15.5f, weight = 500).copy(color = ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showNew) {
                VChip(label = "New")
                Spacer(Modifier.width(6.dp))
            }
            VIconView(
                icon = VIcon.chevron,
                size = 15.dp,
                strokeWidth = 1.6f,
                color = ink.copy(alpha = 0.30f),
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp, end = 16.dp)
                    .height(0.6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ink.copy(alpha = 0.22f),
                                ink.copy(alpha = 0.08f),
                            ),
                        ),
                    ),
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
    val tokens = LiquidGlassTheme.tokens
    val railShape = RoundedCornerShape(VRadius.pill)

    Column(
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight(0.86f)
            .clip(railShape)
            .background(if (tokens.isDark) Color(0x2E161224) else Color(0x52FFFFFF))
            .border(0.85.dp, tokens.borderBrush, railShape)
            .padding(vertical = 6.dp)
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
                    .size(if (isSelected) 20.dp else 16.dp)
                    .let {
                        if (isSelected) {
                            it.clip(CircleShape)
                                .background(tokens.lensHighlight)
                                .border(1.dp, tokens.borderBrush, CircleShape)
                        } else {
                            it
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = VText.inter(
                        size = if (isSelected) 10.5f else 9f,
                        weight = if (isSelected || hasApps) 700 else 400,
                    ).copy(
                        color = when {
                            isSelected -> ink
                            hasApps -> ink.copy(alpha = 0.88f)
                            else -> ink.copy(alpha = 0.34f)
                        },
                    ),
                )
            }
        }
    }
}
