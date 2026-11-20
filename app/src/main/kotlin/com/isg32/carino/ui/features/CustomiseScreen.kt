package com.isg32.carino.ui.features

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.HomeLayout
import com.isg32.carino.data.IconStyle
import com.isg32.carino.data.InkMode
import com.isg32.carino.data.WallpaperRef
import com.isg32.carino.ui.common.WallpaperPaletteCanvas
import com.isg32.carino.ui.common.WallpaperView
import com.isg32.carino.ui.components.CarinoPopupDialog
import com.isg32.carino.ui.components.CarinoSheet
import com.isg32.carino.ui.components.ConfirmDialog
import com.isg32.carino.ui.components.VActionRow
import com.isg32.carino.ui.components.VButton
import com.isg32.carino.ui.components.VCloseHeader
import com.isg32.carino.ui.components.VDestinationRow
import com.isg32.carino.ui.components.VHairlineDivider
import com.isg32.carino.ui.components.VListCard
import com.isg32.carino.ui.components.VListRow
import com.isg32.carino.ui.components.VPillButton
import com.isg32.carino.ui.components.VSectionTitle
import com.isg32.carino.ui.components.VSegmented
import com.isg32.carino.ui.components.VSlider
import com.isg32.carino.ui.components.VSurface
import com.isg32.carino.ui.components.VToggleRow
import com.isg32.carino.ui.theme.CarinoTheme
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VPalette
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import com.isg32.carino.ui.theme.VWallpapers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomiseScreen(
    vm: CarinoViewModel,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onDone)
    val ink = LocalVInk.current
    val layoutsState by vm.layouts.collectAsState()
    val active = layoutsState.active
    val systemDark = isSystemInDarkTheme()

    var showWallpaperSheet by remember { mutableStateOf(false) }
    var menuLayout by remember { mutableStateOf<HomeLayout?>(null) }
    var renameTarget by remember { mutableStateOf<HomeLayout?>(null) }
    var deleteTarget by remember { mutableStateOf<HomeLayout?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.isg32.carino.ui.theme.LocalVPalette.current.paper),
    ) {
        WallpaperView(
            wallpaper = active.wallpaper,
            vm = vm,
            dim = (active.dim + 0.25).coerceAtMost(0.75),
            blur = active.blur,
            soft = true,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            VCloseHeader(
                title = "Customise",
                onClose = onDone,
                trailing = {
                    VPillButton(
                        label = "Edit slots",
                        icon = VIcon.edit,
                        onClick = {
                            onDone()
                            vm.setTab(0)
                            vm.setEditHome(true)
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        tag = "customise_edit_slots",
                    )
                },
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(layoutsState.layouts, key = { it.id }) { item ->
                    val isSelected = item.id == active.id
                    val cardPalette = VPalette.forLayout(item, systemDark)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 148.dp, height = 248.dp)
                                .clip(RoundedCornerShape(VRadius.card))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ink else ink.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(VRadius.card),
                                )
                                .combinedClickable(
                                    onClick = { vm.setActiveLayout(item.id) },
                                    onLongClick = { menuLayout = item },
                                )
                                .testTag("customise_layout_${item.id}"),
                        ) {
                            CarinoTheme(palette = cardPalette) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    WallpaperView(
                                        wallpaper = item.wallpaper,
                                        vm = vm,
                                        dim = item.dim,
                                        blur = item.blur,
                                        soft = true,
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "12:00",
                                            style = VText.doto(24f, weight = 700)
                                                .copy(color = cardPalette.ink),
                                        )
                                        Text(
                                            text = "${item.columns}×${item.favRows}",
                                            style = VText.chip.copy(color = cardPalette.ink.copy(alpha = 0.7f)),
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = item.name,
                            style = VText.secondary.copy(
                                color = if (isSelected) ink else ink.copy(alpha = 0.65f),
                            ),
                        )
                    }
                }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 148.dp, height = 248.dp)
                                .clip(RoundedCornerShape(VRadius.card))
                                .border(VSpace.hairline, ink.copy(alpha = 0.3f), RoundedCornerShape(VRadius.card))
                                .clickable { vm.addLayout() }
                                .testTag("customise_new_layout"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                VIconView(VIcon.plus, size = 24.dp)
                                Text("New layout", style = VText.body.copy(color = ink))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(" ", style = VText.secondary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = VSpace.gutter)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                VListCard {
                    VDestinationRow(
                        title = "Wallpaper",
                        value = active.wallpaper.builtin?.replaceFirstChar { it.uppercase() } ?: "Photo",
                        icon = VIcon.image,
                        onClick = { showWallpaperSheet = true },
                        tag = "customise_wallpaper",
                    )
                    VHairlineDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("Ink", style = VText.rowTitle.copy(color = ink))
                        Spacer(Modifier.height(10.dp))
                        VSegmented(
                            options = listOf(
                                InkMode.white to "White",
                                InkMode.black to "Black",
                                InkMode.auto to "Auto",
                            ),
                            selected = active.ink,
                            onSelected = { m -> vm.updateActiveLayout { it.copy(ink = m) } },
                            compact = true,
                            tagPrefix = "customise_ink",
                        )
                    }
                    VHairlineDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Glass", style = VText.rowTitle.copy(color = ink))
                            Text(
                                "${(active.glass * 100).toInt()}%",
                                style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        VSlider(
                            value = active.glass.toFloat(),
                            onValueChange = { v -> vm.updateActiveLayout { it.copy(glass = v.toDouble()) } },
                            tag = "customise_glass_slider",
                        )
                    }
                }

                VListCard {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("Icons", style = VText.rowTitle.copy(color = ink))
                        Spacer(Modifier.height(10.dp))
                        VSegmented(
                            options = listOf(
                                IconStyle.mono to "Monochrome",
                                IconStyle.colored to "Colored",
                            ),
                            selected = active.icons,
                            onSelected = { s -> vm.updateActiveLayout { it.copy(icons = s) } },
                            compact = true,
                            tagPrefix = "customise_icons",
                        )
                    }
                    VHairlineDivider()
                    VToggleRow(
                        title = "Blur behind Home icons",
                        subtitle = "Frosted circles behind favourites and dock.",
                        checked = active.iconBlur,
                        onCheckedChange = { b -> vm.updateActiveLayout { it.copy(iconBlur = b) } },
                        tag = "customise_icon_blur",
                    )
                    VHairlineDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("Columns & Dock", style = VText.rowTitle.copy(color = ink))
                        Spacer(Modifier.height(10.dp))
                        VSegmented(
                            options = listOf(3 to "3", 4 to "4", 5 to "5"),
                            selected = active.columns,
                            onSelected = { c -> vm.updateActiveLayout { it.copy(columns = c) } },
                            compact = true,
                            tagPrefix = "customise_cols",
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Favourite rows", style = VText.rowTitle.copy(color = ink))
                        Spacer(Modifier.height(10.dp))
                        VSegmented(
                            options = listOf(1 to "1", 2 to "2", 3 to "3"),
                            selected = active.favRows,
                            onSelected = { r -> vm.updateActiveLayout { it.copy(favRows = r) } },
                            compact = true,
                            tagPrefix = "customise_rows",
                        )
                    }
                }
            }
        }
    }

    if (showWallpaperSheet) {
        WallpaperSheet(
            vm = vm,
            onDismiss = { showWallpaperSheet = false },
        )
    }

    menuLayout?.let { target ->
        CarinoSheet(onDismiss = { menuLayout = null }) {
            Text(text = target.name, style = VText.doto(24f, weight = 700).copy(color = ink))
            Spacer(Modifier.height(14.dp))
            VListCard {
                VActionRow(
                    title = "Rename",
                    icon = VIcon.edit,
                    onClick = {
                        renameTarget = target
                        menuLayout = null
                    },
                )
                VHairlineDivider()
                VActionRow(
                    title = "Duplicate",
                    icon = VIcon.copy,
                    onClick = {
                        vm.addLayout(from = target)
                        menuLayout = null
                    },
                )
                if (layoutsState.layouts.size > 1) {
                    VHairlineDivider()
                    VActionRow(
                        title = "Delete",
                        icon = VIcon.trash,
                        onClick = {
                            deleteTarget = target
                            menuLayout = null
                        },
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.name) }
        CarinoPopupDialog(onDismiss = { renameTarget = null }) {
            Text("Rename layout", style = VText.doto(22f, weight = 700).copy(color = ink))
            Spacer(Modifier.height(12.dp))
            VSurface(radius = VRadius.chip) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = VText.body.copy(color = ink),
                    cursorBrush = SolidColor(ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            VButton(
                label = "Save",
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        vm.updateLayout(target.copy(name = trimmed))
                    }
                    renameTarget = null
                },
            )
            Spacer(Modifier.height(8.dp))
            VButton(label = "Cancel", primary = false, onClick = { renameTarget = null })
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "Delete ${target.name}?",
            body = "This removes the layout and its wallpaper setup.",
            confirmLabel = "Delete",
            onConfirm = {
                vm.deleteLayout(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
fun WallpaperSheet(
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
) {
    val ink = LocalVInk.current
    val layout by vm.activeLayout.collectAsState()
    val settings by vm.settings.collectAsState()
    val scope = rememberCoroutineScope()

    CarinoSheet(onDismiss = onDismiss) {
        Text("Wallpaper", style = VText.doto(24f, weight = 700).copy(color = ink))
        Spacer(Modifier.height(14.dp))

        VSectionTitle("Built-in")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(VWallpapers.all.entries.toList(), key = { it.key }) { (name, palette) ->
                val selected = layout.wallpaper.photo == null && layout.wallpaper.builtin == name
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 94.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) ink else ink.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                vm.updateActiveLayout { it.copy(wallpaper = WallpaperRef.builtin(name)) }
                                vm.syncSystemWallpaperNow()
                            }
                            .testTag("wp_builtin_$name"),
                    ) {
                        WallpaperPaletteCanvas(palette = palette)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name.replaceFirstChar { it.uppercase() },
                        style = VText.chip.copy(color = ink.copy(alpha = if (selected) 1f else 0.6f)),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (settings.photos.isNotEmpty()) {
            VSectionTitle("Photos")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(settings.photos, key = { it }) { path ->
                    val selected = layout.wallpaper.photo == path
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 94.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) ink else ink.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable {
                                vm.updateActiveLayout { it.copy(wallpaper = WallpaperRef.photo(path)) }
                                vm.syncSystemWallpaperNow()
                            },
                    ) {
                        WallpaperView(
                            wallpaper = WallpaperRef.photo(path),
                            vm = vm,
                            soft = false,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        VButton(
            label = "Choose photo",
            icon = VIcon.image,
            primary = false,
            tag = "wp_pick_photo",
            onClick = {
                vm.repo?.pickImage { path ->
                    if (path != null) {
                        vm.updateSettings { s ->
                            s.copy(photos = (listOf(path) + s.photos).distinct())
                        }
                        vm.updateActiveLayout { it.copy(wallpaper = WallpaperRef.photo(path)) }
                        vm.syncSystemWallpaperNow()
                    }
                }
            },
        )

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dim", style = VText.rowTitle.copy(color = ink))
            Text("${(layout.dim * 100).toInt()}%", style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)))
        }
        VSlider(
            value = (layout.dim / 0.6).toFloat(),
            onValueChange = { v -> vm.updateActiveLayout { it.copy(dim = v * 0.6) } },
            tag = "wp_dim_slider",
        )

        if (layout.wallpaper.photo != null) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Blur", style = VText.rowTitle.copy(color = ink))
                Text("${(layout.blur * 100).toInt()}%", style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)))
            }
            VSlider(
                value = layout.blur.toFloat(),
                onValueChange = { v ->
                    val nextBlur = v.toDouble()
                    vm.updateActiveLayout { it.copy(blur = nextBlur) }
                    layout.wallpaper.photo?.let { photo ->
                        scope.launch { vm.repo?.bakeBlur(photo, nextBlur) }
                    }
                },
                tag = "wp_blur_slider",
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VButton(
                label = "Set lock screen",
                primary = false,
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        val ok = vm.repo?.applySystemWallpaper(layout, "lock") == true
                        vm.showToast(if (ok) "Lock screen wallpaper updated" else "Could not set wallpaper")
                    }
                },
            )
            VButton(
                label = "Set both",
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        val ok = vm.repo?.applySystemWallpaper(layout, "both") == true
                        vm.showToast(if (ok) "Home & lock wallpapers updated" else "Could not set wallpaper")
                    }
                },
            )
        }
    }
}
