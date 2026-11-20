package com.isg32.carino

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.platform.CrashFiles
import com.isg32.carino.platform.LauncherRepository
import com.isg32.carino.ui.common.CarinoBackdrop
import com.isg32.carino.ui.common.WallpaperView
import com.isg32.carino.ui.components.ToastHost
import com.isg32.carino.ui.components.VTabBar
import com.isg32.carino.ui.components.VTabItem
import com.isg32.carino.ui.features.AppMenuSheet
import com.isg32.carino.ui.features.AppPickerSheet
import com.isg32.carino.ui.features.AppsPage
import com.isg32.carino.ui.features.CrashOverlay
import com.isg32.carino.ui.features.CustomiseScreen
import com.isg32.carino.ui.features.EssentialsEditPage
import com.isg32.carino.ui.features.HiddenFolderSheet
import com.isg32.carino.ui.features.HomePage
import com.isg32.carino.ui.features.LoadingPage
import com.isg32.carino.ui.features.OnboardingFlow
import com.isg32.carino.ui.features.PasscodeDialog
import com.isg32.carino.ui.features.SearchSheet
import com.isg32.carino.ui.features.SettingsHost
import com.isg32.carino.ui.features.SettingsScreen
import com.isg32.carino.ui.features.WidgetsPage
import com.isg32.carino.ui.theme.CarinoTheme
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VMotion
import com.isg32.carino.ui.theme.VPalette
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: CarinoViewModel by viewModels()
    private lateinit var repo: LauncherRepository

    private var pendingPhotoCallback: ((String?) -> Unit)? = null
    private var pendingPermissionCallback: ((Boolean) -> Unit)? = null

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val cb = pendingPhotoCallback
        pendingPhotoCallback = null
        if (uri == null || cb == null) {
            cb?.invoke(null)
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            cb(repo.copyToWallpapers(uri))
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val cb = pendingPermissionCallback
        pendingPermissionCallback = null
        cb?.invoke(granted)
    }

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        vm.refreshSystemStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashFiles.installCrashHandler(applicationContext)
        super.onCreate(savedInstanceState)

        repo = LauncherRepository(this).apply {
            onPickImageRequest = { cb ->
                pendingPhotoCallback?.invoke(null)
                pendingPhotoCallback = cb
                pickMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
            onRequestPermission = { perm, cb ->
                pendingPermissionCallback?.invoke(false)
                pendingPermissionCallback = cb
                permissionLauncher.launch(perm)
            }
            onRequestHomeRole = { intent ->
                homeRoleLauncher.launch(intent)
            }
        }
        vm.attachRepository(repo)

        enableEdgeToEdge()

        setContent {
            val layout by vm.activeLayout.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val palette = remember(layout, systemDark) {
                VPalette.forLayout(layout, systemDark)
            }

            LaunchedEffect(palette.isDark) {
                val barStyle = if (palette.isDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }

            CarinoTheme(palette = palette) {
                CarinoRoot(vm = vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        repo.onResume()
    }

    override fun onPause() {
        repo.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        repo.dispose()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            repo.onHomePressed()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        repo.widgets.onActivityResult(requestCode, resultCode)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarinoRoot(vm: CarinoViewModel) {
    val settings by vm.settings.collectAsState()
    val appsLoaded by vm.appsLoaded.collectAsState()
    val crashReport by vm.crashReport.collectAsState()
    val toastMessage by vm.toastMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (!appsLoaded && !settings.onboarded) {
            LoadingPage()
        } else if (!settings.onboarded) {
            OnboardingFlow(vm = vm)
        } else {
            CarinoShell(vm = vm)
        }

        crashReport?.let { report ->
            CrashOverlay(
                report = report,
                vm = vm,
                onDismiss = { vm.dismissCrashReport() },
            )
        }

        ToastHost(
            message = toastMessage,
            onDone = { vm.clearToast() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CarinoShell(vm: CarinoViewModel) {
    val layout by vm.activeLayout.collectAsState()
    val settings by vm.settings.collectAsState()
    val activeTab by vm.activeTab.collectAsState()
    val searchOpen by vm.searchOpen.collectAsState()
    val customising by vm.customising.collectAsState()
    val settingsOpen by vm.settingsOpen.collectAsState()
    val opening by vm.opening.collectAsState()
    val scope = rememberCoroutineScope()

    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var pickSlotTarget by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
    var showVaultPin by remember { mutableStateOf(false) }
    var showVaultFolder by remember { mutableStateOf(false) }
    var showEssentialsEdit by remember { mutableStateOf(false) }
    var initialSettingsRoute by remember { mutableStateOf(SettingsScreen.Main) }

    val pagerState = rememberPagerState(initialPage = activeTab, pageCount = { 3 })

    LaunchedEffect(activeTab) {
        if (pagerState.currentPage != activeTab) {
            pagerState.animateScrollToPage(activeTab, animationSpec = tween(VMotion.standardMs))
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != activeTab) {
            vm.setTab(pagerState.currentPage)
        }
    }

    if (activeTab != 0 && !searchOpen && !customising && !settingsOpen && !showEssentialsEdit) {
        BackHandler {
            vm.setTab(0)
        }
    }

    val blurFrac by animateFloatAsState(
        targetValue = if (opening) 1f else 0f,
        animationSpec = tween(VMotion.fastMs),
        label = "launch_blur",
    )

    val pagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val veilAlpha = pagePosition.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .let {
                if (Build.VERSION.SDK_INT >= 31 && blurFrac > 0.01f) {
                    it.blur((blurFrac * 18f).dp)
                } else {
                    it
                }
            },
    ) {
        // Live wallpaper + smooth veil crossfade when paging from Home (0) -> Apps/Widgets (1..2)
        WallpaperView(
            wallpaper = layout.wallpaper,
            vm = vm,
            dim = layout.dim,
            blur = layout.blur,
            soft = false,
        )
        if (veilAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(veilAlpha),
            ) {
                CarinoBackdrop(
                    vm = vm,
                    wallpaper = layout.wallpaper,
                    dim = layout.dim,
                    blur = layout.blur,
                    veiled = true,
                    soft = true,
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> HomePage(
                    vm = vm,
                    onAppMenu = { menuApp = it },
                    onPickSlot = { isDock, idx -> pickSlotTarget = isDock to idx },
                    onOpenVault = {
                        if (settings.hasPasscode) {
                            showVaultPin = true
                        } else {
                            showVaultFolder = true
                        }
                    },
                )
                1 -> AppsPage(
                    vm = vm,
                    onAppMenu = { menuApp = it },
                    onOpenSearch = { vm.openSearch() },
                    onOpenSettings = {
                        initialSettingsRoute = SettingsScreen.Main
                        vm.openSettings()
                    },
                )
                2 -> WidgetsPage(
                    vm = vm,
                    onOpenSettings = {
                        initialSettingsRoute = SettingsScreen.Main
                        vm.openSettings()
                    },
                    onOpenWeatherSettings = {
                        initialSettingsRoute = SettingsScreen.Weather
                        vm.openSettings()
                    },
                )
            }
        }

        // Persistent floating glass tab bar
        VTabBar(
            tabs = listOf(
                VTabItem("Home", VIcon.home, "tab_home"),
                VTabItem("Apps", VIcon.apps, "tab_apps"),
                VTabItem("Widgets", VIcon.widgets, "tab_widgets"),
            ),
            selectedIndex = pagerState.currentPage,
            pagePosition = pagePosition,
            onSelect = { idx ->
                scope.launch {
                    vm.setTab(idx)
                    pagerState.animateScrollToPage(idx, animationSpec = tween(VMotion.standardMs))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp),
        )

        // Launch veil softening app launch
        if (blurFrac > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.isg32.carino.ui.theme.LocalVPalette.current.paper.copy(alpha = blurFrac * 0.25f)),
            )
        }
    }

    AnimatedVisibility(
        visible = searchOpen,
        enter = fadeIn(tween(VMotion.fastMs)),
        exit = fadeOut(tween(VMotion.fastMs)),
    ) {
        SearchSheet(
            vm = vm,
            onDismiss = { vm.closeSearch() },
            onAppMenu = { menuApp = it },
            onEditEssentials = { showEssentialsEdit = true },
        )
    }

    AnimatedVisibility(
        visible = showEssentialsEdit,
        enter = fadeIn(tween(VMotion.fastMs)),
        exit = fadeOut(tween(VMotion.fastMs)),
    ) {
        EssentialsEditPage(
            vm = vm,
            onClose = { showEssentialsEdit = false },
        )
    }

    AnimatedVisibility(
        visible = customising,
        enter = fadeIn(tween(VMotion.standardMs)),
        exit = fadeOut(tween(VMotion.standardMs)),
    ) {
        CustomiseScreen(
            vm = vm,
            onDone = { vm.closeCustomise() },
        )
    }

    AnimatedVisibility(
        visible = settingsOpen,
        enter = fadeIn(tween(VMotion.fastMs)),
        exit = fadeOut(tween(VMotion.fastMs)),
    ) {
        SettingsHost(
            vm = vm,
            initialRoute = initialSettingsRoute,
            onClose = { vm.closeSettings() },
            onOpenCustomise = { vm.openCustomise() },
        )
    }

    menuApp?.let { app ->
        AppMenuSheet(
            app = app,
            vm = vm,
            onDismiss = { menuApp = null },
        )
    }

    pickSlotTarget?.let { (isDock, idx) ->
        AppPickerSheet(
            vm = vm,
            title = if (isDock) "Choose dock app" else "Choose favourite app",
            onPick = { app ->
                if (isDock) vm.setDock(idx, app.key) else vm.setFavourite(idx, app.key)
            },
            onDismiss = { pickSlotTarget = null },
        )
    }

    if (showVaultPin) {
        PasscodeDialog(
            title = "Hidden apps",
            subtitle = "Enter your 4-digit passcode.",
            expectedHash = settings.vaultHash,
            expectedSalt = settings.vaultSalt,
            onSuccess = {
                showVaultPin = false
                showVaultFolder = true
            },
            onDismiss = { showVaultPin = false },
        )
    }

    if (showVaultFolder) {
        HiddenFolderSheet(
            vm = vm,
            onDismiss = { showVaultFolder = false },
            onOpenManage = {
                initialSettingsRoute = SettingsScreen.HiddenApps
                vm.openSettings()
            },
        )
    }
}
