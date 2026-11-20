package com.isg32.carino.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VInkProvider
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText

enum class VOutline { solid, dashed }

@Composable
fun VSurface(
    modifier: Modifier = Modifier,
    radius: Dp = VRadius.card,
    color: Color? = null,
    borderColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val pal = LocalVPalette.current
    val shape = RoundedCornerShape(radius)
    val bgMod = if (color != null) {
        Modifier.background(color)
    } else {
        Modifier.background(pal.liquidSurfaceBrush)
    }
    val borderMod = if (borderColor != null) {
        Modifier.border(VSpace.hairline, borderColor, shape)
    } else {
        Modifier.border(VSpace.hairline, pal.liquidBorderBrush, shape)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .then(bgMod)
            .then(borderMod),
        content = content,
    )
}

@Composable
fun VOutlined(
    modifier: Modifier = Modifier,
    radius: Dp = VRadius.card,
    outline: VOutline = VOutline.solid,
    alpha: Float = 0.28f,
    fill: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val ink = LocalVInk.current
    val borderColor = ink.copy(alpha = alpha)
    val shape = RoundedCornerShape(radius)
    val base = modifier.clip(shape).let { if (fill != null) it.background(fill) else it }
    val stroked = when (outline) {
        VOutline.solid -> base.border(VSpace.hairline, borderColor, shape)
        VOutline.dashed -> base.drawBehind {
            val r = radius.toPx()
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(r, r),
                style = Stroke(
                    width = VSpace.hairline.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                ),
            )
        }
    }
    Box(modifier = stroked, content = content)
}

@Composable
fun VButton(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    icon: VIcon? = null,
    tag: String? = null,
) {
    val pal = LocalVPalette.current
    val ink = LocalVInk.current
    val enabled = onClick != null
    val fg = if (primary) VColors.onColor(ink) else ink
    val shape = RoundedCornerShape(VRadius.pill)

    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .alpha(if (enabled) 1f else VColors.disabledOpacity)
            .clip(shape)
            .let {
                if (primary) it.background(ink) else it.background(pal.liquidSurfaceBrush)
            }
            .let {
                if (primary) it.border(VSpace.hairline, ink, shape)
                else it.border(VSpace.hairline, pal.liquidBorderBrush, shape)
            }
            .clickable(enabled = enabled, role = Role.Button) { onClick?.invoke() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        VInkProvider(fg) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    VIconView(icon = icon, size = 18.dp, strokeWidth = 1.8f, color = fg)
                }
                Text(
                    text = label,
                    style = (if (primary) VText.button else VText.buttonSecondary).copy(color = fg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun VPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    icon: VIcon? = null,
    tag: String? = null,
) {
    val pal = LocalVPalette.current
    val ink = LocalVInk.current
    val fg = if (inverted) VColors.onColor(ink) else ink
    val shape = RoundedCornerShape(VRadius.pill)
    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .let { if (inverted) it.background(ink) else it.background(pal.liquidSurfaceBrush) }
            .let {
                if (inverted) it.border(VSpace.hairline, ink, shape)
                else it.border(VSpace.hairline, pal.liquidBorderBrush, shape)
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        VInkProvider(fg) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (icon != null) {
                    VIconView(icon = icon, size = 15.dp, strokeWidth = 1.8f, color = fg)
                }
                Text(
                    text = label,
                    style = VText.chip.copy(color = fg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun VIconButton(
    icon: VIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    outlined: Boolean = false,
    tag: String? = null,
) {
    val pal = LocalVPalette.current
    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .size(size)
            .clip(CircleShape)
            .let {
                if (outlined) {
                    it.background(pal.liquidSurfaceBrush)
                        .border(VSpace.hairline, pal.liquidBorderBrush, CircleShape)
                } else {
                    it
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        VIconView(icon = icon, size = iconSize, contentDescription = contentDescription)
    }
}

@Composable
fun VChip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val ink = LocalVInk.current
    val bg = if (selected) ink else Color.Transparent
    val fg = if (selected) VColors.onColor(ink) else ink
    val shape = RoundedCornerShape(VRadius.pill)
    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .clip(shape)
            .background(bg)
            .border(VSpace.hairline, ink.copy(alpha = if (selected) 1f else 0.28f), shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = VText.chip.copy(color = fg),
            maxLines = 1,
        )
    }
}
