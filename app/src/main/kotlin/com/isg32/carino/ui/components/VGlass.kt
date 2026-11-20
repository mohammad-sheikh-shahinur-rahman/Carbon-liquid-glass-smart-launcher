package com.isg32.carino.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.isg32.carino.ui.theme.LiquidGlassTheme
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import com.isg32.carino.ui.theme.liquidGlassPill
import com.isg32.carino.ui.theme.liquidGlassSurface

@Composable
fun VGlass(
    modifier: Modifier = Modifier,
    radius: Dp = VRadius.pill,
    fill: Color? = null,
    borderAlpha: Float = 0.22f,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = com.isg32.carino.ui.theme.LiquidGlassTheme.tokens
    val shape = RoundedCornerShape(radius)
    val mod = if (fill != null) {
        modifier
            .clip(shape)
            .background(fill)
            .border(tokens.borderWidth, tokens.borderBrush, shape)
    } else {
        modifier.liquidGlassSurface(
            shape = shape,
            elevated = false,
            borderWidth = tokens.borderWidth,
            showSpecularSheen = true,
        )
    }
    Box(
        modifier = mod,
        content = content,
    )
}

@Composable
fun VEdgeBlur(
    height: Dp = 44.dp,
    top: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val pal = LocalVPalette.current
    val edge = pal.paper.copy(alpha = 0.42f)
    val colors = if (top) listOf(edge, Color.Transparent) else listOf(Color.Transparent, edge)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(colors)),
    )
}

data class VTabItem(
    val label: String,
    val icon: VIcon,
    val tag: String,
)

/**
 * Floating Liquid Glass pill tab bar with a luminous refractive glass lens indicator tracking pager fraction 1:1.
 */
@Composable
fun VTabBar(
    tabs: List<VTabItem>,
    selectedIndex: Int,
    pagePosition: Float,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pal = LocalVPalette.current
    val tokens = LiquidGlassTheme.tokens
    val ink = pal.ink
    val clampedPos = pagePosition.coerceIn(0f, (tabs.size - 1).coerceAtLeast(0).toFloat())
    val pillShape = RoundedCornerShape(VRadius.pill)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(62.dp)
            .liquidGlassPill(shape = pillShape, smoked = true),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        ) {
            val slotW = maxWidth / tabs.size.coerceAtLeast(1)
            val lensFill = if (tokens.isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.34f),
                        Color.White.copy(alpha = 0.18f),
                        Color(0x338EDCE0),
                    ),
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.94f),
                        Color.White.copy(alpha = 0.72f),
                    ),
                )
            }
            val lensBorder = if (tokens.isDark) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.24f),
                        Color(0xAA9CE3E8),
                    ),
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.98f),
                        Color.Black.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.80f),
                    ),
                )
            }

            // Refractive Liquid Glass active lens indicator
            Box(
                modifier = Modifier
                    .offset(x = slotW * clampedPos)
                    .width(slotW)
                    .fillMaxHeight()
                    .clip(pillShape)
                    .background(lensFill)
                    .border(1.2.dp, lensBorder, pillShape),
            )
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, item ->
                    val distance = kotlin.math.abs(clampedPos - index).coerceIn(0f, 1f)
                    val active = distance < 0.5f
                    val fg = if (active) ink else ink.copy(alpha = 0.68f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag(item.tag)
                            .clip(pillShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            VIconView(
                                icon = item.icon,
                                size = 17.dp,
                                strokeWidth = if (active) 2.1f else 1.5f,
                                color = fg,
                            )
                            Text(
                                text = item.label,
                                style = VText.inter(13f, weight = if (active) 600 else 500).copy(color = fg),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
