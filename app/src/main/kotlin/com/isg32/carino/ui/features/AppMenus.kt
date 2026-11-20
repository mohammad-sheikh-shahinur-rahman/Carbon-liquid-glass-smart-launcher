package com.isg32.carino.ui.features

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.ui.common.AppGlyph
import com.isg32.carino.ui.components.CarinoSheet
import com.isg32.carino.ui.components.ConfirmDialog
import com.isg32.carino.ui.components.VActionRow
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VSurface
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VText

@Composable
fun AppMenuSheet(
    app: AppEntry,
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    val layout by vm.activeLayout.collectAsState()
    val settings by vm.settings.collectAsState()
    val onHome = app.key in layout.favGrid || app.key in layout.dockRow
    val hidden = app.key in settings.hidden
    val isSystem = remember(app.pkg) { vm.repo?.isSystemApp(app.pkg) ?: false }
    val shortcuts = remember(app.key) { vm.repo?.shortcuts(app.key).orEmpty() }
    var confirmUninstall by remember { mutableStateOf(false) }

    CarinoSheet(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppGlyph(app = app, vm = vm, size = 44.dp, style = layout.icons)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.label, style = VText.doto(22f, weight = 700).copy(color = ink))
                Text(text = app.pkg, style = VText.secondary.copy(color = ink.copy(alpha = 0.55f)))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (shortcuts.isNotEmpty()) {
            VListCard {
                shortcuts.forEachIndexed { idx, sc ->
                    if (idx > 0) VHairlineDivider()
                    VActionRow(
                        title = sc.label,
                        icon = VIcon.openExternal,
                        onClick = {
                            vm.repo?.startShortcut(app.key, sc.id)
                            onDismiss()
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        VListCard {
            VActionRow(
                title = if (onHome) "Remove from Home" else "Pin to Home",
                icon = if (onHome) VIcon.minus else VIcon.pin,
                onClick = {
                    if (onHome) {
                        vm.removeFromHome(app.key)
                    } else if (!vm.pinToHome(app.key)) {
                        vm.showToast("Home is full")
                    }
                    onDismiss()
                },
                tag = "app_menu_pin",
            )
            VHairlineDivider()
            VActionRow(
                title = "Edit Home layout",
                icon = VIcon.edit,
                onClick = {
                    onDismiss()
                    vm.setTab(0)
                    vm.setEditHome(true)
                },
                tag = "app_menu_edit_home",
            )
            VHairlineDivider()
            VActionRow(
                title = if (hidden) "Unhide app" else "Hide app",
                icon = VIcon.hide,
                onClick = {
                    vm.toggleHidden(app.key)
                    onDismiss()
                },
                tag = "app_menu_hide",
            )
            VHairlineDivider()
            VActionRow(
                title = "App info",
                icon = VIcon.info,
                onClick = {
                    vm.repo?.appInfo(app.key)
                    onDismiss()
                },
                tag = "app_menu_info",
            )
            if (!isSystem) {
                VHairlineDivider()
                VActionRow(
                    title = "Uninstall",
                    icon = VIcon.trash,
                    onClick = { confirmUninstall = true },
                    tag = "app_menu_uninstall",
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmUninstall) {
        ConfirmDialog(
            title = "Uninstall ${app.label}?",
            body = "Android will ask you to confirm removing this app.",
            confirmLabel = "Uninstall",
            onConfirm = {
                confirmUninstall = false
                onDismiss()
                vm.repo?.uninstall(app.pkg)
            },
            onDismiss = { confirmUninstall = false },
        )
    }
}

@Composable
fun AppPickerSheet(
    vm: CarinoViewModel,
    title: String = "Choose an app",
    onPick: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    val apps by vm.visibleApps.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
    }

    CarinoSheet(onDismiss = onDismiss) {
        Text(text = title, style = VText.doto(24f, weight = 700).copy(color = ink))
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
                            Text("Search apps", style = VText.body.copy(color = ink.copy(alpha = 0.45f)))
                        }
                        inner()
                    },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
        ) {
            items(filtered, key = { it.key }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onPick(app)
                            onDismiss()
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppGlyph(app = app, vm = vm, size = 38.dp, style = layout.icons)
                    Spacer(Modifier.width(12.dp))
                    Text(text = app.label, style = VText.rowTitle.copy(color = ink))
                }
            }
        }
    }
}
