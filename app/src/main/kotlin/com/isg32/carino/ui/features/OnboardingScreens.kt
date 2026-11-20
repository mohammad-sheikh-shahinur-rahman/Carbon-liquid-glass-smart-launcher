package com.isg32.carino.ui.features

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.ui.common.WallpaperPaletteCanvas
import com.isg32.carino.ui.components.DotLoader
import com.isg32.carino.ui.components.VButton
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VListRow
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import com.isg32.carino.ui.theme.VWallpapers

@Composable
fun OnboardingFlow(
    vm: CarinoViewModel,
) {
    var step by remember { mutableIntStateOf(0) }
    val appsLoaded by vm.appsLoaded.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperPaletteCanvas(palette = VWallpapers.liquid)
        if (!appsLoaded) {
            LoadingPage()
        } else if (step == 0) {
            WelcomePage(onNext = { step = 1 })
        } else {
            PermissionsPage(
                vm = vm,
                onFinish = {
                    vm.updateSettings { it.copy(onboarded = true) }
                    vm.syncSystemWallpaperNow()
                },
            )
        }
    }
}

@Composable
fun LoadingPage() {
    val ink = LocalVInk.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Carbon",
            style = VText.doto(38f, weight = 700).copy(color = ink),
        )
        Spacer(Modifier.height(18.dp))
        DotLoader()
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Getting your apps ready…",
            style = VText.secondary.copy(color = ink.copy(alpha = 0.72f)),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WelcomePage(
    onNext: () -> Unit,
) {
    val ink = LocalVInk.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = "Carbon",
            style = VText.doto(48f, weight = 700, lineHeight = 50f).copy(color = ink),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "A Liquid Glass home screen over your wallpaper. Clean App Library, native widgets, and zero background clutter.",
            style = VText.inter(16f, weight = 400, lineHeight = 24f).copy(color = ink.copy(alpha = 0.84f)),
        )
        Spacer(Modifier.weight(1.2f))
        VButton(
            label = "Get started",
            onClick = onNext,
            primary = true,
            tag = "onboarding_get_started",
        )
    }
}

@Composable
private fun PermissionsPage(
    vm: CarinoViewModel,
    onFinish: () -> Unit,
) {
    val ink = LocalVInk.current
    val isDefaultHome by vm.isDefaultHome.collectAsState()
    val notifAccess by vm.notificationAccess.collectAsState()
    val lockAccess by vm.lockAccess.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Optional setup",
                style = VText.title.copy(color = ink),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Turn on only what you want. You can change any of these later in Settings.",
                style = VText.body.copy(color = ink.copy(alpha = 0.74f)),
            )
            Spacer(Modifier.height(24.dp))

            VListCard {
                VListRow(
                    title = "Default home app",
                    subtitle = "Opens Carbon when you press Home.",
                    leading = { VIconView(VIcon.home, size = 20.dp) },
                    trailing = {
                        if (isDefaultHome) {
                            VIconView(VIcon.check, size = 18.dp)
                        } else {
                            VPillButton(
                                label = "Set",
                                onClick = { vm.repo?.requestDefaultHome() },
                                tag = "perm_default_home",
                            )
                        }
                    },
                )
                VHairlineDivider()
                VListRow(
                    title = "Notification dots & Now playing",
                    subtitle = "Shows dots on app icons and music controls. Nothing is saved.",
                    leading = { VIconView(VIcon.bell, size = 20.dp) },
                    trailing = {
                        if (notifAccess) {
                            VIconView(VIcon.check, size = 18.dp)
                        } else {
                            VPillButton(
                                label = "Allow",
                                onClick = { vm.repo?.requestNotificationAccess() },
                                tag = "perm_notifications",
                            )
                        }
                    },
                )
                VHairlineDivider()
                VListRow(
                    title = "Double-tap to lock",
                    subtitle = "Locks the screen cleanly while keeping fingerprint unlock.",
                    leading = { VIconView(VIcon.lock, size = 20.dp) },
                    trailing = {
                        if (lockAccess) {
                            VIconView(VIcon.check, size = 18.dp)
                        } else {
                            VPillButton(
                                label = "Allow",
                                onClick = { vm.repo?.requestLockAccess() },
                                tag = "perm_lock",
                            )
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        VButton(
            label = "Enter Carbon",
            onClick = onFinish,
            primary = true,
            tag = "onboarding_enter",
        )
    }
}
