package com.isg32.carino.ui.features

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.BatteryState
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.HomeLayout
import com.isg32.carino.data.Weather
import com.isg32.carino.data.WeatherService
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.components.VChip
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.theme.LiquidGlassTheme
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VText
import com.isg32.carino.ui.theme.liquidGlassPill
import com.isg32.carino.ui.theme.liquidGlassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    vm: CarinoViewModel,
    onAppMenu: (AppEntry) -> Unit,
    onPickSlot: (isDock: Boolean, slotIndex: Int) -> Unit,
    onOpenVault: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenWeatherSettings: () -> Unit = {},
    onOpenApps: () -> Unit = {},
) {
    val layout by vm.activeLayout.collectAsState()
    val settings by vm.settings.collectAsState()
    val editHome by vm.editHome.collectAsState()
    val haptic = LocalHapticFeedback.current

    if (editHome) {
        BackHandler { vm.setEditHome(false) }
    }

    var verticalDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(editHome) {
                if (editHome) return@pointerInput
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom > 1.28f) {
                        onOpenVault()
                    }
                }
            }
            .pointerInput(editHome, settings.swipeUp, settings.swipeDown) {
                if (editHome) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { verticalDrag = 0f },
                    onVerticalDrag = { _, delta -> verticalDrag += delta },
                    onDragEnd = {
                        if (verticalDrag < -72f) {
                            vm.runGesture(settings.swipeUp)
                        } else if (verticalDrag > 72f) {
                            vm.runGesture(settings.swipeDown)
                        }
                        verticalDrag = 0f
                    },
                    onDragCancel = { verticalDrag = 0f },
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onDoubleClick = {
                    if (!editHome) vm.runGesture(settings.doubleTap)
                },
                onLongClick = {
                    if (!editHome) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.openCustomise()
                    }
                },
            ),
    ) {
        HomeScene(
            layout = layout,
            vm = vm,
            editing = editHome,
            onDoneEditing = { vm.setEditHome(false) },
            onAppMenu = onAppMenu,
            onPickSlot = onPickSlot,
            onOpenSearch = onOpenSearch,
            onOpenWeatherSettings = onOpenWeatherSettings,
            onOpenApps = onOpenApps,
        )
    }
}

@Composable
fun HomeScene(
    layout: HomeLayout,
    vm: CarinoViewModel,
    editing: Boolean = false,
    previewOnly: Boolean = false,
    onDoneEditing: () -> Unit = {},
    onAppMenu: (AppEntry) -> Unit = {},
    onPickSlot: (Boolean, Int) -> Unit = { _, _ -> },
    onOpenSearch: () -> Unit = {},
    onOpenWeatherSettings: () -> Unit = {},
    onOpenApps: () -> Unit = {},
) {
    val settings by vm.settings.collectAsState()
    val appIndex by vm.appIndex.collectAsState()
    val visibleApps by vm.visibleApps.collectAsState()
    val allApps by vm.apps.collectAsState()
    val nowMs by vm.nowMs.collectAsState()
    val battery by vm.battery.collectAsState()
    val weather by vm.weather.collectAsState()
    val dots by vm.dots.collectAsState()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }

    if (searchQuery.isNotEmpty() && !previewOnly) {
        BackHandler {
            searchQuery = ""
            focusManager.clearFocus()
        }
    }

    val filteredApps = remember(searchQuery, visibleApps, allApps, settings.hidden, settings.hiddenFindByName) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            val matched = visibleApps.filter { it.label.lowercase().contains(q) }
            val hiddenExact = if (settings.hiddenFindByName) {
                allApps.filter { it.key in settings.hidden && it.label.lowercase() == q }
            } else {
                emptyList()
            }
            (hiddenExact + matched)
                .distinctBy { it.key }
                .sortedWith(
                    compareByDescending<AppEntry> { it.label.lowercase().startsWith(q) }
                        .thenBy { it.label.lowercase() },
                )
        }
    }

    val timeFmt = remember(settings.clock24) {
        SimpleDateFormat(if (settings.clock24) "HH:mm" else "hh:mm", Locale.getDefault())
    }
    val dateFmt = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    // Resolve the 7 featured home orbs (4 in left 2x2 glass folder + 3 in right 2x2 glass folder)
    // plus 4 apps for the mini 2x2 sub-folder glass pod.
    val resolvedSlots: List<AppEntry?> = remember(layout.favourites, layout.dock, appIndex, visibleApps) {
        val explicit = (layout.favGrid + layout.dockRow).map { key -> key?.let { appIndex[it] } }
        val nonNullExplicit = explicit.filterNotNull()
        val usedKeys = nonNullExplicit.map { it.key }.toMutableSet()
        val fallbackQueue = ArrayDeque(visibleApps.filter { it.key !in usedKeys })

        List(7) { idx ->
            explicit.getOrNull(idx) ?: if (fallbackQueue.isNotEmpty()) fallbackQueue.removeFirst() else null
        }
    }

    val folderPodApps: List<AppEntry> = remember(resolvedSlots, visibleApps) {
        val usedKeys = resolvedSlots.filterNotNull().map { it.key }.toSet()
        val extras = visibleApps.filter { it.key !in usedKeys }.take(4)
        if (extras.size >= 4) extras else (extras + visibleApps).distinctBy { it.key }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 86.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 0. Top Glass-Morphism App Filter Search Bar
        HomeTopGlassSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onClear = {
                searchQuery = ""
                focusManager.clearFocus()
            },
            onSubmit = {
                if (settings.enterLaunches && filteredApps.isNotEmpty()) {
                    vm.launchApp(filteredApps.first().key)
                    searchQuery = ""
                    focusManager.clearFocus()
                }
            },
            previewOnly = previewOnly,
        )

        if (searchQuery.isNotBlank() && !previewOnly) {
            HomeFilteredAppsResults(
                query = searchQuery,
                apps = filteredApps,
                vm = vm,
                layout = layout,
                showNewBadges = settings.newBadges,
                onLaunch = { app, rect ->
                    vm.launchApp(app.key, rect)
                    searchQuery = ""
                    focusManager.clearFocus()
                },
                onLongPress = onAppMenu,
                onClear = {
                    searchQuery = ""
                    focusManager.clearFocus()
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            // 1. Hero Stretched Condensed Glass Clock
            Box(modifier = Modifier.fillMaxWidth()) {
                HeroStretchedGlassClock(
                    timeText = timeFmt.format(Date(nowMs)),
                    onClick = {
                        if (!previewOnly) vm.repo?.openNamedIntent("clock")
                    },
                )
                if (editing && !previewOnly) {
                    VPillButton(
                        label = "Done",
                        inverted = true,
                        onClick = onDoneEditing,
                        tag = "home_edit_done",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp),
                    )
                }
            }

            // 2. Wide Frosted LiquidGlass Weather Card
            HomeWeatherGlassCard(
                weather = weather,
                battery = battery,
                dateText = dateFmt.format(Date(nowMs)),
                onClick = {
                    if (!previewOnly) onOpenWeatherSettings()
                },
            )

            Spacer(Modifier.weight(1f))

        // 3. Dual Side-by-Side 2x2 Frosted LiquidGlass Folder Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Left 2x2 LiquidGlass App Card (slots 0..3)
            HomeGlassFolderCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeGlassOrbSlot(
                            slotIndex = 0,
                            app = resolvedSlots.getOrNull(0),
                            badgeCount = resolvedSlots.getOrNull(0)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 0) },
                            onRemove = { vm.setFavourite(0, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                        HomeGlassOrbSlot(
                            slotIndex = 1,
                            app = resolvedSlots.getOrNull(1),
                            badgeCount = resolvedSlots.getOrNull(1)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 1) },
                            onRemove = { vm.setFavourite(1, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeGlassOrbSlot(
                            slotIndex = 2,
                            app = resolvedSlots.getOrNull(2),
                            badgeCount = resolvedSlots.getOrNull(2)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 2) },
                            onRemove = { vm.setFavourite(2, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                        HomeGlassOrbSlot(
                            slotIndex = 3,
                            app = resolvedSlots.getOrNull(3),
                            badgeCount = resolvedSlots.getOrNull(3)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 3) },
                            onRemove = { vm.setFavourite(3, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                }
            }

            // Right 2x2 LiquidGlass App Card (slots 4, 5, mini sub-folder pod, slot 6)
            HomeGlassFolderCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HomeGlassOrbSlot(
                            slotIndex = 4,
                            app = resolvedSlots.getOrNull(4),
                            badgeCount = resolvedSlots.getOrNull(4)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 4) },
                            onRemove = { vm.setFavourite(4, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                        HomeGlassOrbSlot(
                            slotIndex = 5,
                            app = resolvedSlots.getOrNull(5),
                            badgeCount = resolvedSlots.getOrNull(5)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 5) },
                            onRemove = { vm.setFavourite(5, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MiniFolderGlassPod(
                            apps = folderPodApps,
                            vm = vm,
                            onClick = {
                                if (!previewOnly) onOpenApps()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                        HomeGlassOrbSlot(
                            slotIndex = 6,
                            app = resolvedSlots.getOrNull(6),
                            badgeCount = resolvedSlots.getOrNull(6)?.let { if (settings.dots) dots[it.pkg] ?: 0 else 0 } ?: 0,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            onPick = { onPickSlot(false, 6) },
                            onRemove = { vm.setFavourite(6, null) },
                            onAppMenu = onAppMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                        )
                    }
                }
            }
        }

            // 4. Bottom Smoked LiquidGlass Search Capsule (G ... Mic + Lens)
            HomeGlassSearchPill(
                onSearch = { if (!previewOnly) onOpenSearch() },
                onVoice = { if (!previewOnly) onOpenSearch() },
                onLens = { if (!previewOnly) vm.repo?.openNamedIntent("camera") },
            )
        }
    }
}

@Composable
private fun HomeTopGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    previewOnly: Boolean,
) {
    val ink = LocalVInk.current
    val pillShape = RoundedCornerShape(VRadius.pill)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .liquidGlassPill(shape = pillShape, smoked = true)
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
                    size = 17.dp,
                    strokeWidth = 2.0f,
                    color = ink.copy(alpha = 0.84f),
                    contentDescription = "Search installed apps",
                )
                if (previewOnly) {
                    Text(
                        text = "Search apps…",
                        style = VText.inter(15f, weight = 500).copy(color = ink.copy(alpha = 0.65f)),
                        maxLines = 1,
                    )
                } else {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = VText.inter(15f, weight = 500).copy(color = ink),
                        cursorBrush = SolidColor(ink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_top_search_input"),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search apps…",
                                    style = VText.inter(15f, weight = 500).copy(color = ink.copy(alpha = 0.65f)),
                                    maxLines = 1,
                                )
                            }
                            inner()
                        },
                    )
                }
                if (query.isNotEmpty() && !previewOnly) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ink.copy(alpha = 0.18f))
                            .clickable(role = Role.Button, onClick = onClear)
                            .testTag("home_top_search_clear"),
                        contentAlignment = Alignment.Center,
                    ) {
                        VIconView(
                            icon = VIcon.close,
                            size = 12.dp,
                            strokeWidth = 2.1f,
                            color = ink,
                            contentDescription = "Clear search",
                        )
                    }
                }
            }
        }

        if (query.isNotEmpty() && !previewOnly) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .liquidGlassPill(shape = pillShape, smoked = true)
                    .clickable(role = Role.Button, onClick = onClear)
                    .padding(horizontal = 14.dp)
                    .testTag("home_top_search_cancel"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancel",
                    style = VText.inter(14f, weight = 600).copy(color = ink),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeFilteredAppsResults(
    query: String,
    apps: List<AppEntry>,
    vm: CarinoViewModel,
    layout: HomeLayout,
    showNewBadges: Boolean,
    onLaunch: (AppEntry, android.graphics.Rect?) -> Unit,
    onLongPress: (AppEntry) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = LocalVInk.current
    val haptic = LocalHapticFeedback.current

    if (apps.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .liquidGlassSurface(
                        shape = RoundedCornerShape(28.dp),
                        elevated = false,
                        showSpecularSheen = true,
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VIconView(
                        icon = VIcon.search,
                        size = 28.dp,
                        strokeWidth = 1.6f,
                        color = ink.copy(alpha = 0.55f),
                    )
                    Text(
                        text = "No apps match \"${query.trim()}\"",
                        style = VText.rowTitle.copy(color = ink),
                    )
                    Text(
                        text = "Try a different app name or clear the search.",
                        style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                    )
                    Spacer(Modifier.height(4.dp))
                    VPillButton(
                        label = "Clear filter",
                        onClick = onClear,
                        tag = "home_search_empty_clear",
                    )
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .liquidGlassSurface(
                    shape = RoundedCornerShape(28.dp),
                    elevated = false,
                    showSpecularSheen = true,
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_filtered_apps_list"),
            ) {
                items(apps, key = { it.key }) { app ->
                    var boundsRect: android.graphics.Rect? = null
                    Column(modifier = Modifier.fillMaxWidth()) {
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
                                        onLaunch(app, boundsRect)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPress(app)
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppGlyph(app = app, vm = vm, size = 42.dp, style = layout.icons, blur = true)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = app.label,
                                style = VText.inter(15.5f, weight = 500).copy(color = ink),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (showNewBadges && app.isNew()) {
                                VChip(label = "New")
                                Spacer(Modifier.width(6.dp))
                            }
                            VIconView(
                                icon = VIcon.chevron,
                                size = 15.dp,
                                strokeWidth = 1.6f,
                                color = ink.copy(alpha = 0.32f),
                            )
                        }
                        if (app.key != apps.last().key) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 72.dp, end = 16.dp)
                                    .height(0.6.dp)
                                    .background(ink.copy(alpha = 0.14f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smoked LiquidGlass surface modifier tailored for the Home screen editorial cards
 * (clean specular edge rim + ultra-sheer smoked glass interior + top-left sheen).
 */
private fun Modifier.homeSmokedGlass(
    shape: androidx.compose.ui.graphics.Shape,
    isDark: Boolean,
    fillAlpha: Float = 0.24f,
): Modifier = this
    .clip(shape)
    .background(
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A2029).copy(alpha = fillAlpha + 0.14f),
                    Color(0xFF11151C).copy(alpha = fillAlpha + 0.24f),
                ),
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.72f),
                    Color(0xFFEBEAF2).copy(alpha = 0.56f),
                ),
            )
        },
    )
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.14f else 0.42f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height * 0.40f,
            ),
            topLeft = Offset.Zero,
            size = Size(size.width, size.height * 0.40f),
        )
    }
    .border(
        width = 1.dp,
        brush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.52f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.34f),
                ),
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.94f),
                    Color.Black.copy(alpha = 0.14f),
                    Color.White.copy(alpha = 0.72f),
                ),
            )
        },
        shape = shape,
    )

@Composable
private fun HeroStretchedGlassClock(
    timeText: String,
    onClick: () -> Unit,
) {
    val tokens = LiquidGlassTheme.tokens
    val ink = LocalVInk.current
    val plateShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .testTag("home_clock")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Back frosted glass plate positioned across the lower ~64% of the hero numerals
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .homeSmokedGlass(shape = plateShape, isDark = tokens.isDark, fillAlpha = 0.14f),
        )

        // Massive tall stretched condensed clock numerals with vertical frost fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = timeText,
                maxLines = 1,
                softWrap = false,
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 94.sp,
                    letterSpacing = (-3).sp,
                    brush = Brush.verticalGradient(
                        0.0f to ink.copy(alpha = 0.98f),
                        0.50f to ink.copy(alpha = 0.90f),
                        0.76f to ink.copy(alpha = 0.42f),
                        1.0f to ink.copy(alpha = 0.10f),
                    ),
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.80f
                    scaleY = 1.50f
                },
            )
        }

        // Front ultra-sheer refractive glass meniscus rim over the lower plate so digits appear submerged in glass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(plateShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.22f),
                            Color.Transparent,
                            Color.White.copy(alpha = if (tokens.isDark) 0.05f else 0.14f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.46f else 0.90f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = if (tokens.isDark) 0.34f else 0.72f),
                        ),
                    ),
                    shape = plateShape,
                ),
        )
    }
}

@Composable
private fun HomeWeatherGlassCard(
    weather: Weather?,
    battery: BatteryState?,
    dateText: String,
    onClick: () -> Unit,
) {
    val tokens = LiquidGlassTheme.tokens
    val ink = LocalVInk.current
    val shape = RoundedCornerShape(30.dp)

    val tempText = weather?.let { "${it.temp.roundToInt()}°" } ?: "—°"
    val conditionTitle = weather?.let { WeatherService.describe(it.code) } ?: "Weather"
    val conditionSub = weather?.place?.takeIf { it.isNotBlank() } ?: "Tap to set location"
    val precipText = weather?.precipChance?.let { "$it%" }
        ?: battery?.let { if (it.charging) "${it.level}% ⚡" else "${it.level}%" }
        ?: "—"
    val rangeText = if (weather?.high != null && weather.low != null) {
        "↑ ${weather.high.roundToInt()}°  ↓ ${weather.low.roundToInt()}°"
    } else {
        dateText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .testTag("home_weather_card")
            .homeSmokedGlass(shape = shape, isDark = tokens.isDark, fillAlpha = 0.20f)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    WeatherConditionGlyph(
                        code = weather?.code ?: 71,
                        tint = ink,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = tempText,
                        style = VText.inter(40f, weight = 600, lineHeight = 42f).copy(color = ink),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = conditionTitle,
                    style = VText.inter(16f, weight = 600).copy(color = ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = conditionSub,
                    style = VText.inter(12.5f, weight = 400).copy(color = ink.copy(alpha = 0.68f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    VIconView(
                        icon = if (weather != null) VIcon.umbrella else VIcon.battery,
                        size = 14.dp,
                        strokeWidth = 1.7f,
                        color = ink.copy(alpha = 0.85f),
                    )
                    Text(
                        text = precipText,
                        style = VText.inter(13.5f, weight = 600).copy(color = ink.copy(alpha = 0.90f)),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = rangeText,
                    style = VText.inter(13f, weight = 500).copy(color = ink.copy(alpha = 0.80f)),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WeatherConditionGlyph(
    code: Int,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cloudColor = tint.copy(alpha = 0.92f)
        val cloudBack = tint.copy(alpha = 0.52f)

        if (code == 0) {
            // Crisp sun glyph
            drawCircle(
                color = cloudColor,
                radius = w * 0.28f,
                center = Offset(w * 0.5f, h * 0.5f),
            )
            val rayLen = w * 0.13f
            val inner = w * 0.36f
            for (i in 0 until 8) {
                val rad = Math.toRadians(i * 45.0)
                val cos = kotlin.math.cos(rad).toFloat()
                val sin = kotlin.math.sin(rad).toFloat()
                drawLine(
                    color = cloudColor,
                    start = Offset(w * 0.5f + cos * inner, h * 0.5f + sin * inner),
                    end = Offset(w * 0.5f + cos * (inner + rayLen), h * 0.5f + sin * (inner + rayLen)),
                    strokeWidth = w * 0.06f,
                    cap = StrokeCap.Round,
                )
            }
        } else {
            // Back subtle cloud puff
            drawCircle(color = cloudBack, radius = w * 0.22f, center = Offset(w * 0.38f, h * 0.38f))
            // Foreground cloud puffs + flat base
            drawCircle(color = cloudColor, radius = w * 0.22f, center = Offset(w * 0.62f, h * 0.36f))
            drawCircle(color = cloudColor, radius = w * 0.17f, center = Offset(w * 0.36f, h * 0.44f))
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.20f, h * 0.38f),
                size = Size(w * 0.62f, h * 0.24f),
                cornerRadius = CornerRadius(h * 0.12f, h * 0.12f),
            )

            // Precipitation dots (snow / rain / drizzle)
            val isRainOrSnow = code >= 51 || code == 71
            if (isRainOrSnow) {
                val dotR = w * 0.036f
                val rows = listOf(0.74f, 0.87f)
                val cols = listOf(0.28f, 0.44f, 0.60f, 0.74f)
                rows.forEachIndexed { rIdx, ry ->
                    cols.forEachIndexed { cIdx, cx ->
                        if (rIdx == 0 || cIdx in 1..2) {
                            drawCircle(
                                color = tint.copy(alpha = 0.82f),
                                radius = dotR,
                                center = Offset(w * cx, h * ry),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGlassFolderCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LiquidGlassTheme.tokens
    val shape = RoundedCornerShape(32.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .homeSmokedGlass(shape = shape, isDark = tokens.isDark, fillAlpha = 0.28f)
            .padding(14.dp),
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeGlassOrbSlot(
    slotIndex: Int,
    app: AppEntry?,
    badgeCount: Int,
    vm: CarinoViewModel,
    editing: Boolean,
    previewOnly: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LiquidGlassTheme.tokens
    val ink = LocalVInk.current
    val pal = LocalVPalette.current
    val haptic = LocalHapticFeedback.current
    var boundsRect: android.graphics.Rect? = null

    val monoBitmap by produceState<Bitmap?>(initialValue = null, app?.key) {
        value = app?.let { vm.loadIconBitmap(it.key, mono = true) }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    boundsRect = android.graphics.Rect(
                        b.left.roundToInt(),
                        b.top.roundToInt(),
                        b.right.roundToInt(),
                        b.bottom.roundToInt(),
                    )
                }
                .clip(CircleShape)
                .background(
                    if (tokens.isDark) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x5236414F),
                                Color(0x7A12161E),
                            ),
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.85f),
                                Color(0x66D5D7E4),
                            ),
                        )
                    },
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.44f else 0.92f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = if (tokens.isDark) 0.28f else 0.65f),
                        ),
                    ),
                    shape = CircleShape,
                )
                .let { base ->
                    if (previewOnly) {
                        base
                    } else if (editing) {
                        base.clickable { onPick() }
                    } else if (app != null) {
                        base.combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.launchApp(app.key, boundsRect)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppMenu(app)
                            },
                        )
                    } else {
                        base.clickable { onPick() }
                    }
                }
                .testTag("home_glass_orb_$slotIndex"),
            contentAlignment = Alignment.Center,
        ) {
            if (app == null) {
                VIconView(
                    icon = VIcon.plus,
                    size = 20.dp,
                    strokeWidth = 1.6f,
                    color = ink.copy(alpha = 0.55f),
                )
            } else {
                val bmp = monoBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = app.label,
                        colorFilter = ColorFilter.tint(
                            if (tokens.isDark) Color(0xFFEAEFF5) else Color(0xFF1A1E28),
                        ),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(0.56f),
                    )
                } else {
                    Text(
                        text = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = VText.doto(22f, weight = 800).copy(color = ink),
                    )
                }
            }
        }

        // Crisp white notification badge (e.g., 11, 1) floating at TopEnd
        if (!editing && badgeCount > 0 && app != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    style = VText.inter(10.5f, weight = 700).copy(color = Color.Black),
                    maxLines = 1,
                )
            }
        }

        // Remove badge in Edit mode
        if (editing && !previewOnly && app != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ink)
                    .border(1.5.dp, pal.paper, CircleShape)
                    .clickable { onRemove() }
                    .testTag("home_slot_remove_$slotIndex"),
                contentAlignment = Alignment.Center,
            ) {
                VIconView(icon = VIcon.minus, size = 12.dp, strokeWidth = 2.2f, color = VColors.onColor(ink))
            }
        }
    }
}

@Composable
private fun MiniFolderGlassPod(
    apps: List<AppEntry>,
    vm: CarinoViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LiquidGlassTheme.tokens
    val ink = LocalVInk.current

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(if (tokens.isDark) Color(0x36141922) else Color(0x55FFFFFF))
                .border(
                    width = 0.9.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (tokens.isDark) 0.36f else 0.88f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = if (tokens.isDark) 0.22f else 0.60f),
                        ),
                    ),
                    shape = RoundedCornerShape(22.dp),
                )
                .clickable(role = Role.Button, onClick = onClick)
                .padding(7.dp)
                .testTag("home_mini_folder_pod"),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (r in 0..1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        for (c in 0..1) {
                            val item = apps.getOrNull(r * 2 + c)
                            val bmp by produceState<Bitmap?>(initialValue = null, item?.key) {
                                value = item?.let { vm.loadIconBitmap(it.key, mono = true) }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(if (tokens.isDark) Color(0x4D2A3340) else Color(0x66FFFFFF)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp!!.asImageBitmap(),
                                        contentDescription = item?.label,
                                        colorFilter = ColorFilter.tint(ink.copy(alpha = 0.88f)),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize(0.68f),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(ink.copy(alpha = 0.45f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGlassSearchPill(
    onSearch: () -> Unit,
    onVoice: () -> Unit,
    onLens: () -> Unit,
) {
    val tokens = LiquidGlassTheme.tokens
    val ink = LocalVInk.current
    val pillShape = RoundedCornerShape(VRadius.pill)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("home_glass_search_pill")
            .homeSmokedGlass(shape = pillShape, isDark = tokens.isDark, fillAlpha = 0.28f)
            .clickable(role = Role.Button, onClick = onSearch)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "G",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = ink.copy(alpha = 0.94f),
                ),
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onVoice)
                    .testTag("home_search_mic"),
                contentAlignment = Alignment.Center,
            ) {
                VIconView(
                    icon = VIcon.mic,
                    size = 19.dp,
                    strokeWidth = 1.9f,
                    color = ink.copy(alpha = 0.90f),
                    contentDescription = "Voice search",
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onLens)
                    .testTag("home_search_lens"),
                contentAlignment = Alignment.Center,
            ) {
                VIconView(
                    icon = VIcon.lens,
                    size = 19.dp,
                    strokeWidth = 1.9f,
                    color = ink.copy(alpha = 0.90f),
                    contentDescription = "Visual search",
                )
            }
        }
    }
}
