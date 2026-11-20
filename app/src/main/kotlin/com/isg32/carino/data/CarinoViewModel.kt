package com.isg32.carino.data

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.isg32.carino.platform.CrashFiles
import com.isg32.carino.platform.CrashReport
import com.isg32.carino.platform.LauncherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

class CarinoViewModel(app: Application) : AndroidViewModel(app) {

    val prefs = CarinoPrefs(app)
    var repo: LauncherRepository? = null
        private set

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _appsLoaded = MutableStateFlow(false)
    val appsLoaded: StateFlow<Boolean> = _appsLoaded.asStateFlow()

    private val _settings = MutableStateFlow(prefs.readSettings())
    val settings: StateFlow<CarinoSettings> = _settings.asStateFlow()

    private val _layouts = MutableStateFlow(prefs.readLayouts())
    val layouts: StateFlow<LayoutsState> = _layouts.asStateFlow()

    val activeLayout: StateFlow<HomeLayout> = _layouts
        .map { it.active }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _layouts.value.active)

    val visibleApps: StateFlow<List<AppEntry>> = combine(_apps, _settings) { list, s ->
        list.filter { it.key !in s.hidden }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val appIndex: StateFlow<Map<String, AppEntry>> = _apps
        .map { list -> list.associateBy { it.key } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _isDefaultHome = MutableStateFlow(false)
    val isDefaultHome: StateFlow<Boolean> = _isDefaultHome.asStateFlow()

    private val _notificationAccess = MutableStateFlow(false)
    val notificationAccess: StateFlow<Boolean> = _notificationAccess.asStateFlow()

    private val _lockAccess = MutableStateFlow(false)
    val lockAccess: StateFlow<Boolean> = _lockAccess.asStateFlow()

    private val _hasCalendarAccess = MutableStateFlow(false)
    val hasCalendarAccess: StateFlow<Boolean> = _hasCalendarAccess.asStateFlow()

    private val _hasContactsAccess = MutableStateFlow(false)
    val hasContactsAccess: StateFlow<Boolean> = _hasContactsAccess.asStateFlow()

    private val _hasLocationAccess = MutableStateFlow(false)
    val hasLocationAccess: StateFlow<Boolean> = _hasLocationAccess.asStateFlow()

    private val _battery = MutableStateFlow<BatteryState?>(null)
    val battery: StateFlow<BatteryState?> = _battery.asStateFlow()

    private val _torchOn = MutableStateFlow(false)
    val torchOn: StateFlow<Boolean> = _torchOn.asStateFlow()

    private val _dots = MutableStateFlow<Map<String, Int>>(emptyMap())
    val dots: StateFlow<Map<String, Int>> = _dots.asStateFlow()

    private val _media = MutableStateFlow<MediaState?>(null)
    val media: StateFlow<MediaState?> = _media.asStateFlow()

    private val _weather = MutableStateFlow<Weather?>(null)
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    private val _upNext = MutableStateFlow(UpNext())
    val upNext: StateFlow<UpNext> = _upNext.asStateFlow()

    private val _nextEvent = MutableStateFlow<CalendarEvent?>(null)
    val nextEvent: StateFlow<CalendarEvent?> = _nextEvent.asStateFlow()

    private val _nowMs = MutableStateFlow(System.currentTimeMillis())
    val nowMs: StateFlow<Long> = _nowMs.asStateFlow()

    private val _opening = MutableStateFlow(false)
    val opening: StateFlow<Boolean> = _opening.asStateFlow()

    private val _crashReport = MutableStateFlow<CrashReport?>(null)
    val crashReport: StateFlow<CrashReport?> = _crashReport.asStateFlow()

    // Shell navigation state
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen.asStateFlow()

    private val _customising = MutableStateFlow(false)
    val customising: StateFlow<Boolean> = _customising.asStateFlow()

    private val _editHome = MutableStateFlow(false)
    val editHome: StateFlow<Boolean> = _editHome.asStateFlow()

    private val _settingsOpen = MutableStateFlow(false)
    val settingsOpen: StateFlow<Boolean> = _settingsOpen.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var lastSyncedSignature: String? = null
    private var wallpaperSyncJob: Job? = null
    private var minuteTickerJob: Job? = null

    private val iconMemoryCache = ConcurrentHashMap<String, Bitmap>()
    private val iconMissingCache = ConcurrentHashMap<String, Boolean>()

    init {
        viewModelScope.launch {
            _crashReport.value = CrashFiles.consumeLatestCrash(app)
        }
        startMinuteTicker()
    }

    fun attachRepository(repository: LauncherRepository) {
        if (repo === repository) return
        repo = repository

        viewModelScope.launch { repository.battery.collect { _battery.value = it } }
        viewModelScope.launch { repository.torchOn.collect { _torchOn.value = it } }
        viewModelScope.launch { repository.dots.collect { _dots.value = it } }
        viewModelScope.launch { repository.media.collect { _media.value = it } }
        viewModelScope.launch {
            repository.packagesChanged.collect {
                iconMemoryCache.clear()
                iconMissingCache.clear()
                refreshApps()
            }
        }
        viewModelScope.launch {
            repository.resumed.collect {
                _opening.value = false
                refreshSystemStatus()
                refreshDynamicData()
                scheduleWallpaperSync(immediate = false)
            }
        }
        viewModelScope.launch {
            repository.homePressed.collect {
                _opening.value = false
                _searchOpen.value = false
                _customising.value = false
                _editHome.value = false
                _settingsOpen.value = false
                _activeTab.value = 0
            }
        }

        // Load cached apps immediately, then refresh live list
        val cached = prefs.readAppsCache()
        if (cached != null) {
            _apps.value = cached
            _appsLoaded.value = true
            seedFirstLayoutIfNeeded(cached)
            viewModelScope.launch { refreshApps() }
        } else {
            viewModelScope.launch { loadInitialApps() }
        }

        refreshSystemStatus()
        refreshDynamicData()
    }

    private fun startMinuteTicker() {
        minuteTickerJob?.cancel()
        minuteTickerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                _nowMs.value = now
                refreshDynamicData()
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, 1)
                }
                delay((cal.timeInMillis - now).coerceAtLeast(1000L))
            }
        }
    }

    fun refreshSystemStatus() {
        val r = repo ?: return
        _isDefaultHome.value = r.isDefaultHome()
        _notificationAccess.value = r.hasNotificationAccess()
        _lockAccess.value = r.hasLockAccess()
        _hasCalendarAccess.value = r.hasPermission("calendar")
        _hasContactsAccess.value = r.hasPermission("contacts")
        _hasLocationAccess.value = r.hasPermission("location")
    }

    fun refreshDynamicData() {
        val r = repo ?: return
        viewModelScope.launch {
            _upNext.value = r.upNext()
            _nextEvent.value = if (_settings.value.showNextEvent) r.nextEvent() else null
            if (_settings.value.weatherOn) refreshWeather() else _weather.value = null
        }
    }

    private suspend fun loadInitialApps() {
        val r = repo ?: return
        var list = emptyList<AppEntry>()
        for (attempt in 0 until 8) {
            list = runCatching { r.listApps() }.getOrDefault(emptyList())
            if (list.isNotEmpty()) break
            delay(650L)
        }
        if (list.isNotEmpty()) {
            _apps.value = list
            prefs.writeAppsCache(list)
            seedFirstLayoutIfNeeded(list)
        }
        _appsLoaded.value = true
    }

    fun refreshApps() {
        val r = repo ?: return
        viewModelScope.launch {
            val list = runCatching { r.listApps() }.getOrDefault(emptyList())
            if (list.isNotEmpty()) {
                _apps.value = list
                _appsLoaded.value = true
                prefs.writeAppsCache(list)
                seedFirstLayoutIfNeeded(list)
            }
        }
    }

    suspend fun loadIconBitmap(key: String, mono: Boolean): Bitmap? {
        val cacheKey = "$key|$mono"
        iconMemoryCache[cacheKey]?.let { return it }
        if (iconMissingCache[cacheKey] == true) return null
        val bmp = repo?.loadIcon(key, 160, mono)
        if (bmp != null) {
            iconMemoryCache[cacheKey] = bmp
        } else {
            iconMissingCache[cacheKey] = true
        }
        return bmp
    }

    // region Settings & Layouts Mutations

    fun updateSettings(transform: (CarinoSettings) -> CarinoSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.writeSettings(next)
        if (!next.weatherOn) {
            _weather.value = null
        } else {
            viewModelScope.launch { refreshWeather() }
        }
        if (!next.showNextEvent) {
            _nextEvent.value = null
        } else {
            viewModelScope.launch { _nextEvent.value = repo?.nextEvent() }
        }
    }

    fun toggleHidden(key: String) = updateSettings { s ->
        val next = s.hidden.toMutableSet()
        if (!next.remove(key)) next.add(key)
        s.copy(hidden = next)
    }

    private fun saveLayouts(next: LayoutsState) {
        _layouts.value = next
        prefs.writeLayouts(next)
        scheduleWallpaperSync(immediate = false)
    }

    private fun seedFirstLayoutIfNeeded(apps: List<AppEntry>) {
        val st = _layouts.value
        if (st.seeded || apps.isEmpty()) return
        fun find(names: List<String>): String? {
            for (n in names) {
                apps.firstOrNull { it.label.equals(n, ignoreCase = true) }?.let { return it.key }
            }
            return null
        }
        val dock = listOf(
            find(listOf("phone", "dialer")),
            find(listOf("messages", "messaging", "sms")),
            find(listOf("chrome", "browser", "firefox", "brave", "internet")),
            find(listOf("camera")),
        )
        val used = dock.filterNotNull().toMutableSet()
        val favs = mutableListOf<String?>()
        val favCandidates = listOf(
            listOf("gmail", "mail", "email"),
            listOf("spotify", "youtube music", "music"),
            listOf("maps", "google maps"),
            listOf("calendar"),
            listOf("photos", "gallery"),
            listOf("clock"),
            listOf("files", "files by google", "file manager"),
            listOf("settings"),
        )
        for (names in favCandidates) {
            val k = find(names)
            if (k != null && used.add(k)) favs.add(k)
        }
        val first = st.layouts.first().copy(favourites = favs, dock = dock)
        saveLayouts(LayoutsState(listOf(first) + st.layouts.drop(1), st.activeId, seeded = true))
    }

    fun setActiveLayout(id: String) {
        val st = _layouts.value
        saveLayouts(st.copy(activeId = id))
    }

    fun updateLayout(layout: HomeLayout) {
        val st = _layouts.value
        saveLayouts(st.copy(layouts = st.layouts.map { if (it.id == layout.id) layout else it }))
    }

    fun updateActiveLayout(transform: (HomeLayout) -> HomeLayout) =
        updateLayout(transform(_layouts.value.active))

    fun addLayout(from: HomeLayout? = null): HomeLayout {
        val st = _layouts.value
        val id = "l${System.currentTimeMillis()}"
        val layout = if (from != null) {
            from.copy(id = id, name = "${from.name} copy")
        } else {
            HomeLayout(
                id = id,
                name = "Layout ${st.layouts.size + 1}",
                dock = st.active.dock,
            )
        }
        saveLayouts(st.copy(layouts = st.layouts + layout, activeId = id))
        return layout
    }

    fun deleteLayout(id: String) {
        val st = _layouts.value
        if (st.layouts.size < 2) return
        val rest = st.layouts.filter { it.id != id }
        val nextActive = if (st.activeId == id) rest.first().id else st.activeId
        saveLayouts(st.copy(layouts = rest, activeId = nextActive))
    }

    fun pinToHome(key: String): Boolean {
        val l = _layouts.value.active
        val grid = l.favGrid.toMutableList()
        if (key in grid || key in l.dockRow) return true
        val idx = grid.indexOf(null)
        if (idx < 0) return false
        grid[idx] = key
        updateLayout(l.copy(favourites = grid))
        return true
    }

    fun removeFromHome(key: String) {
        val l = _layouts.value.active
        updateLayout(
            l.copy(
                favourites = l.favGrid.map { if (it == key) null else it },
                dock = l.dockRow.map { if (it == key) null else it },
            ),
        )
    }

    fun setFavourite(index: Int, key: String?) {
        val grid = _layouts.value.active.favGrid.toMutableList()
        if (index !in grid.indices) return
        grid[index] = key
        updateActiveLayout { it.copy(favourites = grid) }
    }

    fun setDock(index: Int, key: String?) {
        val row = _layouts.value.active.dockRow.toMutableList()
        if (index !in row.indices) return
        row[index] = key
        updateActiveLayout { it.copy(dock = row) }
    }

    fun swapSlots(a: Int, b: Int) {
        val l = _layouts.value.active
        val all = (l.favGrid + l.dockRow).toMutableList()
        if (a !in all.indices || b !in all.indices) return
        val tmp = all[a]
        all[a] = all[b]
        all[b] = tmp
        updateLayout(
            l.copy(
                favourites = all.subList(0, l.favSlots),
                dock = all.subList(l.favSlots, all.size),
            ),
        )
    }

    // endregion

    // region Weather

    suspend fun refreshWeather(force: Boolean = false) {
        val s = _settings.value
        if (!s.weatherOn) {
            _weather.value = null
            return
        }
        val cached = prefs.readWeatherCache(s.weatherPlace, s.weatherFahrenheit)
        if (cached != null) _weather.value = cached
        val now = System.currentTimeMillis()
        if (!force && cached != null && now - cached.at < 30L * 60 * 1000) {
            return
        }
        try {
            val where = s.weatherPlace ?: repo?.lastLocation() ?: return
            val fresh = WeatherService.fetch(where, s.weatherFahrenheit)
            prefs.writeWeatherCache(s.weatherPlace, s.weatherFahrenheit, fresh)
            _weather.value = fresh
        } catch (_: Exception) {
            if (cached != null && now - cached.at < 6L * 60 * 60 * 1000) {
                _weather.value = cached
            }
        }
    }

    // endregion

    // region Navigation & Actions

    fun setTab(tab: Int) {
        _activeTab.value = tab.coerceIn(0, 2)
    }

    fun openSearch() {
        _searchOpen.value = true
    }

    fun closeSearch() {
        _searchOpen.value = false
    }

    fun openCustomise() {
        _editHome.value = false
        _customising.value = true
    }

    fun closeCustomise() {
        _customising.value = false
    }

    fun setEditHome(editing: Boolean) {
        _editHome.value = editing
    }

    fun openSettings() {
        _settingsOpen.value = true
    }

    fun closeSettings() {
        _settingsOpen.value = false
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun launchApp(key: String, bounds: Rect? = null): Boolean {
        val r = repo ?: return false
        _opening.value = true
        val ok = r.launch(key, bounds)
        if (!ok) _opening.value = false
        return ok
    }

    fun runGesture(action: GestureAction) {
        when (action) {
            GestureAction.search -> openSearch()
            GestureAction.notifications -> repo?.expandNotifications()
            GestureAction.customise -> openCustomise()
            GestureAction.apps -> setTab(1)
            GestureAction.lock -> {
                if (repo?.lockScreen() == false) {
                    showToast("Turn on Lock screen in Settings → Home & gestures")
                }
            }
            GestureAction.nothing -> Unit
        }
    }

    fun syncSystemWallpaperNow() = scheduleWallpaperSync(immediate = true)

    private fun scheduleWallpaperSync(immediate: Boolean) {
        val r = repo ?: return
        val layout = _layouts.value.active
        val (w, h) = r.screenPixels()
        val sig = "${layout.wallpaper.builtin}|${layout.wallpaper.photo}|${layout.dim}|${layout.blur}|$w|$h"
        if (!immediate && sig == lastSyncedSignature) return
        wallpaperSyncJob?.cancel()
        wallpaperSyncJob = viewModelScope.launch {
            if (!immediate) delay(900L)
            if (sig == lastSyncedSignature && !immediate) return@launch
            val ok = r.applySystemWallpaper(layout, "home")
            if (ok) lastSyncedSignature = sig
        }
    }

    fun triggerCrashReport(report: CrashReport) {
        _crashReport.value = report
    }

    fun dismissCrashReport() {
        _crashReport.value = null
    }

    fun exportBackupJson(): String = prefs.exportAll(_settings.value, _layouts.value)

    fun importBackupJson(raw: String): Boolean {
        val parsed = prefs.parseImport(raw) ?: return false
        _settings.value = parsed.settings
        prefs.writeSettings(parsed.settings)
        val layouts = if (parsed.layouts.layouts.isEmpty()) LayoutsState() else parsed.layouts
        saveLayouts(layouts)
        refreshDynamicData()
        return true
    }

    fun resetAllData(context: Context) {
        prefs.clearAll()
        _settings.value = CarinoSettings(onboarded = true)
        prefs.writeSettings(_settings.value)
        _layouts.value = LayoutsState()
        prefs.writeLayouts(_layouts.value)
        seedFirstLayoutIfNeeded(_apps.value)
        showToast("Reset complete")
    }

    // endregion
}
