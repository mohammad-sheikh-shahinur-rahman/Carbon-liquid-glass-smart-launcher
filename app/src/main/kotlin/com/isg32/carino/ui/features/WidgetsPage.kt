package com.isg32.carino.ui.features

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.isg32.carino.data.BuiltinWidget
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.WeatherService
import com.isg32.carino.data.WidgetKey
import com.isg32.carino.data.WidgetProviderInfo
import com.isg32.carino.ui.components.CarinoSheet
import com.isg32.carino.ui.components.DotLoader
import com.isg32.carino.ui.components.VActionRow
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VIconButton
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VOutline
import com.isg32.carino.ui.components.VOutlined
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.components.VSectionTitle
import com.isg32.carino.ui.components.VSegmented
import com.isg32.carino.ui.components.VSlider
import com.isg32.carino.ui.components.VSurface
import com.isg32.carino.ui.components.VTitleHeader
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface WidgetRowItem {
    data class PairRow(val left: String, val right: String) : WidgetRowItem
    data class SingleRow(val key: String) : WidgetRowItem
}

private fun BuiltinWidget.isSmall(): Boolean =
    this == BuiltinWidget.clock || this == BuiltinWidget.battery || this == BuiltinWidget.weather

private fun BuiltinWidget.label(): String = when (this) {
    BuiltinWidget.media -> "Now playing"
    BuiltinWidget.upNext -> "Up next"
    BuiltinWidget.clock -> "Clock"
    BuiltinWidget.weather -> "Weather"
    BuiltinWidget.battery -> "Battery"
    BuiltinWidget.calendar -> "Calendar"
    BuiltinWidget.notes -> "Notes"
}

@Composable
fun WidgetsPage(
    vm: CarinoViewModel,
    onOpenSettings: () -> Unit,
    onOpenWeatherSettings: () -> Unit,
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var menuKey by remember { mutableStateOf<String?>(null) }

    val rows = remember(settings.widgets) {
        buildList {
            var i = 0
            val list = settings.widgets
            while (i < list.size) {
                val cur = list[i]
                val bCur = WidgetKey.builtin(cur)
                val next = list.getOrNull(i + 1)
                val bNext = next?.let { WidgetKey.builtin(it) }
                if (bCur != null && bCur.isSmall() && bNext != null && bNext.isSmall()) {
                    add(WidgetRowItem.PairRow(cur, next))
                    i += 2
                } else {
                    add(WidgetRowItem.SingleRow(cur))
                    i += 1
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        VTitleHeader(
            title = "Widgets",
            trailing = {
                VIconButton(
                    icon = VIcon.plus,
                    contentDescription = "Add widget",
                    onClick = { showAddSheet = true },
                    outlined = true,
                    tag = "widgets_add_button",
                )
                VIconButton(
                    icon = VIcon.gear,
                    contentDescription = "Settings",
                    onClick = onOpenSettings,
                    outlined = true,
                    tag = "widgets_settings_button",
                )
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = VSpace.gutter,
                end = VSpace.gutter,
                top = 4.dp,
                bottom = VSpace.tabBarClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(VSpace.cardGap),
        ) {
            if (rows.isEmpty()) {
                item {
                    VSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No widgets yet", style = VText.rowTitle.copy(color = ink))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Tap + above to add Carbon or Android widgets.",
                                style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            items(rows, key = { row ->
                when (row) {
                    is WidgetRowItem.PairRow -> "p_${row.left}_${row.right}"
                    is WidgetRowItem.SingleRow -> "s_${row.key}"
                }
            }) { row ->
                when (row) {
                    is WidgetRowItem.PairRow -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(VSpace.cardGap),
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                WidgetCardHost(
                                    key = row.left,
                                    vm = vm,
                                    onLongPress = { menuKey = row.left },
                                    onOpenWeatherSettings = onOpenWeatherSettings,
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                WidgetCardHost(
                                    key = row.right,
                                    vm = vm,
                                    onLongPress = { menuKey = row.right },
                                    onOpenWeatherSettings = onOpenWeatherSettings,
                                )
                            }
                        }
                    }
                    is WidgetRowItem.SingleRow -> {
                        WidgetCardHost(
                            key = row.key,
                            vm = vm,
                            onLongPress = { menuKey = row.key },
                            onOpenWeatherSettings = onOpenWeatherSettings,
                        )
                    }
                }
            }
        }
    }

    menuKey?.let { key ->
        WidgetMenuSheet(
            key = key,
            vm = vm,
            onDismiss = { menuKey = null },
        )
    }

    if (showAddSheet) {
        AddWidgetSheet(
            vm = vm,
            onDismiss = { showAddSheet = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetCardHost(
    key: String,
    vm: CarinoViewModel,
    onLongPress: () -> Unit,
    onOpenWeatherSettings: () -> Unit,
) {
    val builtin = WidgetKey.builtin(key)
    val hostedId = WidgetKey.hostedId(key)
    val settings by vm.settings.collectAsState()

    if (builtin != null) {
        VSurface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .testTag("widget_$key"),
        ) {
            when (builtin) {
                BuiltinWidget.media -> NowPlayingCard(vm)
                BuiltinWidget.upNext -> UpNextCard(vm)
                BuiltinWidget.clock -> ClockCard(vm)
                BuiltinWidget.weather -> WeatherCard(vm, onOpenWeatherSettings)
                BuiltinWidget.battery -> BatteryCard(vm)
                BuiltinWidget.calendar -> CalendarCard(vm)
                BuiltinWidget.notes -> NotesCard(vm)
            }
        }
    } else if (hostedId != null) {
        val rows = (settings.widgetRows[key] ?: 2).coerceIn(1, 4)
        val heightDp = (rows * 88).dp
        val info = remember(hostedId) { vm.repo?.widgets?.info(hostedId) }
        val ink = LocalVInk.current
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (info != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combinedClickable(onClick = {}, onLongClick = onLongPress),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = info.label.uppercase(),
                        style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = info.app,
                        style = VText.secondary.copy(color = ink.copy(alpha = 0.5f)),
                    )
                }
            }
            VOutlined(
                radius = VRadius.card,
                outline = VOutline.dashed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .combinedClickable(onClick = {}, onLongClick = onLongPress),
            ) {
                AndroidView(
                    factory = {
                        vm.repo?.widgets?.createHostView(hostedId, 340f, heightDp.value)
                            ?: android.view.View(it)
                    },
                    update = { view ->
                        vm.repo?.widgets?.updateWidgetBounds(view, 340f, heightDp.value)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun NowPlayingCard(vm: CarinoViewModel) {
    val ink = LocalVInk.current
    val media by vm.media.collectAsState()
    val notifAccess by vm.notificationAccess.collectAsState()
    var liveNow by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(media?.playing) {
        while (media?.playing == true) {
            liveNow = System.currentTimeMillis()
            delay(1000L)
        }
    }

    Column(modifier = Modifier.padding(VSpace.cardPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "NOW PLAYING",
                style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)),
                modifier = Modifier.weight(1f),
            )
            media?.let { m ->
                if (m.app.isNotBlank()) {
                    Text(
                        text = m.app,
                        style = VText.secondary.copy(color = ink.copy(alpha = 0.6f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (!notifAccess) {
            Text(
                text = "Allow notification access to see and control media here.",
                style = VText.body.copy(color = ink.copy(alpha = 0.72f)),
            )
            Spacer(Modifier.height(10.dp))
            VPillButton(
                label = "Allow access",
                onClick = { vm.repo?.requestNotificationAccess() },
            )
        } else if (media == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VIconView(VIcon.music, size = 18.dp, color = ink.copy(alpha = 0.55f))
                Text(
                    text = "Nothing playing",
                    style = VText.body.copy(color = ink.copy(alpha = 0.62f)),
                )
            }
        } else {
            val m = media!!
            Column(modifier = Modifier.fillMaxWidth().clickable { vm.repo?.mediaOpen() }) {
                Text(
                    text = m.title,
                    style = VText.doto(20f, weight = 700).copy(color = ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (m.artist.isNotBlank()) {
                    Text(
                        text = m.artist,
                        style = VText.secondary.copy(color = ink.copy(alpha = 0.68f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (m.durationMs > 0 && m.canSeek) {
                Spacer(Modifier.height(8.dp))
                val pos = m.positionAt(liveNow).toFloat() / m.durationMs.toFloat()
                VSlider(
                    value = pos.coerceIn(0f, 1f),
                    onValueChange = { frac ->
                        vm.repo?.mediaAction("seek", (frac * m.durationMs).toLong())
                    },
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VIconButton(
                    icon = VIcon.previous,
                    contentDescription = "Previous",
                    onClick = { vm.repo?.mediaAction("previous") },
                )
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(ink)
                        .clickable { vm.repo?.mediaAction(if (m.playing) "pause" else "play") },
                    contentAlignment = Alignment.Center,
                ) {
                    VIconView(
                        icon = if (m.playing) VIcon.pause else VIcon.play,
                        size = 18.dp,
                        color = VColors.onColor(ink),
                    )
                }
                VIconButton(
                    icon = VIcon.next,
                    contentDescription = "Next",
                    onClick = { vm.repo?.mediaAction("next") },
                )
            }
        }
    }
}

@Composable
private fun UpNextCard(vm: CarinoViewModel) {
    val ink = LocalVInk.current
    val upNext by vm.upNext.collectAsState()
    val hasCalendar by vm.hasCalendarAccess.collectAsState()
    val settings by vm.settings.collectAsState()
    val timeFmt = remember(settings.clock24) {
        SimpleDateFormat(if (settings.clock24) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    Column(modifier = Modifier.padding(VSpace.cardPadding)) {
        Text(
            text = "UP NEXT",
            style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)),
        )
        Spacer(Modifier.height(10.dp))

        if (upNext.alarm != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { vm.repo?.openNamedIntent("clock") },
            ) {
                VIconView(VIcon.alarm, size = 16.dp, color = ink.copy(alpha = 0.75f))
                Text(
                    text = "Alarm · ${timeFmt.format(Date(upNext.alarm!!))}",
                    style = VText.rowTitle.copy(color = ink),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!hasCalendar) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Show today's events",
                    style = VText.body.copy(color = ink.copy(alpha = 0.7f)),
                    modifier = Modifier.weight(1f),
                )
                VPillButton(
                    label = "Allow",
                    onClick = {
                        vm.repo?.requestPermission("calendar") {
                            vm.refreshSystemStatus()
                            vm.refreshDynamicData()
                        }
                    },
                )
            }
        } else if (upNext.events.isEmpty() && upNext.alarm == null) {
            Text(
                text = "Nothing else today",
                style = VText.body.copy(color = ink.copy(alpha = 0.62f)),
            )
        } else {
            upNext.events.forEach { ev ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.repo?.openNamedIntent("calendar") }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ev.title,
                        style = VText.rowTitle.copy(color = ink),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (ev.allDay) "All day" else timeFmt.format(Date(ev.begin)),
                        style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockCard(vm: CarinoViewModel) {
    val ink = LocalVInk.current
    val nowMs by vm.nowMs.collectAsState()
    val settings by vm.settings.collectAsState()
    val timeFmt = remember(settings.clock24) {
        SimpleDateFormat(if (settings.clock24) "HH:mm" else "h:mm", Locale.getDefault())
    }
    val dayFmt = remember { SimpleDateFormat("EEE, d MMM", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { vm.repo?.openNamedIntent("clock") }
            .padding(VSpace.cardPadding),
    ) {
        Text("CLOCK", style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)))
        Spacer(Modifier.height(8.dp))
        Text(
            text = timeFmt.format(Date(nowMs)),
            style = VText.doto(28f, weight = 700).copy(color = ink),
        )
        Text(
            text = dayFmt.format(Date(nowMs)),
            style = VText.secondary.copy(color = ink.copy(alpha = 0.68f)),
        )
    }
}

@Composable
private fun WeatherCard(
    vm: CarinoViewModel,
    onOpenWeatherSettings: () -> Unit,
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val weather by vm.weather.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenWeatherSettings() }
            .padding(VSpace.cardPadding),
    ) {
        Text("WEATHER", style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)))
        Spacer(Modifier.height(8.dp))
        if (!settings.weatherOn) {
            Text(
                text = "Off",
                style = VText.doto(24f, weight = 700).copy(color = ink),
            )
            Text(
                text = "Tap to set up",
                style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
            )
        } else if (weather == null) {
            Text("—°", style = VText.doto(28f, weight = 700).copy(color = ink))
            Text(
                text = settings.weatherPlace?.name ?: "Tap for place",
                style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
            )
        } else {
            val w = weather!!
            val unit = if (w.fahrenheit) "°F" else "°"
            Text(
                text = "${w.temp.roundToInt()}$unit",
                style = VText.doto(28f, weight = 700).copy(color = ink),
            )
            Text(
                text = listOf(WeatherService.describe(w.code), w.place)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = VText.secondary.copy(color = ink.copy(alpha = 0.68f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BatteryCard(vm: CarinoViewModel) {
    val ink = LocalVInk.current
    val battery by vm.battery.collectAsState()
    val level = battery?.level ?: 0
    val charging = battery?.charging ?: false
    val filledSegments = ((level + 5) / 10).coerceIn(0, 10)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(VSpace.cardPadding),
    ) {
        Text("BATTERY", style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)))
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (charging) "$level% ⚡" else "$level%",
            style = VText.doto(28f, weight = 700).copy(color = ink),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (i in 0 until 10) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < filledSegments) ink else ink.copy(alpha = 0.16f)),
                )
            }
        }
    }
}

@Composable
private fun CalendarCard(vm: CarinoViewModel) {
    val ink = LocalVInk.current
    val nowMs by vm.nowMs.collectAsState()
    val cal = remember(nowMs / 60000L) { Calendar.getInstance().apply { timeInMillis = nowMs } }
    val monthTitle = remember(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR)) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = remember(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR)) {
        (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            .get(Calendar.DAY_OF_WEEK) - 1
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { vm.repo?.openNamedIntent("calendar") }
            .padding(VSpace.cardPadding),
    ) {
        Text(monthTitle.uppercase(), style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)))
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    text = d,
                    style = VText.chip.copy(color = ink.copy(alpha = 0.5f)),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val totalCells = firstDow + daysInMonth
        val rows = (totalCells + 6) / 7
        for (r in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - firstDow + 1
                    val valid = day in 1..daysInMonth
                    val isToday = valid && day == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (valid) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) ink else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = VText.chip.copy(
                                        color = if (isToday) VColors.onColor(ink) else ink.copy(alpha = 0.85f),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesCard(vm: CarinoViewModel) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    var note by remember(settings.note) { mutableStateOf(settings.note) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(VSpace.cardPadding),
    ) {
        Text("NOTES", style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)))
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = note,
            onValueChange = { next ->
                note = next
                vm.updateSettings { it.copy(note = next) }
            },
            textStyle = VText.body.copy(color = ink),
            cursorBrush = SolidColor(ink),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("widget_notes_input"),
            decorationBox = { inner ->
                if (note.isEmpty()) {
                    Text(
                        text = "Jot something down…",
                        style = VText.body.copy(color = ink.copy(alpha = 0.42f)),
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun WidgetMenuSheet(
    key: String,
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val list = settings.widgets
    val idx = list.indexOf(key)
    val hostedId = WidgetKey.hostedId(key)
    val builtin = WidgetKey.builtin(key)
    val info = remember(hostedId) { hostedId?.let { vm.repo?.widgets?.info(it) } }
    val title = builtin?.label() ?: info?.label ?: "Widget"

    CarinoSheet(onDismiss = onDismiss) {
        Text(text = title, style = VText.doto(24f, weight = 700).copy(color = ink))
        Spacer(Modifier.height(14.dp))

        if (hostedId != null) {
            val currentRows = (settings.widgetRows[key] ?: 2).coerceIn(1, 4)
            Text("Height", style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)))
            Spacer(Modifier.height(8.dp))
            VSegmented(
                options = listOf(1 to "S", 2 to "M", 3 to "L", 4 to "XL"),
                selected = currentRows,
                onSelected = { rows ->
                    vm.updateSettings { s ->
                        s.copy(widgetRows = s.widgetRows + (key to rows))
                    }
                },
                compact = true,
            )
            Spacer(Modifier.height(14.dp))
        }

        VListCard {
            if (idx > 0) {
                VActionRow(
                    title = "Move up",
                    icon = VIcon.up,
                    onClick = {
                        val copy = list.toMutableList()
                        copy[idx] = copy[idx - 1]
                        copy[idx - 1] = key
                        vm.updateSettings { it.copy(widgets = copy) }
                        onDismiss()
                    },
                )
                VHairlineDivider()
            }
            if (idx in 0 until list.lastIndex) {
                VActionRow(
                    title = "Move down",
                    icon = VIcon.downArrow,
                    onClick = {
                        val copy = list.toMutableList()
                        copy[idx] = copy[idx + 1]
                        copy[idx + 1] = key
                        vm.updateSettings { it.copy(widgets = copy) }
                        onDismiss()
                    },
                )
                VHairlineDivider()
            }
            if (hostedId != null && info?.configurable == true) {
                VActionRow(
                    title = "Configure",
                    icon = VIcon.gear,
                    onClick = {
                        vm.repo?.widgets?.configure(hostedId)
                        onDismiss()
                    },
                )
                VHairlineDivider()
            }
            VActionRow(
                title = "Remove",
                icon = VIcon.trash,
                onClick = {
                    if (hostedId != null) vm.repo?.widgets?.remove(hostedId)
                    vm.updateSettings { s ->
                        s.copy(
                            widgets = s.widgets - key,
                            widgetRows = s.widgetRows - key,
                        )
                    }
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun AddWidgetSheet(
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val missingBuiltins = remember(settings.widgets) {
        BuiltinWidget.entries.filter { it.name !in settings.widgets }
    }
    var providers by remember { mutableStateOf<List<WidgetProviderInfo>?>(null) }

    LaunchedEffect(Unit) {
        providers = vm.repo?.widgets?.providers().orEmpty()
    }

    val grouped = remember(providers) {
        providers.orEmpty().groupBy { it.app }
    }

    CarinoSheet(onDismiss = onDismiss) {
        Text("Add widget", style = VText.doto(24f, weight = 700).copy(color = ink))
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (missingBuiltins.isNotEmpty()) {
                item {
                    VSectionTitle("Carbon")
                    VListCard {
                        missingBuiltins.forEachIndexed { idx, w ->
                            if (idx > 0) VHairlineDivider()
                            VActionRow(
                                title = w.label(),
                                icon = VIcon.plus,
                                onClick = {
                                    vm.updateSettings { s -> s.copy(widgets = s.widgets + w.name) }
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }

            if (providers == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        DotLoader()
                    }
                }
            } else {
                grouped.forEach { (appName, list) ->
                    item(key = appName) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            VSectionTitle(appName)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(list, key = { "${it.provider}_${it.user}" }) { provider ->
                                    WidgetProviderCard(
                                        provider = provider,
                                        vm = vm,
                                        onClick = {
                                            vm.repo?.widgets?.add(provider) { id ->
                                                if (id != null) {
                                                    val key = WidgetKey.hosted(id)
                                                    val rows = (provider.minHeight / 84).coerceIn(1, 4)
                                                    vm.updateSettings { s ->
                                                        s.copy(
                                                            widgets = s.widgets + key,
                                                            widgetRows = s.widgetRows + (key to rows),
                                                        )
                                                    }
                                                }
                                            }
                                            onDismiss()
                                        },
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
private fun WidgetProviderCard(
    provider: WidgetProviderInfo,
    vm: CarinoViewModel,
    onClick: () -> Unit,
) {
    val ink = LocalVInk.current
    val preview by produceState<Bitmap?>(initialValue = null, provider.provider, provider.user) {
        value = vm.repo?.widgets?.preview(provider.provider, provider.user)
    }

    VOutlined(
        radius = 16.dp,
        outline = VOutline.dashed,
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview!!.asImageBitmap(),
                        contentDescription = provider.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = provider.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = VText.inter(12f, weight = 500).copy(color = ink),
            )
            Text(
                text = "${provider.minWidth}×${provider.minHeight} dp",
                style = VText.secondary.copy(color = ink.copy(alpha = 0.6f)),
            )
        }
    }
}
