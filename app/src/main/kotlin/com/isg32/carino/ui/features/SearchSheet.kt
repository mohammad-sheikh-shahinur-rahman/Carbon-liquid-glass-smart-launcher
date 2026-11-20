package com.isg32.carino.ui.features

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.ContactHit
import com.isg32.carino.data.SearchMode
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.common.CarinoBackdrop
import com.isg32.carino.ui.components.VActionRow
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VIconButton
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.components.VSectionTitle
import com.isg32.carino.ui.components.VSegmented
import com.isg32.carino.ui.components.VSurface
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import java.net.URLEncoder

data class SystemSettingEntry(
    val title: String,
    val action: String,
    val keywords: String,
    val icon: VIcon,
)

object SystemSettingsCatalog {
    val entries = listOf(
        SystemSettingEntry("Wi-Fi", Settings.ACTION_WIFI_SETTINGS, "network internet wireless", VIcon.wifi),
        SystemSettingEntry("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS, "devices pair headphones", VIcon.bluetooth),
        SystemSettingEntry("Mobile network", Settings.ACTION_NETWORK_OPERATOR_SETTINGS, "cellular sim 5g lte", VIcon.internet),
        SystemSettingEntry("Sound & vibration", Settings.ACTION_SOUND_SETTINGS, "volume ringtone media haptics", VIcon.volume),
        SystemSettingEntry("Display", Settings.ACTION_DISPLAY_SETTINGS, "brightness dark theme screen timeout", VIcon.image),
        SystemSettingEntry("Wallpaper & style", Settings.ACTION_DISPLAY_SETTINGS, "theme background", VIcon.image),
        SystemSettingEntry("Notifications", Settings.ACTION_NOTIFICATION_ASSISTANT_SETTINGS, "alerts badges dnd", VIcon.bell),
        SystemSettingEntry("Apps", Settings.ACTION_APPLICATION_SETTINGS, "installed permissions default", VIcon.apps),
        SystemSettingEntry("Battery", Settings.ACTION_BATTERY_SAVER_SETTINGS, "power usage charging", VIcon.battery),
        SystemSettingEntry("Storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS, "space files memory", VIcon.cached),
        SystemSettingEntry("Security", Settings.ACTION_SECURITY_SETTINGS, "fingerprint lock screen pin", VIcon.lock),
        SystemSettingEntry("Privacy", Settings.ACTION_PRIVACY_SETTINGS, "permissions camera mic", VIcon.hide),
        SystemSettingEntry("Location", Settings.ACTION_LOCATION_SOURCE_SETTINGS, "gps maps place", VIcon.globe),
        SystemSettingEntry("Accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS, "talkback magnification", VIcon.info),
        SystemSettingEntry("Date & time", Settings.ACTION_DATE_SETTINGS, "clock timezone", VIcon.clock),
        SystemSettingEntry("Language & input", Settings.ACTION_LOCALE_SETTINGS, "keyboard speech", VIcon.edit),
        SystemSettingEntry("NFC & payments", Settings.ACTION_NFC_SETTINGS, "tap pay contactless", VIcon.sync),
        SystemSettingEntry("Airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS, "flight offline", VIcon.offline),
        SystemSettingEntry("Default apps", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, "browser home launcher", VIcon.home),
        SystemSettingEntry("Home settings", Settings.ACTION_HOME_SETTINGS, "launcher", VIcon.home2),
        SystemSettingEntry("About phone", Settings.ACTION_DEVICE_INFO_SETTINGS, "android version model", VIcon.info),
    )
}

data class EssentialEntry(
    val id: String,
    val label: String,
    val icon: VIcon,
    val isPanel: Boolean,
)

object EssentialsCatalog {
    val all = listOf(
        EssentialEntry("wifi", "Wi-Fi", VIcon.wifi, isPanel = true),
        EssentialEntry("internet", "Internet", VIcon.internet, isPanel = true),
        EssentialEntry("bluetooth", "Bluetooth", VIcon.bluetooth, isPanel = true),
        EssentialEntry("torch", "Torch", VIcon.torch, isPanel = false),
        EssentialEntry("dnd", "Do Not Disturb", VIcon.moon, isPanel = true),
        EssentialEntry("volume", "Volume", VIcon.volume, isPanel = true),
        EssentialEntry("nfc", "NFC", VIcon.sync, isPanel = true),
        EssentialEntry("timer", "Timer", VIcon.timer, isPanel = false),
        EssentialEntry("alarm", "Alarm", VIcon.alarm, isPanel = false),
        EssentialEntry("camera", "Camera", VIcon.camera, isPanel = false),
        EssentialEntry("calendar", "Calendar", VIcon.calendar, isPanel = false),
        EssentialEntry("settings", "Settings", VIcon.gear, isPanel = false),
    )

    fun byId(id: String): EssentialEntry? = all.firstOrNull { it.id == id }
}

@Composable
fun SearchSheet(
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    onEditEssentials: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    val allApps by vm.apps.collectAsState()
    val visibleApps by vm.visibleApps.collectAsState()
    val torchOn by vm.torchOn.collectAsState()

    var mode by remember { mutableStateOf(settings.searchOpensOn) }
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactHit>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(mode) {
        if (mode == SearchMode.apps) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    LaunchedEffect(query, settings.searchContacts) {
        contacts = if (settings.searchContacts && query.trim().length >= 2) {
            vm.repo?.searchContacts(query.trim()).orEmpty()
        } else {
            emptyList()
        }
    }

    val matchingApps = remember(query, visibleApps, allApps, settings.hiddenFindByName, settings.hidden) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            visibleApps.take(12)
        } else {
            val base = visibleApps.filter { it.label.lowercase().contains(q) }
            val hiddenExact = if (settings.hiddenFindByName) {
                allApps.filter { it.key in settings.hidden && it.label.lowercase() == q }
            } else {
                emptyList()
            }
            (hiddenExact + base).distinctBy { it.key }.take(14)
        }
    }

    val matchingSettings = remember(query, settings.searchSettings) {
        val q = query.trim().lowercase()
        if (!settings.searchSettings || q.length < 2) {
            emptyList()
        } else {
            SystemSettingsCatalog.entries.filter {
                it.title.lowercase().contains(q) || it.keywords.contains(q)
            }.take(4)
        }
    }

    fun searchWeb(q: String) {
        val encoded = URLEncoder.encode(q, "UTF-8")
        val url = when (settings.webEngine) {
            "Google" -> "https://www.google.com/search?q=$encoded"
            "Brave" -> "https://search.brave.com/search?q=$encoded"
            "Startpage" -> "https://www.startpage.com/do/search?q=$encoded"
            else -> "https://duckduckgo.com/?q=$encoded"
        }
        vm.repo?.openUrl(url)
        onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CarinoBackdrop(
            vm = vm,
            wallpaper = layout.wallpaper,
            dim = layout.dim,
            blur = layout.blur,
            veiled = true,
            soft = true,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = VSpace.gutter, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VSegmented(
                    options = listOf(SearchMode.apps to "Apps", SearchMode.essentials to "Essentials"),
                    selected = mode,
                    onSelected = { mode = it },
                    modifier = Modifier.weight(1f),
                    compact = true,
                    tagPrefix = "search_mode",
                )
                VIconButton(
                    icon = VIcon.close,
                    contentDescription = "Close search",
                    onClick = onDismiss,
                    outlined = true,
                    tag = "search_close",
                )
            }

            Spacer(Modifier.height(14.dp))

            if (mode == SearchMode.apps) {
                val pal = com.isg32.carino.ui.theme.LocalVPalette.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            VIconView(VIcon.search, size = 18.dp, strokeWidth = 1.9f, color = ink.copy(alpha = 0.85f))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = VText.inter(16f, weight = 500).copy(color = ink),
                                cursorBrush = SolidColor(ink),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        if (settings.enterLaunches && matchingApps.isNotEmpty()) {
                                            vm.launchApp(matchingApps.first().key)
                                            onDismiss()
                                        } else if (settings.searchWeb && query.isNotBlank()) {
                                            searchWeb(query.trim())
                                        }
                                    },
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("search_input"),
                                decorationBox = { inner ->
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "App Library",
                                            style = VText.inter(16f, weight = 500).copy(color = ink.copy(alpha = 0.65f)),
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
                    Text(
                        text = "Cancel",
                        style = VText.inter(15f, weight = 600).copy(color = ink),
                        modifier = Modifier
                            .clip(RoundedCornerShape(VRadius.pill))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                            .testTag("search_cancel_text"),
                    )
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (matchingApps.isNotEmpty()) {
                        item {
                            VListCard {
                                matchingApps.forEachIndexed { idx, app ->
                                    if (idx > 0) VHairlineDivider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                vm.launchApp(app.key)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AppGlyph(app = app, vm = vm, size = 36.dp, style = layout.icons)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = app.label,
                                            style = VText.rowTitle.copy(color = ink),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        VIconButton(
                                            icon = VIcon.info,
                                            contentDescription = "App menu",
                                            onClick = { onAppMenu(app) },
                                            size = 36.dp,
                                            iconSize = 16.dp,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (contacts.isNotEmpty()) {
                        item {
                            VSectionTitle("Contacts")
                            VListCard {
                                contacts.forEachIndexed { idx, hit ->
                                    if (idx > 0) VHairlineDivider()
                                    VActionRow(
                                        title = hit.name,
                                        icon = VIcon.person,
                                        onClick = {
                                            vm.repo?.openContact(hit)
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (matchingSettings.isNotEmpty()) {
                        item {
                            VSectionTitle("System settings")
                            VListCard {
                                matchingSettings.forEachIndexed { idx, entry ->
                                    if (idx > 0) VHairlineDivider()
                                    VActionRow(
                                        title = entry.title,
                                        icon = entry.icon,
                                        onClick = {
                                            vm.repo?.openSettingAction(entry.action)
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (settings.searchWeb && query.isNotBlank()) {
                        item {
                            VListCard {
                                VActionRow(
                                    title = "Search ${settings.webEngine} for \"${query.trim()}\"",
                                    icon = VIcon.globe,
                                    onClick = { searchWeb(query.trim()) },
                                    tag = "search_web_action",
                                )
                            }
                        }
                    }
                }
            } else {
                EssentialsView(
                    ids = settings.essentials,
                    torchOn = torchOn,
                    onToggleTorch = { vm.repo?.toggleTorch() },
                    onOpenPanel = { name -> vm.repo?.openPanel(name) },
                    onOpenIntent = { name ->
                        vm.repo?.openNamedIntent(name)
                        onDismiss()
                    },
                    onEdit = onEditEssentials,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EssentialsView(
    ids: List<String>,
    torchOn: Boolean,
    onToggleTorch: () -> Unit,
    onOpenPanel: (String) -> Unit,
    onOpenIntent: (String) -> Unit,
    onEdit: () -> Unit,
) {
    val ink = LocalVInk.current
    val resolved = remember(ids) { ids.mapNotNull { EssentialsCatalog.byId(it) } }
    val toggles = resolved.filter { it.isPanel || it.id == "torch" }
    val tools = resolved.filter { !it.isPanel && it.id != "torch" }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Essentials",
                style = VText.title.copy(color = ink),
                modifier = Modifier.weight(1f),
            )
            VPillButton(
                label = "Edit",
                icon = VIcon.edit,
                onClick = onEdit,
                tag = "essentials_edit_button",
            )
        }
        Spacer(Modifier.height(18.dp))

        if (toggles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                toggles.forEach { entry ->
                    val active = entry.id == "torch" && torchOn
                    val bg = if (active) ink else ink.copy(alpha = 0.08f)
                    val fg = if (active) VColors.onColor(ink) else ink
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (entry.id == "torch") onToggleTorch() else onOpenPanel(entry.id)
                            }
                            .padding(6.dp)
                            .testTag("essential_toggle_${entry.id}"),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(VSpace.hairline, ink.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            VIconView(icon = entry.icon, size = 22.dp, color = fg)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = entry.label,
                            style = VText.secondary.copy(color = ink),
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        if (tools.isNotEmpty()) {
            VListCard {
                tools.forEachIndexed { idx, entry ->
                    if (idx > 0) VHairlineDivider()
                    VActionRow(
                        title = entry.label,
                        icon = entry.icon,
                        onClick = { onOpenIntent(entry.id) },
                        tag = "essential_tool_${entry.id}",
                    )
                }
            }
        }
    }
}
