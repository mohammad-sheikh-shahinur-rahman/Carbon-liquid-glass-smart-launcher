package com.isg32.carino.ui.features

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.CarinoPrefs
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.components.CarinoPopupDialog
import com.isg32.carino.ui.components.CarinoSheet
import com.isg32.carino.ui.components.VButton
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PasscodeDialog(
    title: String,
    subtitle: String? = null,
    expectedHash: String? = null,
    expectedSalt: String? = null,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (expectedHash != null && expectedSalt != null) {
                val computed = CarinoPrefs.hashPasscode(pin, expectedSalt)
                if (computed == expectedHash) {
                    onSuccess(pin)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    errorMsg = "Wrong passcode"
                    pin = ""
                    scope.launch {
                        for (dx in listOf(14f, -14f, 10f, -10f, 5f, 0f)) {
                            shake.animateTo(dx, tween(45))
                        }
                    }
                }
            } else {
                onSuccess(pin)
            }
        }
    }

    CarinoPopupDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = title, style = VText.doto(24f, weight = 700).copy(color = ink))
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(text = subtitle, style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)))
            }
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { idx ->
                    val filled = idx < pin.length
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (filled) ink else ink.copy(alpha = 0.12f))
                            .border(VSpace.hairline, ink.copy(alpha = 0.45f), CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMsg ?: " ",
                style = VText.secondary.copy(color = ink.copy(alpha = 0.75f)),
            )
            Spacer(Modifier.height(12.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "⌫"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (digit in row) {
                            if (digit.isEmpty()) {
                                Spacer(Modifier.size(58.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .background(ink.copy(alpha = 0.08f))
                                        .border(VSpace.hairline, ink.copy(alpha = 0.2f), CircleShape)
                                        .clickable {
                                            errorMsg = null
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (digit == "⌫") {
                                                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                            } else if (pin.length < 4) {
                                                pin += digit
                                            }
                                        }
                                        .testTag("pin_key_$digit"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = digit,
                                        style = VText.doto(20f, weight = 700).copy(color = ink),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            VButton(label = "Cancel", onClick = onDismiss, primary = false)
        }
    }
}

@Composable
fun HiddenFolderSheet(
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
    onOpenManage: () -> Unit,
) {
    val ink = LocalVInk.current
    val apps by vm.apps.collectAsState()
    val settings by vm.settings.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    val hiddenApps = remember(apps, settings.hidden) {
        apps.filter { it.key in settings.hidden }
    }

    CarinoSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hidden apps",
                style = VText.doto(24f, weight = 700).copy(color = ink),
                modifier = Modifier.weight(1f),
            )
            VPillButton(
                label = "Manage",
                onClick = {
                    onDismiss()
                    onOpenManage()
                },
                tag = "vault_manage_button",
            )
        }
        Spacer(Modifier.height(12.dp))
        if (hiddenApps.isEmpty()) {
            Text(
                text = "No hidden apps. Long-press any app in Apps to hide it.",
                style = VText.body.copy(color = ink.copy(alpha = 0.65f)),
                modifier = Modifier.padding(vertical = 18.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            ) {
                items(hiddenApps, key = { it.key }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                vm.launchApp(app.key)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppGlyph(app = app, vm = vm, size = 40.dp, style = layout.icons)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = app.label,
                            style = VText.rowTitle.copy(color = ink),
                            modifier = Modifier.weight(1f),
                        )
                        VPillButton(
                            label = "Unhide",
                            onClick = { vm.toggleHidden(app.key) },
                        )
                    }
                }
            }
        }
    }
}
