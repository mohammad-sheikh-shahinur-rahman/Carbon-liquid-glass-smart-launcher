package com.isg32.carino.ui.common

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.data.WallpaperRef
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VMotion
import com.isg32.carino.ui.theme.VWallpaperPalette
import com.isg32.carino.ui.theme.VWallpapers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

@Composable
fun WallpaperView(
    wallpaper: WallpaperRef,
    vm: CarinoViewModel,
    modifier: Modifier = Modifier,
    dim: Double = 0.0,
    blur: Double = 0.0,
    soft: Boolean = false,
) {
    val clampedDim = dim.coerceIn(0.0, 0.8).toFloat()
    val stateKey = "${wallpaper.builtin}|${wallpaper.photo}|$soft|$blur"

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = stateKey,
            animationSpec = tween(VMotion.slowMs),
            label = "wallpaper_fade",
        ) { _ ->
            val photo = wallpaper.photo
            if (photo == null) {
                val palette = VWallpapers.all[wallpaper.builtin] ?: VWallpapers.night
                WallpaperPaletteCanvas(palette = palette)
            } else {
                PhotoWallpaperLayer(
                    photo = photo,
                    blur = if (soft) 0.0 else blur,
                    soft = soft,
                    vm = vm,
                )
            }
        }
        if (clampedDim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VColors.black.copy(alpha = clampedDim)),
            )
        }
    }
}

@Composable
fun WallpaperPaletteCanvas(
    palette: VWallpaperPalette,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = palette.base)
        val longer = max(size.width, size.height)
        for (b in palette.blobs) {
            val cx = size.width / 2f + b.x * size.width / 2f
            val cy = size.height / 2f + b.y * size.height / 2f
            val r = (b.radius * longer).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        b.color.copy(alpha = 0.88f),
                        b.color.copy(alpha = 0.34f),
                        b.color.copy(alpha = 0f),
                    ),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }
        // Subtle diagonal liquid-crystal light caustics for glossy depth behind frosted glass
        if (!palette.light && palette.blobs.isNotEmpty()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent,
                        Color(0x2284D4DE),
                        Color.Transparent,
                        Color(0x1AC8A4E0),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
        }
    }
}

@Composable
private fun PhotoWallpaperLayer(
    photo: String,
    blur: Double,
    soft: Boolean,
    vm: CarinoViewModel,
) {
    val repo = vm.repo
    val bitmap by produceState<Bitmap?>(initialValue = null, photo, blur, soft) {
        value = withContext(Dispatchers.IO) {
            if (repo == null) return@withContext null
            if (soft) {
                repo.decodeSampledBitmap(photo, 120, 200)
            } else if (blur > 0.01 && repo.blurReady(photo, blur)) {
                repo.decodeSampledBitmap(repo.bakedPath(photo, blur), 720, 1280)
            } else {
                if (blur > 0.01 && File(photo).exists()) {
                    repo.bakeBlur(photo, blur)
                    val baked = repo.bakedPath(photo, blur)
                    if (File(baked).exists()) {
                        return@withContext repo.decodeSampledBitmap(baked, 720, 1280)
                    }
                }
                repo.decodeSampledBitmap(photo, 1080, 1920)
            }
        }
    }

    val bmp = bitmap
    if (bmp == null) {
        WallpaperPaletteCanvas(palette = VWallpapers.night)
    } else {
        val blurMod = if (soft || (blur > 0.01 && repo?.blurReady(photo, blur) != true)) {
            val rDp = if (soft) 22.dp else (blur * 28.0).dp
            if (Build.VERSION.SDK_INT >= 31) Modifier.blur(rDp) else Modifier
        } else {
            Modifier
        }
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(blurMod),
        )
    }
}

/**
 * Full-screen blurred and veiled backdrop used on Apps, Widgets, and full pages.
 */
@Composable
fun CarinoBackdrop(
    vm: CarinoViewModel,
    wallpaper: WallpaperRef,
    dim: Double,
    blur: Double,
    veiled: Boolean = true,
    soft: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val pal = LocalVPalette.current
    Box(modifier = modifier.fillMaxSize()) {
        WallpaperView(
            wallpaper = wallpaper,
            vm = vm,
            dim = dim,
            blur = blur,
            soft = soft,
        )
        if (veiled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pal.veil),
            )
        }
    }
}
