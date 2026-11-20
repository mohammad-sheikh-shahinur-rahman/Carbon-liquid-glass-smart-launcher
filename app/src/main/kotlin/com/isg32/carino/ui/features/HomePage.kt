package com.isg32.carino.ui.features

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.HomeLayout
import com.isg32.carino.data.WeatherService
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.common.AppTile
import com.isg32.carino.ui.common.EmptySlot
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
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
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val appIndex by vm.appIndex.collectAsState()
    val nowMs by vm.nowMs.collectAsState()
    val battery by vm.battery.collectAsState()
    val weather by vm.weather.collectAsState()
    val nextEvent by vm.nextEvent.collectAsState()
    val haptic = LocalHapticFeedback.current

    val dateFmt = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }
    val timeFmt = remember(settings.clock24) {
        SimpleDateFormat(if (settings.clock24) "HH:mm" else "h:mm", Locale.getDefault())
    }
    val eventFmt = remember(settings.clock24) {
        SimpleDateFormat(if (settings.clock24) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var draggingSlot by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverSlot by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = VSpace.titleGutter)
            .padding(top = 18.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (settings.showDate) {
                    Text(
                        text = dateFmt.format(Date(nowMs)).uppercase(),
                        style = VText.eyebrow.copy(color = ink.copy(alpha = 0.68f)),
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = timeFmt.format(Date(nowMs)),
                    style = VText.doto(56f, weight = 700, lineHeight = 58f).copy(color = ink),
                    modifier = Modifier
                        .testTag("home_clock")
                        .let {
                            if (previewOnly) it else it.clickable { vm.repo?.openNamedIntent("clock") }
                        },
                )
            }
            if (editing && !previewOnly) {
                VPillButton(
                    label = "Done",
                    inverted = true,
                    onClick = onDoneEditing,
                    tag = "home_edit_done",
                )
            }
        }

        // Glance line: weather · battery · next event
        val glanceParts = buildList {
            weather?.let { w ->
                val unit = if (w.fahrenheit) "°F" else "°"
                add("${w.temp.roundToInt()}$unit ${WeatherService.describe(w.code)}")
            }
            if (settings.showBattery) {
                battery?.let { b ->
                    add(if (b.charging) "${b.level}% ⚡" else "${b.level}%")
                }
            }
            if (settings.showNextEvent) {
                nextEvent?.let { ev ->
                    val whenStr = if (ev.ongoing(nowMs)) "now" else eventFmt.format(Date(ev.begin))
                    add("${ev.title} · $whenStr")
                }
            }
        }
        if (glanceParts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = glanceParts.joinToString("  ·  "),
                style = VText.secondary.copy(color = ink.copy(alpha = 0.72f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))

        // Favourites Grid (favRows x columns)
        val favs = layout.favGrid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            for (r in 0 until layout.favRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for (c in 0 until layout.columns) {
                        val slotIdx = r * layout.columns + c
                        val key = favs.getOrNull(slotIdx)
                        val app = key?.let { appIndex[it] }
                        HomeSlotItem(
                            slotIndex = slotIdx,
                            app = app,
                            layout = layout,
                            vm = vm,
                            editing = editing,
                            previewOnly = previewOnly,
                            isDragging = draggingSlot == slotIdx,
                            isHovered = hoverSlot == slotIdx && draggingSlot >= 0 && draggingSlot != slotIdx,
                            dragOffset = if (draggingSlot == slotIdx) dragOffset else Offset.Zero,
                            onBounds = { rect -> slotBounds[slotIdx] = rect },
                            onTapEmpty = { onPickSlot(false, slotIdx) },
                            onRemove = { vm.setFavourite(slotIdx, null) },
                            onLongPress = {
                                if (!editing && app != null && !previewOnly) onAppMenu(app)
                            },
                            onDragStart = {
                                draggingSlot = slotIdx
                                dragOffset = Offset.Zero
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { delta ->
                                dragOffset += delta
                                val currentRect = slotBounds[slotIdx]
                                if (currentRect != null) {
                                    val center = currentRect.center + dragOffset
                                    hoverSlot = slotBounds.entries.firstOrNull { (_, rRect) ->
                                        rRect.contains(center)
                                    }?.key ?: -1
                                }
                            },
                            onDragEnd = {
                                if (draggingSlot >= 0 && hoverSlot >= 0 && draggingSlot != hoverSlot) {
                                    vm.swapSlots(draggingSlot, hoverSlot)
                                }
                                draggingSlot = -1
                                hoverSlot = -1
                                dragOffset = Offset.Zero
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Dock row (shared in Home & Customise preview; ShellScreen places persistent dock above VTabBar)
        val dock = layout.dockRow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (c in 0 until layout.columns) {
                val slotIdx = layout.favSlots + c
                val key = dock.getOrNull(c)
                val app = key?.let { appIndex[it] }
                HomeSlotItem(
                    slotIndex = slotIdx,
                    app = app,
                    layout = layout,
                    vm = vm,
                    editing = editing,
                    previewOnly = previewOnly,
                    showLabel = false,
                    isDragging = draggingSlot == slotIdx,
                    isHovered = hoverSlot == slotIdx && draggingSlot >= 0 && draggingSlot != slotIdx,
                    dragOffset = if (draggingSlot == slotIdx) dragOffset else Offset.Zero,
                    onBounds = { rect -> slotBounds[slotIdx] = rect },
                    onTapEmpty = { onPickSlot(true, c) },
                    onRemove = { vm.setDock(c, null) },
                    onLongPress = {
                        if (!editing && app != null && !previewOnly) onAppMenu(app)
                    },
                    onDragStart = {
                        draggingSlot = slotIdx
                        dragOffset = Offset.Zero
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { delta ->
                        dragOffset += delta
                        val currentRect = slotBounds[slotIdx]
                        if (currentRect != null) {
                            val center = currentRect.center + dragOffset
                            hoverSlot = slotBounds.entries.firstOrNull { (_, rRect) ->
                                rRect.contains(center)
                            }?.key ?: -1
                        }
                    },
                    onDragEnd = {
                        if (draggingSlot >= 0 && hoverSlot >= 0 && draggingSlot != hoverSlot) {
                            vm.swapSlots(draggingSlot, hoverSlot)
                        }
                        draggingSlot = -1
                        hoverSlot = -1
                        dragOffset = Offset.Zero
                    },
                )
            }
        }

        // Reserve space for floating VTabBar
        Spacer(Modifier.height(82.dp))
    }
}

@Composable
private fun HomeSlotItem(
    slotIndex: Int,
    app: AppEntry?,
    layout: HomeLayout,
    vm: CarinoViewModel,
    editing: Boolean,
    previewOnly: Boolean,
    showLabel: Boolean = true,
    isDragging: Boolean,
    isHovered: Boolean,
    dragOffset: Offset,
    onBounds: (Rect) -> Unit,
    onTapEmpty: () -> Unit,
    onRemove: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val pal = LocalVPalette.current
    val ink = pal.ink
    val iconSize = if (layout.columns >= 5) 48.dp else 56.dp

    val dragMod = if (editing && app != null && !previewOnly) {
        Modifier
            .zIndex(if (isDragging) 10f else 0f)
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .pointerInput(app.key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                )
            }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords -> onBounds(coords.boundsInWindow()) }
            .then(dragMod),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (app == null) {
            if (editing && !previewOnly) {
                EmptySlot(
                    size = iconSize,
                    highlight = isHovered,
                    showLabelSpacer = showLabel,
                    onClick = onTapEmpty,
                )
            } else {
                Spacer(Modifier.size(iconSize + 16.dp, if (showLabel) iconSize + 24.dp else iconSize))
            }
        } else if (previewOnly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AppGlyph(app = app, vm = vm, size = iconSize, style = layout.icons, blur = layout.iconBlur)
                if (showLabel) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = app.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = VText.inter(11f, weight = 500).copy(color = ink),
                    )
                }
            }
        } else if (editing) {
            Box(modifier = Modifier.alpha(if (isHovered) 0.55f else 1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppGlyph(
                        app = app,
                        vm = vm,
                        size = iconSize,
                        style = layout.icons,
                        lifted = isDragging,
                        blur = layout.iconBlur,
                    )
                    if (showLabel) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = app.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = VText.inter(12f, weight = 500).copy(color = ink),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
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
        } else {
            AppTile(
                app = app,
                vm = vm,
                size = iconSize,
                style = layout.icons,
                showLabel = showLabel,
                blur = layout.iconBlur,
                onLongPress = onLongPress,
            )
        }
    }
}
