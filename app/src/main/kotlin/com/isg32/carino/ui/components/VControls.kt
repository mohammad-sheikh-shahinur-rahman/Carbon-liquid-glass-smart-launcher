package com.isg32.carino.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VMotion
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText

@Composable
fun <T> VSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    tagPrefix: String = "seg",
) {
    val ink = LocalVInk.current
    val shape = RoundedCornerShape(VRadius.pill)
    Row(
        modifier = modifier
            .clip(shape)
            .background(ink.copy(alpha = 0.06f))
            .border(VSpace.hairline, ink.copy(alpha = 0.2f), shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { idx, (value, label) ->
            val isSelected = value == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) ink else Color.Transparent,
                animationSpec = tween(VMotion.fastMs),
                label = "seg_bg",
            )
            val fg = if (isSelected) com.isg32.carino.ui.theme.VColors.onColor(ink) else ink.copy(alpha = 0.72f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .testTag("${tagPrefix}_$idx")
                    .defaultMinSize(minHeight = if (compact) 34.dp else 40.dp)
                    .clip(shape)
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelected(value) }
                    .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = (if (compact) VText.chip else VText.body).copy(color = fg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun VSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val ink = LocalVInk.current
    val trackBg by animateColorAsState(
        targetValue = if (checked) ink else Color.Transparent,
        animationSpec = tween(VMotion.fastMs),
        label = "switch_bg",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 4.dp,
        animationSpec = tween(VMotion.fastMs),
        label = "switch_thumb",
    )
    val thumbColor = if (checked) com.isg32.carino.ui.theme.VColors.onColor(ink) else ink

    Box(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(28.dp)
                .clip(CircleShape)
                .background(trackBg)
                .border(VSpace.hairline, ink.copy(alpha = if (checked) 1f else 0.35f), CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
            )
        }
    }
}

@Composable
fun VSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null,
) {
    val ink = LocalVInk.current
    val clamped = value.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onValueChange((change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackW = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.18f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(3.dp)
                .clip(CircleShape)
                .background(ink),
        )
        Box(
            modifier = Modifier
                .offset(x = (trackW - 18.dp) * clamped)
                .size(18.dp)
                .clip(CircleShape)
                .background(ink),
        )
    }
}
