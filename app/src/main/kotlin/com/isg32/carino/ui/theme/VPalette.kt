package com.isg32.carino.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.isg32.carino.data.HomeLayout
import com.isg32.carino.data.InkMode

@Immutable
data class VPalette(
    val isDark: Boolean,
    val glass: Float = 0.5f,
) {
    val ink: Color get() = if (isDark) VColors.white else VColors.black
    val paper: Color get() = if (isDark) VColors.black else VColors.white

    private val glassScale: Float get() = 0.3f + glass * 1.4f

    private fun scaled(c: Color): Color =
        c.copy(alpha = (c.alpha * glassScale).coerceIn(0f, 1f))

    val blurDp: Float get() = 8f + glass * 20f

    val glassBar: Color get() = scaled(if (isDark) VColors.glassBar else VColors.glassBarLight)
    val glassCard: Color get() = scaled(if (isDark) VColors.glassCard else VColors.glassCardLight)
    val glassSheet: Color get() = if (isDark) VColors.glassSheet else VColors.glassSheetLight
    val veil: Color get() = if (isDark) VColors.black.copy(alpha = 0.22f) else VColors.white.copy(alpha = 0.34f)
    val popupScrim: Color get() = if (isDark) VColors.popupScrim else VColors.popupScrimLight

    val liquidSurfaceBrush: androidx.compose.ui.graphics.Brush
        get() {
            val strength = 0.65f + glass * 0.7f
            return if (isDark) {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        VColors.white.copy(alpha = (0.20f * strength).coerceIn(0.08f, 0.34f)),
                        VColors.white.copy(alpha = (0.10f * strength).coerceIn(0.04f, 0.20f)),
                        VColors.white.copy(alpha = (0.14f * strength).coerceIn(0.06f, 0.26f)),
                    ),
                )
            } else {
                androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        VColors.white.copy(alpha = (0.76f * strength).coerceIn(0.45f, 0.90f)),
                        VColors.white.copy(alpha = (0.56f * strength).coerceIn(0.30f, 0.75f)),
                        VColors.white.copy(alpha = (0.68f * strength).coerceIn(0.38f, 0.84f)),
                    ),
                )
            }
        }

    val liquidSearchBrush: androidx.compose.ui.graphics.Brush
        get() = if (isDark) {
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    VColors.white.copy(alpha = 0.34f),
                    VColors.white.copy(alpha = 0.20f),
                    VColors.white.copy(alpha = 0.28f),
                ),
            )
        } else {
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    VColors.white.copy(alpha = 0.84f),
                    VColors.white.copy(alpha = 0.66f),
                    VColors.white.copy(alpha = 0.78f),
                ),
            )
        }

    val liquidBorderBrush: androidx.compose.ui.graphics.Brush
        get() = if (isDark) {
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    VColors.white.copy(alpha = 0.58f),
                    VColors.white.copy(alpha = 0.16f),
                    VColors.white.copy(alpha = 0.40f),
                ),
            )
        } else {
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    VColors.white.copy(alpha = 0.92f),
                    VColors.black.copy(alpha = 0.14f),
                    VColors.white.copy(alpha = 0.65f),
                ),
            )
        }

    val liquidSheetBrush: androidx.compose.ui.graphics.Brush
        get() = if (isDark) {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xC81C1A24),
                    Color(0xDE101016),
                ),
            )
        } else {
            androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    Color(0xE0F8F8FA),
                    Color(0xEDF0F2F6),
                ),
            )
        }

    fun withGlass(value: Float): VPalette = copy(glass = value.coerceIn(0f, 1f))

    companion object {
        val Dark = VPalette(isDark = true, glass = 0.5f)
        val Light = VPalette(isDark = false, glass = 0.5f)

        fun forLayout(layout: HomeLayout, systemDark: Boolean): VPalette {
            val dark = when (layout.ink) {
                InkMode.white -> true
                InkMode.black -> false
                InkMode.auto -> if (layout.wallpaper.photo != null) {
                    true
                } else {
                    !(VWallpapers.isLight(layout.wallpaper) && layout.dim < 0.3)
                }
            }
            return VPalette(isDark = dark, glass = layout.glass.toFloat().coerceIn(0f, 1f))
        }
    }
}

val LocalVPalette = staticCompositionLocalOf { VPalette.Dark }
val LocalVInk = staticCompositionLocalOf { VColors.white }

@Composable
fun VInkProvider(
    ink: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalVInk provides ink,
        LocalContentColor provides ink,
        content = content,
    )
}
