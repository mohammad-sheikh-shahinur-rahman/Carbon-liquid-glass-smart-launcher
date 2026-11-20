package com.isg32.carino.ui.features

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.isg32.carino.BuildConfig
import com.isg32.carino.R
import com.isg32.carino.data.AppsView
import com.isg32.carino.data.CarinoPrefs
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.GestureAction
import com.isg32.carino.data.SearchMode
import com.isg32.carino.data.WeatherPlace
import com.isg32.carino.data.WeatherService
import com.isg32.carino.platform.CrashFiles
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.common.CarinoBackdrop
import com.isg32.carino.ui.components.CarinoSheet
import com.isg32.carino.ui.components.ConfirmDialog
import com.isg32.carino.ui.components.DotLoader
import com.isg32.carino.ui.components.VActionRow
import com.isg32.carino.ui.components.VButton
import com.isg32.carino.ui.components.VDestinationRow
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VListRow
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.components.VScrollPage
import com.isg32.carino.ui.components.VSectionFootnote
import com.isg32.carino.ui.components.VSectionTitle
import com.isg32.carino.ui.components.VSegmented
import com.isg32.carino.ui.components.VSurface
import com.isg32.carino.ui.components.VSwitch
import com.isg32.carino.ui.components.VTitleHeader
import com.isg32.carino.ui.components.VToggleRow
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SettingsScreen {
    Main,
    HomeGestures,
    AppsSearch,
    Weather,
    HiddenApps,
    Backup,
    About,
    Licences,
}

@Composable
fun SettingsHost(
    vm: CarinoViewModel,
    initialRoute: SettingsScreen = SettingsScreen.Main,
    onClose: () -> Unit,
    onOpenCustomise: () -> Unit,
) {
    var route by remember(initialRoute) { mutableStateOf(initialRoute) }
    val layout by vm.activeLayout.collectAsState()

    BackHandler {
        if (route == SettingsScreen.Licences) {
            route = SettingsScreen.About
        } else if (route != SettingsScreen.Main && initialRoute == SettingsScreen.Main) {
            route = SettingsScreen.Main
        } else {
            onClose()
        }
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

        when (route) {
            SettingsScreen.Main -> SettingsMainPage(
                vm = vm,
                onBack = onClose,
                onNavigate = { route = it },
                onOpenCustomise = onOpenCustomise,
            )
            SettingsScreen.HomeGestures -> HomeGesturesPage(
                vm = vm,
                onBack = { route = SettingsScreen.Main },
            )
            SettingsScreen.AppsSearch -> AppsSearchPage(
                vm = vm,
                onBack = { route = SettingsScreen.Main },
            )
            SettingsScreen.Weather -> WeatherSettingsPage(
                vm = vm,
                onBack = {
                    if (initialRoute == SettingsScreen.Weather) onClose()
                    else route = SettingsScreen.Main
                },
            )
            SettingsScreen.HiddenApps -> HiddenAppsPage(
                vm = vm,
                onBack = {
                    if (initialRoute == SettingsScreen.HiddenApps) onClose()
                    else route = SettingsScreen.Main
                },
            )
            SettingsScreen.Backup -> BackupPage(
                vm = vm,
                onBack = { route = SettingsScreen.Main },
            )
            SettingsScreen.About -> AboutPage(
                vm = vm,
                onBack = { route = SettingsScreen.Main },
                onLicences = { route = SettingsScreen.Licences },
            )
            SettingsScreen.Licences -> LicencesPage(
                onBack = { route = SettingsScreen.About },
            )
        }
    }
}

@Composable
private fun SettingsMainPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
    onNavigate: (SettingsScreen) -> Unit,
    onOpenCustomise: () -> Unit,
) {
    val isDefaultHome by vm.isDefaultHome.collectAsState()
    val notifAccess by vm.notificationAccess.collectAsState()
    val settings by vm.settings.collectAsState()

    VScrollPage {
        VTitleHeader(title = "Settings", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VListCard {
            VListRow(
                title = "Default home app",
                subtitle = if (isDefaultHome) "Carbon is your home screen." else "Tap to set Carbon as default.",
                leading = { VIconView(VIcon.home, size = 20.dp) },
                trailing = {
                    if (isDefaultHome) {
                        VIconView(VIcon.check, size = 18.dp)
                    } else {
                        VPillButton(
                            label = "Set",
                            onClick = { vm.repo?.requestDefaultHome() },
                            tag = "settings_default_home",
                        )
                    }
                },
                onClick = { if (!isDefaultHome) vm.repo?.requestDefaultHome() },
            )
            VHairlineDivider()
            VDestinationRow(
                title = "Customise look & layouts",
                icon = VIcon.edit,
                onClick = {
                    onBack()
                    onOpenCustomise()
                },
                tag = "settings_customise",
            )
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Launcher")
        VListCard {
            VDestinationRow(
                title = "Home & gestures",
                icon = VIcon.home2,
                onClick = { onNavigate(SettingsScreen.HomeGestures) },
                tag = "settings_home_gestures",
            )
            VHairlineDivider()
            VDestinationRow(
                title = "Apps & search",
                icon = VIcon.search,
                onClick = { onNavigate(SettingsScreen.AppsSearch) },
                tag = "settings_apps_search",
            )
            VHairlineDivider()
            VDestinationRow(
                title = "Weather",
                value = if (settings.weatherOn) settings.weatherPlace?.name ?: "Here" else "Off",
                icon = VIcon.cloud,
                onClick = { onNavigate(SettingsScreen.Weather) },
                tag = "settings_weather",
            )
            VHairlineDivider()
            VToggleRow(
                title = "Notification dots",
                subtitle = if (notifAccess) "Shows a dot on apps with unread notifications." else "Needs notification access.",
                icon = VIcon.bell,
                checked = settings.dots && notifAccess,
                onCheckedChange = { want ->
                    if (want && !notifAccess) {
                        vm.repo?.requestNotificationAccess()
                    } else {
                        vm.updateSettings { it.copy(dots = want) }
                    }
                },
                tag = "settings_dots",
            )
            VHairlineDivider()
            VDestinationRow(
                title = "Hidden apps",
                value = if (settings.hidden.isEmpty()) "None" else settings.hidden.size.toString(),
                icon = VIcon.hide,
                onClick = { onNavigate(SettingsScreen.HiddenApps) },
                tag = "settings_hidden_apps",
            )
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Data & app")
        VListCard {
            VDestinationRow(
                title = "Backup & reset",
                icon = VIcon.copy,
                onClick = { onNavigate(SettingsScreen.Backup) },
                tag = "settings_backup",
            )
            VHairlineDivider()
            VDestinationRow(
                title = "About Carbon",
                value = BuildConfig.VERSION_NAME,
                icon = VIcon.info,
                onClick = { onNavigate(SettingsScreen.About) },
                tag = "settings_about",
            )
        }
    }
}

private fun GestureAction.label(): String = when (this) {
    GestureAction.search -> "Search"
    GestureAction.notifications -> "Notifications"
    GestureAction.customise -> "Customise"
    GestureAction.apps -> "Apps"
    GestureAction.lock -> "Lock screen"
    GestureAction.nothing -> "Nothing"
}

@Composable
private fun HomeGesturesPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
) {
    val settings by vm.settings.collectAsState()
    val hasCalendar by vm.hasCalendarAccess.collectAsState()
    val lockAccess by vm.lockAccess.collectAsState()
    var pickingGesture by remember { mutableStateOf<String?>(null) }

    VScrollPage {
        VTitleHeader(title = "Home & gestures", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VSectionTitle("Clock & glance")
        VListCard {
            VToggleRow(
                title = "24-hour clock",
                checked = settings.clock24,
                onCheckedChange = { b -> vm.updateSettings { it.copy(clock24 = b) } },
            )
            VHairlineDivider()
            VToggleRow(
                title = "Show date",
                checked = settings.showDate,
                onCheckedChange = { b -> vm.updateSettings { it.copy(showDate = b) } },
            )
            VHairlineDivider()
            VToggleRow(
                title = "Show battery",
                checked = settings.showBattery,
                onCheckedChange = { b -> vm.updateSettings { it.copy(showBattery = b) } },
            )
            VHairlineDivider()
            VToggleRow(
                title = "Next calendar event",
                subtitle = if (hasCalendar) "Shows your next timed event under the clock." else "Needs calendar permission.",
                checked = settings.showNextEvent && hasCalendar,
                onCheckedChange = { want ->
                    if (want && !hasCalendar) {
                        vm.repo?.requestPermission("calendar") { granted ->
                            vm.refreshSystemStatus()
                            if (granted) vm.updateSettings { it.copy(showNextEvent = true) }
                        }
                    } else {
                        vm.updateSettings { it.copy(showNextEvent = want) }
                    }
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Gestures")
        VListCard {
            VDestinationRow(
                title = "Swipe up",
                value = settings.swipeUp.label(),
                icon = VIcon.swipeUp,
                onClick = { pickingGesture = "up" },
            )
            VHairlineDivider()
            VDestinationRow(
                title = "Swipe down",
                value = settings.swipeDown.label(),
                icon = VIcon.down,
                onClick = { pickingGesture = "down" },
            )
            VHairlineDivider()
            VDestinationRow(
                title = "Double-tap",
                value = settings.doubleTap.label(),
                icon = VIcon.lock,
                onClick = { pickingGesture = "double" },
            )
            VHairlineDivider()
            VListRow(
                title = "Lock screen accessibility",
                subtitle = if (lockAccess) "Enabled (keeps fingerprint unlock)." else "Required for Lock screen action.",
                trailing = {
                    if (lockAccess) VIconView(VIcon.check, size = 18.dp)
                    else VPillButton(label = "Enable", onClick = { vm.repo?.requestLockAccess() })
                },
            )
        }
    }

    pickingGesture?.let { which ->
        val ink = LocalVInk.current
        val current = when (which) {
            "up" -> settings.swipeUp
            "down" -> settings.swipeDown
            else -> settings.doubleTap
        }
        CarinoSheet(onDismiss = { pickingGesture = null }) {
            Text(
                text = when (which) {
                    "up" -> "Swipe up"
                    "down" -> "Swipe down"
                    else -> "Double-tap"
                },
                style = VText.doto(24f, weight = 700).copy(color = ink),
            )
            Spacer(Modifier.height(12.dp))
            VListCard {
                GestureAction.entries.forEachIndexed { idx, action ->
                    if (idx > 0) VHairlineDivider()
                    VListRow(
                        title = action.label(),
                        trailing = {
                            if (action == current) VIconView(VIcon.check, size = 18.dp)
                        },
                        onClick = {
                            vm.updateSettings { s ->
                                when (which) {
                                    "up" -> s.copy(swipeUp = action)
                                    "down" -> s.copy(swipeDown = action)
                                    else -> s.copy(doubleTap = action)
                                }
                            }
                            pickingGesture = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppsSearchPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val hasContacts by vm.hasContactsAccess.collectAsState()
    var pickEngine by remember { mutableStateOf(false) }

    VScrollPage {
        VTitleHeader(title = "Apps & search", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VSectionTitle("Apps drawer")
        VListCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Layout", style = VText.rowTitle.copy(color = ink))
                Spacer(Modifier.height(10.dp))
                VSegmented(
                    options = listOf(AppsView.list to "List", AppsView.grid to "Grid"),
                    selected = settings.appsView,
                    onSelected = { v -> vm.updateSettings { it.copy(appsView = v) } },
                    compact = true,
                )
            }
            VHairlineDivider()
            VToggleRow(
                title = "New app badges",
                checked = settings.newBadges,
                onCheckedChange = { b -> vm.updateSettings { it.copy(newBadges = b) } },
            )
            VHairlineDivider()
            VToggleRow(
                title = "A–Z scrubber haptics",
                checked = settings.scrubberHaptics,
                onCheckedChange = { b -> vm.updateSettings { it.copy(scrubberHaptics = b) } },
            )
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Search")
        VListCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Opens on", style = VText.rowTitle.copy(color = ink))
                Spacer(Modifier.height(10.dp))
                VSegmented(
                    options = listOf(SearchMode.apps to "Apps", SearchMode.essentials to "Essentials"),
                    selected = settings.searchOpensOn,
                    onSelected = { m -> vm.updateSettings { it.copy(searchOpensOn = m) } },
                    compact = true,
                )
            }
            VHairlineDivider()
            VToggleRow(
                title = "Enter launches top app",
                checked = settings.enterLaunches,
                onCheckedChange = { b -> vm.updateSettings { it.copy(enterLaunches = b) } },
            )
            VHairlineDivider()
            VToggleRow(
                title = "Search contacts",
                subtitle = if (hasContacts) "Matches names on your device." else "Needs contacts permission.",
                checked = settings.searchContacts && hasContacts,
                onCheckedChange = { want ->
                    if (want && !hasContacts) {
                        vm.repo?.requestPermission("contacts") { granted ->
                            vm.refreshSystemStatus()
                            if (granted) vm.updateSettings { it.copy(searchContacts = true) }
                        }
                    } else {
                        vm.updateSettings { it.copy(searchContacts = want) }
                    }
                },
            )
            VHairlineDivider()
            VToggleRow(
                title = "Search system settings",
                checked = settings.searchSettings,
                onCheckedChange = { b -> vm.updateSettings { it.copy(searchSettings = b) } },
            )
            VHairlineDivider()
            VToggleRow(
                title = "Web search",
                checked = settings.searchWeb,
                onCheckedChange = { b -> vm.updateSettings { it.copy(searchWeb = b) } },
            )
            if (settings.searchWeb) {
                VHairlineDivider()
                VDestinationRow(
                    title = "Search engine",
                    value = settings.webEngine,
                    onClick = { pickEngine = true },
                )
            }
        }
    }

    if (pickEngine) {
        CarinoSheet(onDismiss = { pickEngine = false }) {
            Text("Search engine", style = VText.doto(24f, weight = 700).copy(color = ink))
            Spacer(Modifier.height(12.dp))
            VListCard {
                listOf("DuckDuckGo", "Google", "Brave", "Startpage").forEachIndexed { idx, eng ->
                    if (idx > 0) VHairlineDivider()
                    VListRow(
                        title = eng,
                        trailing = { if (settings.webEngine == eng) VIconView(VIcon.check, size = 18.dp) },
                        onClick = {
                            vm.updateSettings { it.copy(webEngine = eng) }
                            pickEngine = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherSettingsPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
) {
    val ink = LocalVInk.current
    val settings by vm.settings.collectAsState()
    val weather by vm.weather.collectAsState()
    val hasLocation by vm.hasLocationAccess.collectAsState()
    val scope = rememberCoroutineScope()
    var showPlacePicker by remember { mutableStateOf(false) }

    VScrollPage {
        VTitleHeader(title = "Weather", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VListCard {
            VToggleRow(
                title = "Show weather",
                subtitle = "Uses Open-Meteo. Weather is the only thing Carbon fetches online.",
                checked = settings.weatherOn,
                onCheckedChange = { on -> vm.updateSettings { it.copy(weatherOn = on) } },
                tag = "weather_toggle",
            )
            if (settings.weatherOn) {
                VHairlineDivider()
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Unit", style = VText.rowTitle.copy(color = ink))
                    Spacer(Modifier.height(10.dp))
                    VSegmented(
                        options = listOf(false to "Celsius (°C)", true to "Fahrenheit (°F)"),
                        selected = settings.weatherFahrenheit,
                        onSelected = { f -> vm.updateSettings { it.copy(weatherFahrenheit = f) } },
                        compact = true,
                    )
                }
                VHairlineDivider()
                VDestinationRow(
                    title = "Place",
                    value = settings.weatherPlace?.name ?: if (hasLocation) "Here" else "Choose city",
                    onClick = { showPlacePicker = true },
                    tag = "weather_place_row",
                )
                VHairlineDivider()
                VActionRow(
                    title = "Refresh now",
                    subtitle = weather?.let { "Last reading: ${it.temp.roundToInt()}° ${WeatherService.describe(it.code)}" },
                    icon = VIcon.sync,
                    onClick = { scope.launch { vm.refreshWeather(force = true) } },
                )
            }
        }
    }

    if (showPlacePicker) {
        PlacePickerSheet(
            vm = vm,
            hasLocation = hasLocation,
            onDismiss = { showPlacePicker = false },
        )
    }
}

@Composable
private fun PlacePickerSheet(
    vm: CarinoViewModel,
    hasLocation: Boolean,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(350L)
        results = runCatching { WeatherService.searchPlaces(query) }.getOrDefault(emptyList())
        searching = false
    }

    CarinoSheet(onDismiss = onDismiss) {
        Text("Choose place", style = VText.doto(24f, weight = 700).copy(color = ink))
        Spacer(Modifier.height(12.dp))

        VListCard {
            VActionRow(
                title = "Use approximate device location",
                subtitle = if (hasLocation) "Uses the phone's last known place." else "Asks for coarse location only (no GPS).",
                icon = VIcon.globe,
                onClick = {
                    if (hasLocation) {
                        vm.updateSettings { it.copy(weatherPlace = null) }
                        onDismiss()
                    } else {
                        vm.repo?.requestPermission("location") { granted ->
                            vm.refreshSystemStatus()
                            if (granted) {
                                vm.updateSettings { it.copy(weatherPlace = null) }
                                onDismiss()
                            }
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        VSurface(radius = VRadius.pill) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VIconView(VIcon.search, size = 18.dp, color = ink.copy(alpha = 0.6f))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = VText.body.copy(color = ink),
                    cursorBrush = SolidColor(ink),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search city or town…", style = VText.body.copy(color = ink.copy(alpha = 0.48f)))
                        }
                        inner()
                    },
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (searching) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                DotLoader()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
            ) {
                items(results) { place ->
                    VListRow(
                        title = place.name,
                        subtitle = place.subtitle.takeIf { it.isNotBlank() },
                        onClick = {
                            vm.updateSettings { it.copy(weatherPlace = place) }
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HiddenAppsPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
) {
    val apps by vm.apps.collectAsState()
    val settings by vm.settings.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    var showSetPin by remember { mutableStateOf(false) }

    VScrollPage {
        VTitleHeader(title = "Hidden apps", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VListCard {
            VToggleRow(
                title = "Find hidden apps by exact name",
                subtitle = "Shows a hidden app in Search only when you type its full name.",
                checked = settings.hiddenFindByName,
                onCheckedChange = { b -> vm.updateSettings { it.copy(hiddenFindByName = b) } },
            )
            VHairlineDivider()
            VActionRow(
                title = if (settings.hasPasscode) "Change vault passcode" else "Set vault passcode",
                subtitle = "Two-finger spread on Home opens your hidden folder.",
                icon = VIcon.lock,
                onClick = { showSetPin = true },
            )
            if (settings.hasPasscode) {
                VHairlineDivider()
                VActionRow(
                    title = "Remove vault passcode",
                    icon = VIcon.trash,
                    onClick = { vm.updateSettings { it.copy(vaultHash = null, vaultSalt = null) } },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Apps")
        VListCard {
            apps.forEachIndexed { idx, app ->
                if (idx > 0) VHairlineDivider()
                val hidden = app.key in settings.hidden
                VListRow(
                    title = app.label,
                    leading = { AppGlyph(app = app, vm = vm, size = 34.dp, style = layout.icons) },
                    trailing = {
                        VSwitch(
                            checked = hidden,
                            onCheckedChange = { vm.toggleHidden(app.key) },
                        )
                    },
                    onClick = { vm.toggleHidden(app.key) },
                )
            }
        }
    }

    if (showSetPin) {
        PasscodeDialog(
            title = "Set 4-digit passcode",
            subtitle = "Protects your hidden apps folder.",
            onSuccess = { pin ->
                val salt = CarinoPrefs.newSalt()
                val hash = CarinoPrefs.hashPasscode(pin, salt)
                vm.updateSettings { it.copy(vaultHash = hash, vaultSalt = salt) }
                vm.showToast("Passcode saved")
                showSetPin = false
            },
            onDismiss = { showSetPin = false },
        )
    }
}

@Composable
private fun BackupPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    VScrollPage {
        VTitleHeader(title = "Backup & reset", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VListCard {
            VActionRow(
                title = "Copy backup to clipboard",
                subtitle = "Exports your layouts, settings, essentials, and notes as JSON.",
                icon = VIcon.copy,
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("Carbon backup", vm.exportBackupJson()))
                    vm.showToast("Backup copied to clipboard")
                },
                tag = "backup_export",
            )
            VHairlineDivider()
            VActionRow(
                title = "Restore from clipboard",
                subtitle = "Imports a JSON backup currently on your clipboard.",
                icon = VIcon.sync,
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                    if (clip != null && vm.importBackupJson(clip)) {
                        vm.showToast("Backup restored")
                    } else {
                        vm.showToast("No valid Carbon backup on clipboard")
                    }
                },
                tag = "backup_import",
            )
            VHairlineDivider()
            VActionRow(
                title = "Reset Carbon",
                subtitle = "Clears layouts and settings back to defaults.",
                icon = VIcon.trash,
                onClick = { confirmReset = true },
                tag = "backup_reset",
            )
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset everything?",
            body = "Your layouts, customisations, and notes will be cleared.",
            confirmLabel = "Reset",
            onConfirm = {
                confirmReset = false
                vm.resetAllData(context)
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun AboutPage(
    vm: CarinoViewModel,
    onBack: () -> Unit,
    onLicences: () -> Unit,
) {
    val ink = LocalVInk.current

    VScrollPage {
        VTitleHeader(title = "About Carbon", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.carino_mark),
                    contentDescription = "Carbon mark",
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text("Carbon", style = VText.doto(28f, weight = 700).copy(color = ink))
                Text(
                    "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · com.itamadersomajinc.Carbon",
                    style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Diagnostics & legal")
        VListCard {
            VDestinationRow(
                title = "Open-source licences",
                icon = VIcon.info,
                onClick = onLicences,
            )
            VHairlineDivider()
            VActionRow(
                title = "Create test crash report",
                icon = VIcon.spinner,
                onClick = {
                    val report = CrashFiles.buildManualReport(
                        RuntimeException("Test diagnostic report triggered from About Carbon"),
                    )
                    vm.triggerCrashReport(report)
                },
            )
        }
        VSectionFootnote("Carbon has no analytics, no telemetry, and no network access unless Weather is turned on.")
    }
}

@Composable
private fun LicencesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val ink = LocalVInk.current
    val dotoOfl = remember {
        context.resources.openRawResource(R.raw.ofl_doto).bufferedReader().use { it.readText() }
    }
    val interOfl = remember {
        context.resources.openRawResource(R.raw.ofl_inter).bufferedReader().use { it.readText() }
    }

    VScrollPage {
        VTitleHeader(title = "Licences", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        VSectionTitle("Doto Font (OFL)")
        VSurface(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = dotoOfl,
                style = VText.secondary.copy(color = ink.copy(alpha = 0.78f)),
                modifier = Modifier.padding(16.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        VSectionTitle("Inter Font (OFL)")
        VSurface(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = interOfl,
                style = VText.secondary.copy(color = ink.copy(alpha = 0.78f)),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
