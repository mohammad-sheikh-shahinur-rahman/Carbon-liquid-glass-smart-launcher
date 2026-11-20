package com.isg32.carino.ui.features

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.ui.common.CarinoBackdrop
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VIconButton
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VListRow
import com.isg32.carino.ui.components.VScrollPage
import com.isg32.carino.ui.components.VSectionTitle
import com.isg32.carino.ui.components.VTitleHeader
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView

@Composable
fun EssentialsEditPage(
    vm: CarinoViewModel,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val settings by vm.settings.collectAsState()
    val layout by vm.activeLayout.collectAsState()
    val current = settings.essentials
    val available = EssentialsCatalog.all.filter { it.id !in current }

    Box(modifier = Modifier.fillMaxSize()) {
        CarinoBackdrop(
            vm = vm,
            wallpaper = layout.wallpaper,
            dim = layout.dim,
            blur = layout.blur,
            veiled = true,
            soft = true,
        )
        VScrollPage {
            VTitleHeader(title = "Edit Essentials", onBack = onClose)
            Spacer(Modifier.height(8.dp))

            if (current.isNotEmpty()) {
                VSectionTitle("Shown")
                VListCard {
                    current.forEachIndexed { idx, id ->
                        val entry = EssentialsCatalog.byId(id) ?: return@forEachIndexed
                        if (idx > 0) VHairlineDivider()
                        VListRow(
                            title = entry.label,
                            leading = { VIconView(entry.icon, size = 20.dp) },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    if (idx > 0) {
                                        VIconButton(
                                            icon = VIcon.up,
                                            contentDescription = "Move up",
                                            size = 38.dp,
                                            iconSize = 16.dp,
                                            onClick = {
                                                val copy = current.toMutableList()
                                                copy[idx] = copy[idx - 1]
                                                copy[idx - 1] = id
                                                vm.updateSettings { it.copy(essentials = copy) }
                                            },
                                        )
                                    }
                                    if (idx < current.lastIndex) {
                                        VIconButton(
                                            icon = VIcon.downArrow,
                                            contentDescription = "Move down",
                                            size = 38.dp,
                                            iconSize = 16.dp,
                                            onClick = {
                                                val copy = current.toMutableList()
                                                copy[idx] = copy[idx + 1]
                                                copy[idx + 1] = id
                                                vm.updateSettings { it.copy(essentials = copy) }
                                            },
                                        )
                                    }
                                    VIconButton(
                                        icon = VIcon.minus,
                                        contentDescription = "Remove",
                                        size = 38.dp,
                                        iconSize = 16.dp,
                                        onClick = {
                                            vm.updateSettings { it.copy(essentials = current - id) }
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (available.isNotEmpty()) {
                VSectionTitle("Add")
                VListCard {
                    available.forEachIndexed { idx, entry ->
                        if (idx > 0) VHairlineDivider()
                        VListRow(
                            title = entry.label,
                            leading = { VIconView(entry.icon, size = 20.dp) },
                            trailing = {
                                VIconButton(
                                    icon = VIcon.plus,
                                    contentDescription = "Add ${entry.label}",
                                    size = 38.dp,
                                    iconSize = 16.dp,
                                    onClick = {
                                        vm.updateSettings { it.copy(essentials = current + entry.id) }
                                    },
                                )
                            },
                            onClick = {
                                vm.updateSettings { it.copy(essentials = current + entry.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}
