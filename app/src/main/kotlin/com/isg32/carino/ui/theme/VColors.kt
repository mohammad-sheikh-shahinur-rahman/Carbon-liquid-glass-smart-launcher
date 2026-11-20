package com.isg32.carino.ui.theme

import androidx.compose.ui.graphics.Color
import com.isg32.carino.data.WallpaperRef

/**
 * The only colours Cariño uses (Veronica's palette).
 * Pure black and white: white ink on dark wallpapers, black ink on light ones.
 */
object VColors {
    val black = Color(0xFF000000)
    val white = Color(0xFFFFFFFF)
    val transparent = Color(0x00000000)

    val glassBar = Color(0x1AFFFFFF)
    val glassCard = Color(0x14FFFFFF)
    val glassSheet = Color(0x8C000000)

    val glassBarLight = Color(0x0F000000)
    val glassCardLight = Color(0x0D000000)
    val glassSheetLight = Color(0x99FFFFFF)

    val veil = Color(0x99000000)
    val veilLight = Color(0x99FFFFFF)

    val popupScrim = Color(0x66000000)
    val popupScrimLight = Color(0x66FFFFFF)

    const val disabledOpacity = 0.35f

    fun onColor(color: Color): Color = if (color == white) black else white
}

data class VWallpaperBlob(
    val colorInt: Int,
    val x: Float,
    val y: Float,
    val radius: Float,
) {
    val color: Color get() = Color(colorInt)
}

data class VWallpaperPalette(
    val baseInt: Int,
    val blobs: List<VWallpaperBlob>,
    val light: Boolean = false,
) {
    val base: Color get() = Color(baseInt)
}

object VWallpapers {
    val liquid = VWallpaperPalette(
        0xFF3E354B.toInt(),
        listOf(
            VWallpaperBlob(0xFF8E6E95.toInt(), -0.65f, -0.75f, 0.85f),
            VWallpaperBlob(0xFF564A72.toInt(), 0.75f, -0.25f, 0.78f),
            VWallpaperBlob(0xFF4F8E92.toInt(), 0.45f, 0.95f, 0.90f),
            VWallpaperBlob(0xFF6B5C7A.toInt(), -0.55f, 0.55f, 0.75f),
        ),
    )

    val night = VWallpaperPalette(
        0xFF06070A.toInt(),
        listOf(
            VWallpaperBlob(0xFF24344F.toInt(), -0.7f, -0.8f, 0.75f),
            VWallpaperBlob(0xFF3D2536.toInt(), 0.9f, 0.1f, 0.6f),
            VWallpaperBlob(0xFF15322F.toInt(), -0.4f, 1.1f, 0.7f),
        ),
    )

    val dune = VWallpaperPalette(
        0xFF120B07.toInt(),
        listOf(
            VWallpaperBlob(0xFF6B3B1F.toInt(), 0.6f, -0.6f, 0.8f),
            VWallpaperBlob(0xFF3A2416.toInt(), -0.8f, 0.8f, 0.65f),
        ),
    )

    val paper = VWallpaperPalette(
        0xFFEFE9E0.toInt(),
        listOf(
            VWallpaperBlob(0xFFF3E6D2.toInt(), 0.7f, -0.85f, 0.7f),
            VWallpaperBlob(0xFFDCD0C0.toInt(), -1.0f, 0.2f, 0.65f),
            VWallpaperBlob(0xFFCDBFA9.toInt(), 0.4f, 1.1f, 0.6f),
        ),
        light = true,
    )

    val moss = VWallpaperPalette(
        0xFF060A07.toInt(),
        listOf(
            VWallpaperBlob(0xFF1F3A26.toInt(), -0.6f, 0.8f, 0.7f),
            VWallpaperBlob(0xFF2C3B2F.toInt(), 0.8f, -0.8f, 0.6f),
        ),
    )

    val fog = VWallpaperPalette(
        0xFF1A1D22.toInt(),
        listOf(
            VWallpaperBlob(0xFF5B6470.toInt(), 0.0f, -0.4f, 0.8f),
        ),
    )

    val ember = VWallpaperPalette(
        0xFF0B0505.toInt(),
        listOf(
            VWallpaperBlob(0xFF5A1A1A.toInt(), -0.4f, 0.6f, 0.6f),
        ),
    )

    val black = VWallpaperPalette(0xFF000000.toInt(), emptyList())

    val white = VWallpaperPalette(0xFFFFFFFF.toInt(), emptyList(), light = true)

    val all: Map<String, VWallpaperPalette> = linkedMapOf(
        "liquid" to liquid,
        "night" to night,
        "dune" to dune,
        "paper" to paper,
        "moss" to moss,
        "fog" to fog,
        "ember" to ember,
        "black" to black,
        "white" to white,
    )

    fun isLight(ref: WallpaperRef): Boolean =
        ref.photo == null && (all[ref.builtin]?.light == true)
}
