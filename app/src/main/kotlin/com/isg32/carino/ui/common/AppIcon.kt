package com.isg32.carino.ui.common

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.AppEntry
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.IconStyle
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import kotlin.math.roundToInt

@Composable
fun AppGlyph(
    app: AppEntry,
    vm: CarinoViewModel,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    style: IconStyle = IconStyle.mono,
    lifted: Boolean = false,
    blur: Boolean = false,
) {
    val ink = LocalVInk.current
    val pal = LocalVPalette.current
    val paper = pal.paper
    val settings by vm.settings.collectAsState()
    val dots by vm.dots.collectAsState()
    val showDot = !lifted && settings.dots && (dots[app.pkg] ?: 0) > 0

    val colored = style == IconStyle.colored && !lifted
    val bitmap by produceState<Bitmap?>(initialValue = null, app.key, colored) {
        value = vm.loadIconBitmap(app.key, mono = !colored)
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (colored && bitmap != null) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(
                        width = 0.85.dp,
                        brush = pal.liquidBorderBrush,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape),
                )
            }
        } else {
            val fg = if (lifted) paper else ink
            val circleFill = when {
                lifted -> ink
                blur -> pal.glassCard.copy(alpha = (pal.glassCard.alpha + 0.14f).coerceAtMost(0.45f))
                else -> pal.glassCard
            }
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(circleFill)
                    .border(VSpace.hairline, ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = app.label,
                        colorFilter = ColorFilter.tint(fg),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(size * 0.62f),
                    )
                } else {
                    val letter = app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    Text(
                        text = letter,
                        style = VText.doto(size.value * 0.46f, weight = 800, lineHeight = size.value * 0.5f)
                            .copy(color = fg),
                    )
                }
            }
        }

        if (showDot) {
            val dotSize = (size.value * 0.2f).coerceIn(8f, 13f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-1).dp, y = 1.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(ink)
                    .border(2.dp, paper, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: AppEntry,
    vm: CarinoViewModel,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    style: IconStyle = IconStyle.mono,
    showLabel: Boolean = true,
    blur: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onOpened: (() -> Unit)? = null,
) {
    val ink = LocalVInk.current
    val haptic = LocalHapticFeedback.current
    var boundsRect: android.graphics.Rect? = null

    Column(
        modifier = modifier
            .width(size + 16.dp)
            .testTag("app_tile_${app.pkg}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    val b = coords.boundsInWindow()
                    boundsRect = android.graphics.Rect(
                        b.left.roundToInt(),
                        b.top.roundToInt(),
                        b.right.roundToInt(),
                        b.bottom.roundToInt(),
                    )
                }
                .clip(CircleShape)
                .combinedClickable(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.launchApp(app.key, boundsRect)
                        onOpened?.invoke()
                    },
                    onLongClick = onLongPress?.let {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            it()
                        }
                    },
                ),
        ) {
            AppGlyph(app = app, vm = vm, size = size, style = style, blur = blur)
        }
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = VText.inter(12f, weight = 500).copy(color = ink),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmptySlot(
    size: Dp = 56.dp,
    highlight: Boolean = false,
    showLabelSpacer: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val ink = LocalVInk.current
    val pal = LocalVPalette.current
    Column(
        modifier = modifier.width(size + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .let { if (onClick != null) it.combinedClickable(onClick = onClick) else it },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeW = 1.dp.toPx()
                val r = this.size.minDimension / 2f - strokeW
                if (highlight) {
                    drawCircle(color = ink, radius = r)
                } else {
                    val dashes = 28
                    val sweep = 360f / dashes
                    for (i in 0 until dashes) {
                        drawArc(
                            color = ink.copy(alpha = 0.65f),
                            startAngle = i * sweep,
                            sweepAngle = sweep * 0.55f,
                            useCenter = false,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round),
                        )
                    }
                }
            }
            Text(
                text = "+",
                style = VText.inter(22f, weight = 300).copy(
                    color = if (highlight) pal.paper else ink,
                ),
            )
        }
        if (showLabelSpacer) {
            Spacer(Modifier.height(24.dp))
        }
    }
}
