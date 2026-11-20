package com.isg32.carino.ui.features

import android.graphics.Bitmap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.AppsView
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.IconStyle
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.components.CarinoSheet
import com.isg32.carino.ui.components.VActionRow
import com.isg32.carino.ui.components.VChip
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VListCard
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

private val CategoryOrder = listOf("All", "Communication", "Entertainment", "Tools", "Finance", "Other")

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
    val appIndex by vm.appIndex.collectAsState()
    val settings by vm.settings.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    val dots by vm.dots.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showOverflowSheet by remember { mutableStateOf(false) }

    // Available categories dynamically populated from installed apps
    val categories = remember(apps) {
        val present = apps.map { it.category }.toSet()
        listOf("All") + CategoryOrder.drop(1).filter { it in present }
    }
    if (selectedCategory !in categories) {
        selectedCategory = "All"
    }

    // Top 4 suggested / favourite apps shown above the divider on "All"
    val suggestedApps = remember(layout.favourites, layout.dock, appIndex, apps) {
        val pinned = (layout.favGrid + layout.dockRow)
            .mapNotNull { key -> key?.let { appIndex[it] } }
            .distinctBy { it.key }
        (pinned + apps).distinctBy { it.key }.take(4)
    }

    val filteredApps = remember(apps, query, selectedCategory) {
        val byCategory = if (selectedCategory == "All") {
            apps
        } else {
            apps.filter { it.category == selectedCategory }
        }
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            byCategory
        } else {
            byCategory.filter { it.label.lowercase().contains(q) }
                .sortedWith(
                    compareByDescending<AppEntry> { it.label.lowercase().startsWith(q) }
                        .thenBy { it.label.lowercase() },
                )
        }
    }

    val personal = remember(filteredApps) { filteredApps.filter { !it.work } }
    val work = remember(filteredApps) { filteredApps.filter { it.work } }
    val workPaused = remember(work) { work.any { it.paused } }

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

    val showSuggestedRow = query.isEmpty() && selectedCategory == "All" && suggestedApps.isNotEmpty()
    // Offset in LazyVerticalGrid items before alphabetical apps begin
    val gridTopOffset = if (showSuggestedRow) 2 else 0 // 1 for suggested row + 1 for divider

    val gridIndexByLetter = remember(filteredApps, gridTopOffset) {
        buildMap {
            put("♡", 0)
            filteredApps.forEachIndexed { idx, app ->
                val sec = app.section
                val key = if (sec == "#") "…" else sec
                putIfAbsent(key, idx + gridTopOffset)
            }
        }
    }
    val listIndexByLetter = remember(listSections) {
        buildMap {
            put("♡", 0)
            listSections.forEachIndexed { idx, sec ->
                val key = when (sec.title) {
                    "Work" -> "W"
                    "#" -> "…"
                    else -> sec.title
                }
                putIfAbsent(key, idx)
            }
        }
    }

    // Compact scrubber symbols matching screenshot: ♡ + A..Z + …
    val scrubberSymbols = remember {
        listOf("♡") + ('A'..'Z').map { it.toString() } + listOf("…")
    }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var activeSymbol by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // 1. Top LiquidGlass "Search for apps" Capsule with trailing 3-dot overflow menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .liquidGlassPill(shape = RoundedCornerShape(VRadius.pill))
                    .padding(start = 18.dp, end = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    VIconView(
                        icon = VIcon.search,
                        size = 19.dp,
                        strokeWidth = 1.9f,
                        color = ink.copy(alpha = 0.78f),
                        contentDescription = "Search for apps",
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = VText.inter(16.5f, weight = 500).copy(color = ink),
                        cursorBrush = SolidColor(ink),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apps_library_search_input"),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search for apps",
                                    style = VText.inter(16.5f, weight = 400).copy(color = ink.copy(alpha = 0.72f)),
                                    maxLines = 1,
                                )
                            }
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    query = ""
                                    focusManager.clearFocus()
                                }
                                .testTag("apps_search_cancel"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ink.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                VIconView(VIcon.close, size = 12.dp, strokeWidth = 2.1f, color = ink)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { showOverflowSheet = true }
                                .testTag("apps_settings_button"),
                            contentAlignment = Alignment.Center,
                        ) {
                            VIconView(
                                icon = VIcon.moreVert,
                                size = 19.dp,
                                strokeWidth = 2.0f,
                                color = ink.copy(alpha = 0.84f),
                                contentDescription = "Drawer options",
                            )
                        }
                    }
                }
            }
        }

        // 2. Category Tabs Bar (All · Communication · Entertainment · Tools · ...)
        if (query.isEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(categories, key = { it }) { category ->
                    val selected = category == selectedCategory
                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) {
                                selectedCategory = category
                                scope.launch {
                                    gridState.scrollToItem(0)
                                    listState.scrollToItem(0)
                                }
                            }
                            .padding(vertical = 6.dp)
                            .testTag("apps_cat_${category.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = category,
                            style = VText.inter(
                                size = 17f,
                                weight = if (selected) 600 else 400,
                            ).copy(
                                color = if (selected) ink else ink.copy(alpha = 0.62f),
                            ),
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 26.dp else 0.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(VRadius.pill))
                                .background(if (selected) ink else Color.Transparent),
                        )
                    }
                }
            }
        }

        // 3. Main Content Area + Minimal Right-Edge Scrubber
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (settings.appsView == AppsView.grid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 36.dp,
                        top = 6.dp,
                        bottom = VSpace.tabBarClearance + 12.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Suggested / Frequent Top 4 Row
                    if (showSuggestedRow) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "suggested_row") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                suggestedApps.forEach { app ->
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        SquircleAppTile(
                                            app = app,
                                            vm = vm,
                                            badgeCount = if (settings.dots) dots[app.pkg] ?: 0 else 0,
                                            style = layout.icons,
                                            onLongPress = { onAppMenu(app) },
                                        )
                                    }
                                }
                                repeat((4 - suggestedApps.size).coerceAtLeast(0)) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        // Soft Frosted Glass Divider separating Suggested row from A–Z Grid
                        item(span = { GridItemSpan(maxLineSpan) }, key = "suggested_divider") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .height(0.8.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                ink.copy(alpha = 0.22f),
                                                ink.copy(alpha = 0.22f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                        }
                    }

                    // Main 4-column A–Z Squircle App Grid
                    items(
                        items = filteredApps,
                        key = { it.key },
                    ) { app ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (app.paused) 0.45f else 1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            SquircleAppTile(
                                app = app,
                                vm = vm,
                                badgeCount = if (settings.dots) dots[app.pkg] ?: 0 else 0,
                                style = layout.icons,
                                onLongPress = { onAppMenu(app) },
                            )
                        }
                    }
                }
            } else {
                // LiquidGlass Sectioned List View (toggleable from ⋮ menu)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 40.dp,
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
            }

            // 4. Right-Edge Minimal LiquidGlass Scrubber (♡ A..Z …)
            val activeIndexMap = if (settings.appsView == AppsView.grid) {
                gridIndexByLetter
            } else {
                listIndexByLetter
            }
            if (query.isEmpty() && activeIndexMap.isNotEmpty()) {
                MinimalLiquidScrubber(
                    symbols = scrubberSymbols,
                    availableSymbols = activeIndexMap.keys,
                    activeSymbol = activeSymbol,
                    onSymbol = { sym ->
                        if (sym != activeSymbol) {
                            activeSymbol = sym
                            if (settings.scrubberHaptics) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            val targetIdx = activeIndexMap[sym]
                            if (targetIdx != null) {
                                scope.launch {
                                    if (settings.appsView == AppsView.grid) {
                                        gridState.scrollToItem(targetIdx)
                                    } else {
                                        listState.scrollToItem(targetIdx)
                                    }
                                }
                            }
                        }
                    },
                    onRelease = { activeSymbol = null },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 4.dp, bottom = VSpace.tabBarClearance - 24.dp),
                )
            }

            // 5. Floating LiquidGlass Loupe Sphere while scrubbing
            androidx.compose.animation.AnimatedVisibility(
                visible = activeSymbol != null,
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
                        text = activeSymbol.orEmpty(),
                        style = VText.doto(34f, weight = 700).copy(color = ink),
                    )
                }
            }
        }
    }

    if (showOverflowSheet) {
        CarinoSheet(onDismiss = { showOverflowSheet = false }) {
            Text(
                text = "App Drawer",
                style = VText.doto(24f, weight = 700).copy(color = ink),
            )
            Spacer(Modifier.height(14.dp))
            VListCard {
                VActionRow(
                    title = if (settings.appsView == AppsView.grid) "Switch to list view" else "Switch to grid view",
                    icon = if (settings.appsView == AppsView.grid) VIcon.list else VIcon.apps,
                    onClick = {
                        vm.updateSettings { s ->
                            s.copy(appsView = if (s.appsView == AppsView.grid) AppsView.list else AppsView.grid)
                        }
                        showOverflowSheet = false
                    },
                    tag = "apps_toggle_view",
                )
                VHairlineDivider()
                VActionRow(
                    title = "Launcher settings",
                    icon = VIcon.gear,
                    onClick = {
                        showOverflowSheet = false
                        onOpenSettings()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SquircleAppTile(
    app: AppEntry,
    vm: CarinoViewModel,
    badgeCount: Int,
    style: IconStyle,
    iconSize: Dp = 58.dp,
    onLongPress: () -> Unit,
) {
    val ink = LocalVInk.current
    val tokens = LiquidGlassTheme.tokens
    val haptic = LocalHapticFeedback.current
    var boundsRect: android.graphics.Rect? = null
    val squircleShape = RoundedCornerShape(16.dp)

    val colored = style == IconStyle.colored
    val bitmap by produceState<Bitmap?>(initialValue = null, app.key, colored) {
        value = vm.loadIconBitmap(app.key, mono = !colored)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_tile_${app.pkg}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(iconSize + 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Squircle LiquidGlass Icon Tile with subtle specular top reflection & rim
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        boundsRect = android.graphics.Rect(
                            b.left.roundToInt(),
                            b.top.roundToInt(),
                            b.right.roundToInt(),
                            b.bottom.roundToInt(),
                        )
                    }
                    .clip(squircleShape)
                    .background(
                        if (colored) {
                            Color.White.copy(alpha = 0.14f)
                        } else {
                            if (tokens.isDark) Color(0x52181B26) else Color(0x99FFFFFF)
                        },
                    )
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
                    .drawWithContent {
                        drawContent()
                        // Subtle LiquidGlass specular sheen arc on top edge of each icon tile
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    Color.Transparent,
                                ),
                                startY = 0f,
                                endY = size.height * 0.34f,
                            ),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height * 0.34f),
                        )
                    }
                    .border(
                        width = 0.9.dp,
                        brush = tokens.borderBrush,
                        shape = squircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = app.label,
                        colorFilter = if (colored) null else ColorFilter.tint(ink),
                        contentScale = if (colored) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(if (colored) 1f else 0.62f),
                    )
                } else {
                    val letter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = letter,
                        style = VText.doto(24f, weight = 800).copy(color = ink),
                    )
                }
            }

            // Crisp Coral-Red Notification Count Badge (matching screenshot)
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .defaultMinSize(minWidth = 21.dp, minHeight = 21.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = VText.inter(10.5f, weight = 700).copy(color = Color.White),
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = app.label,
            style = VText.inter(12.5f, weight = 400).copy(color = ink.copy(alpha = 0.94f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
    iconStyle: IconStyle,
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
private fun MinimalLiquidScrubber(
    symbols: List<String>,
    availableSymbols: Set<String>,
    activeSymbol: String?,
    onSymbol: (String) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalVInk.current
    val tokens = LiquidGlassTheme.tokens
    val railShape = RoundedCornerShape(VRadius.pill)

    // Show ♡, available letters, and … if present (falls back to all symbols if very few)
    val visibleSymbols = remember(symbols, availableSymbols) {
        val filtered = symbols.filter { it == "♡" || it in availableSymbols }
        if (filtered.size >= 8) filtered else symbols
    }

    Column(
        modifier = modifier
            .width(22.dp)
            .fillMaxHeight(0.78f)
            .let { base ->
                if (activeSymbol != null) {
                    base.clip(railShape)
                        .background(if (tokens.isDark) Color(0x36161224) else Color(0x55FFFFFF))
                        .border(0.85.dp, tokens.borderBrush, railShape)
                } else {
                    base
                }
            }
            .padding(vertical = 4.dp)
            .pointerInput(visibleSymbols) {
                detectTapGestures { offset ->
                    val i = ((offset.y / size.height.coerceAtLeast(1)) * visibleSymbols.size)
                        .toInt()
                        .coerceIn(0, visibleSymbols.lastIndex)
                    onSymbol(visibleSymbols[i])
                    onRelease()
                }
            }
            .pointerInput(visibleSymbols) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val i = ((offset.y / size.height.coerceAtLeast(1)) * visibleSymbols.size)
                            .toInt()
                            .coerceIn(0, visibleSymbols.lastIndex)
                        onSymbol(visibleSymbols[i])
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val i = ((change.position.y / size.height.coerceAtLeast(1)) * visibleSymbols.size)
                            .toInt()
                            .coerceIn(0, visibleSymbols.lastIndex)
                        onSymbol(visibleSymbols[i])
                    },
                    onDragEnd = onRelease,
                    onDragCancel = onRelease,
                )
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        visibleSymbols.forEach { sym ->
            val isSelected = sym == activeSymbol
            val isHeart = sym == "♡"
            val hasApps = sym in availableSymbols
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
                    text = sym,
                    style = VText.inter(
                        size = if (isSelected) 10.5f else 9.5f,
                        weight = if (isSelected || hasApps) 600 else 400,
                    ).copy(
                        color = when {
                            isSelected -> ink
                            isHeart -> Color(0xFF72D69A)
                            hasApps -> ink.copy(alpha = 0.88f)
                            else -> ink.copy(alpha = 0.36f)
                        },
                    ),
                )
            }
        }
    }
}
