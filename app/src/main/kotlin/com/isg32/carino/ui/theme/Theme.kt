package com.isg32.carino.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Color tokens defining the Carbon LiquidGlass Material 3 palette.
 * Translucent glass layers are paired with high-contrast foreground ink
 * and soft plum/slate/teal atmospheric accents.
 */
object LiquidGlassColors {
    // Atmospheric accents
    val MistPlum = Color(0xFF9C7FA8)
    val DeepPlum = Color(0xFF3E354B)
    val FrostTeal = Color(0xFF5FA3A8)
    val SlateIndigo = Color(0xFF564A72)
    val SoftCoral = Color(0xFFFF8A80)
    val DeepError = Color(0xFFB3261E)

    // Dark LiquidGlass semi-transparent surfaces
    val DarkBackground = Color(0xFF14111B)
    val DarkSurface = Color(0x38FFFFFF)           // ~22% frosted white
    val DarkSurfaceDim = Color(0xB812101A)        // ~72% deep smoked glass
    val DarkSurfaceBright = Color(0x4DFFFFFF)     // ~30% bright frosted glass
    val DarkContainerLowest = Color(0x1AFFFFFF)   // ~10% subtle veil
    val DarkContainerLow = Color(0x26FFFFFF)      // ~15% soft card glass
    val DarkContainer = Color(0x33FFFFFF)         // ~20% regular glass
    val DarkContainerHigh = Color(0x45FFFFFF)     // ~27% elevated glass
    val DarkContainerHighest = Color(0x57FFFFFF)  // ~34% search capsule / lens
    val DarkOutline = Color(0x66FFFFFF)           // ~40% specular rim
    val DarkOutlineVariant = Color(0x29FFFFFF)    // ~16% hairline divider
    val DarkScrim = Color(0x8A0A0810)

    // Light LiquidGlass semi-transparent surfaces
    val LightBackground = Color(0xFFECEAF2)
    val LightSurface = Color(0xADFFFFFF)          // ~68% bright frosted glass
    val LightSurfaceDim = Color(0xCCE4E1EC)       // ~80% muted frosted glass
    val LightSurfaceBright = Color(0xDBFFFFFF)    // ~86% luminous glass
    val LightContainerLowest = Color(0x73FFFFFF)  // ~45% airy veil
    val LightContainerLow = Color(0x94FFFFFF)     // ~58% soft card glass
    val LightContainer = Color(0xADFFFFFF)        // ~68% regular glass
    val LightContainerHigh = Color(0xC7FFFFFF)    // ~78% elevated glass
    val LightContainerHighest = Color(0xE0FFFFFF) // ~88% search capsule / lens
    val LightOutline = Color(0x52181524)          // ~32% crisp rim
    val LightOutlineVariant = Color(0x1F181524)   // ~12% hairline divider
    val LightScrim = Color(0x59FFFFFF)
}

/**
 * Extended LiquidGlass design tokens exposed through [LocalLiquidGlass] and [LiquidGlassTheme.tokens].
 * Captures semi-transparent surface fills, soft directional glass-morphism gradients,
 * specular border rims, and Android 12+ blur radii scaled by [glassIntensity].
 */
@Immutable
data class LiquidGlassTokens(
    val isDark: Boolean,
    val glassIntensity: Float,
    // Semi-transparent glass tints
    val surfaceSubtle: Color,
    val surfaceRegular: Color,
    val surfaceElevated: Color,
    val surfaceSheet: Color,
    val searchCapsule: Color,
    val lensHighlight: Color,
    val scrim: Color,
    // Specular & frost brushes
    val surfaceBrush: Brush,
    val elevatedSurfaceBrush: Brush,
    val searchBarBrush: Brush,
    val sheetBrush: Brush,
    val borderBrush: Brush,
    val specularSheenBrush: Brush,
    val backdropBrush: Brush,
    // Blur radii (applied on Android 12+ via RenderEffect / Modifier.blur)
    val blurSubtle: Dp,
    val blurRegular: Dp,
    val blurHeavy: Dp,
    val blurSheet: Dp,
    // Stroke & elevation metrics
    val hairlineWidth: Dp = 1.dp,
    val borderWidth: Dp = 1.15.dp,
) {
    companion object {
        fun create(isDark: Boolean, glassIntensity: Float = 0.65f): LiquidGlassTokens {
            val g = glassIntensity.coerceIn(0f, 1f)
            val strength = 0.65f + g * 0.70f

            val blurSubtle = (8f + g * 10f).dp
            val blurRegular = (14f + g * 16f).dp
            val blurHeavy = (22f + g * 22f).dp
            val blurSheet = (28f + g * 24f).dp

            return if (isDark) {
                LiquidGlassTokens(
                    isDark = true,
                    glassIntensity = g,
                    surfaceSubtle = VColors.white.copy(alpha = (0.10f * strength).coerceIn(0.05f, 0.20f)),
                    surfaceRegular = VColors.white.copy(alpha = (0.16f * strength).coerceIn(0.08f, 0.30f)),
                    surfaceElevated = VColors.white.copy(alpha = (0.24f * strength).coerceIn(0.14f, 0.40f)),
                    surfaceSheet = Color(0xD4161420),
                    searchCapsule = VColors.white.copy(alpha = (0.28f * strength).coerceIn(0.18f, 0.42f)),
                    lensHighlight = VColors.white.copy(alpha = 0.42f),
                    scrim = LiquidGlassColors.DarkScrim,
                    surfaceBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = (0.22f * strength).coerceIn(0.10f, 0.36f)),
                            VColors.white.copy(alpha = (0.10f * strength).coerceIn(0.04f, 0.20f)),
                            VColors.white.copy(alpha = (0.15f * strength).coerceIn(0.06f, 0.26f)),
                        ),
                    ),
                    elevatedSurfaceBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = (0.30f * strength).coerceIn(0.16f, 0.44f)),
                            VColors.white.copy(alpha = (0.16f * strength).coerceIn(0.08f, 0.28f)),
                            VColors.white.copy(alpha = (0.22f * strength).coerceIn(0.12f, 0.34f)),
                        ),
                    ),
                    searchBarBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = 0.36f),
                            VColors.white.copy(alpha = 0.20f),
                            VColors.white.copy(alpha = 0.28f),
                        ),
                    ),
                    sheetBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC1E1B28),
                            Color(0xE6111018),
                        ),
                    ),
                    borderBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = 0.62f),
                            VColors.white.copy(alpha = 0.14f),
                            VColors.white.copy(alpha = 0.42f),
                        ),
                    ),
                    specularSheenBrush = Brush.verticalGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = 0.24f),
                            VColors.white.copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                    ),
                    backdropBrush = Brush.linearGradient(
                        colors = listOf(
                            LiquidGlassColors.MistPlum,
                            LiquidGlassColors.DeepPlum,
                            LiquidGlassColors.FrostTeal,
                        ),
                    ),
                    blurSubtle = blurSubtle,
                    blurRegular = blurRegular,
                    blurHeavy = blurHeavy,
                    blurSheet = blurSheet,
                )
            } else {
                LiquidGlassTokens(
                    isDark = false,
                    glassIntensity = g,
                    surfaceSubtle = VColors.white.copy(alpha = (0.50f * strength).coerceIn(0.32f, 0.70f)),
                    surfaceRegular = VColors.white.copy(alpha = (0.68f * strength).coerceIn(0.45f, 0.84f)),
                    surfaceElevated = VColors.white.copy(alpha = (0.80f * strength).coerceIn(0.58f, 0.92f)),
                    surfaceSheet = Color(0xE8F6F5FA),
                    searchCapsule = VColors.white.copy(alpha = (0.82f * strength).coerceIn(0.62f, 0.92f)),
                    lensHighlight = VColors.white.copy(alpha = 0.90f),
                    scrim = LiquidGlassColors.LightScrim,
                    surfaceBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = (0.78f * strength).coerceIn(0.48f, 0.92f)),
                            VColors.white.copy(alpha = (0.56f * strength).coerceIn(0.32f, 0.76f)),
                            VColors.white.copy(alpha = (0.68f * strength).coerceIn(0.40f, 0.84f)),
                        ),
                    ),
                    elevatedSurfaceBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = (0.88f * strength).coerceIn(0.60f, 0.96f)),
                            VColors.white.copy(alpha = (0.68f * strength).coerceIn(0.44f, 0.84f)),
                            VColors.white.copy(alpha = (0.78f * strength).coerceIn(0.52f, 0.90f)),
                        ),
                    ),
                    searchBarBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = 0.86f),
                            VColors.white.copy(alpha = 0.66f),
                            VColors.white.copy(alpha = 0.78f),
                        ),
                    ),
                    sheetBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xE4FAF9FC),
                            Color(0xF0EFEFF5),
                        ),
                    ),
                    borderBrush = Brush.linearGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = 0.95f),
                            VColors.black.copy(alpha = 0.14f),
                            VColors.white.copy(alpha = 0.68f),
                        ),
                    ),
                    specularSheenBrush = Brush.verticalGradient(
                        colors = listOf(
                            VColors.white.copy(alpha = 0.55f),
                            VColors.white.copy(alpha = 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    backdropBrush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF3EDF7),
                            Color(0xFFE7E4F0),
                            Color(0xFFDCEEEF),
                        ),
                    ),
                    blurSubtle = blurSubtle,
                    blurRegular = blurRegular,
                    blurHeavy = blurHeavy,
                    blurSheet = blurSheet,
                )
            }
        }
    }
}

val LocalLiquidGlass = staticCompositionLocalOf {
    LiquidGlassTokens.create(isDark = true, glassIntensity = 0.65f)
}

/**
 * Material 3 Dark ColorScheme built with semi-transparent glass surfaces.
 */
fun liquidGlassDarkColorScheme(tokens: LiquidGlassTokens = LiquidGlassTokens.create(isDark = true)): ColorScheme =
    darkColorScheme(
        primary = VColors.white,
        onPrimary = VColors.black,
        primaryContainer = tokens.surfaceElevated,
        onPrimaryContainer = VColors.white,
        secondary = LiquidGlassColors.MistPlum,
        onSecondary = VColors.white,
        secondaryContainer = tokens.surfaceRegular,
        onSecondaryContainer = VColors.white,
        tertiary = LiquidGlassColors.FrostTeal,
        onTertiary = VColors.black,
        tertiaryContainer = LiquidGlassColors.FrostTeal.copy(alpha = 0.24f),
        onTertiaryContainer = VColors.white,
        background = LiquidGlassColors.DarkBackground,
        onBackground = VColors.white,
        surface = tokens.surfaceRegular,
        onSurface = VColors.white,
        surfaceVariant = tokens.surfaceSubtle,
        onSurfaceVariant = VColors.white.copy(alpha = 0.72f),
        surfaceTint = VColors.white.copy(alpha = 0.12f),
        surfaceDim = LiquidGlassColors.DarkSurfaceDim,
        surfaceBright = LiquidGlassColors.DarkSurfaceBright,
        surfaceContainerLowest = LiquidGlassColors.DarkContainerLowest,
        surfaceContainerLow = LiquidGlassColors.DarkContainerLow,
        surfaceContainer = LiquidGlassColors.DarkContainer,
        surfaceContainerHigh = LiquidGlassColors.DarkContainerHigh,
        surfaceContainerHighest = LiquidGlassColors.DarkContainerHighest,
        inverseSurface = VColors.white.copy(alpha = 0.92f),
        inverseOnSurface = VColors.black,
        inversePrimary = LiquidGlassColors.DeepPlum,
        outline = LiquidGlassColors.DarkOutline,
        outlineVariant = LiquidGlassColors.DarkOutlineVariant,
        scrim = tokens.scrim,
        error = LiquidGlassColors.SoftCoral,
        onError = VColors.black,
        errorContainer = LiquidGlassColors.SoftCoral.copy(alpha = 0.22f),
        onErrorContainer = VColors.white,
    )

/**
 * Material 3 Light ColorScheme built with luminous frosted semi-transparent surfaces.
 */
fun liquidGlassLightColorScheme(tokens: LiquidGlassTokens = LiquidGlassTokens.create(isDark = false)): ColorScheme =
    lightColorScheme(
        primary = VColors.black,
        onPrimary = VColors.white,
        primaryContainer = tokens.surfaceElevated,
        onPrimaryContainer = VColors.black,
        secondary = LiquidGlassColors.SlateIndigo,
        onSecondary = VColors.white,
        secondaryContainer = tokens.surfaceRegular,
        onSecondaryContainer = VColors.black,
        tertiary = LiquidGlassColors.FrostTeal,
        onTertiary = VColors.white,
        tertiaryContainer = LiquidGlassColors.FrostTeal.copy(alpha = 0.20f),
        onTertiaryContainer = VColors.black,
        background = LiquidGlassColors.LightBackground,
        onBackground = VColors.black,
        surface = tokens.surfaceRegular,
        onSurface = VColors.black,
        surfaceVariant = tokens.surfaceSubtle,
        onSurfaceVariant = VColors.black.copy(alpha = 0.68f),
        surfaceTint = VColors.white.copy(alpha = 0.25f),
        surfaceDim = LiquidGlassColors.LightSurfaceDim,
        surfaceBright = LiquidGlassColors.LightSurfaceBright,
        surfaceContainerLowest = LiquidGlassColors.LightContainerLowest,
        surfaceContainerLow = LiquidGlassColors.LightContainerLow,
        surfaceContainer = LiquidGlassColors.LightContainer,
        surfaceContainerHigh = LiquidGlassColors.LightContainerHigh,
        surfaceContainerHighest = LiquidGlassColors.LightContainerHighest,
        inverseSurface = Color(0xE6181622),
        inverseOnSurface = VColors.white,
        inversePrimary = LiquidGlassColors.MistPlum,
        outline = LiquidGlassColors.LightOutline,
        outlineVariant = LiquidGlassColors.LightOutlineVariant,
        scrim = tokens.scrim,
        error = LiquidGlassColors.DeepError,
        onError = VColors.white,
        errorContainer = LiquidGlassColors.DeepError.copy(alpha = 0.14f),
        onErrorContainer = LiquidGlassColors.DeepError,
    )

/**
 * Soft, fluid rounded shapes matching the LiquidGlass aesthetic.
 */
val LiquidGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(VRadius.card),
    large = RoundedCornerShape(VRadius.sheet),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Material 3 Typography mapping Carbon's bundled Doto display and Inter UI variable fonts.
 */
val LiquidGlassTypography: Typography by lazy {
    Typography(
        displayLarge = VText.doto(52f, weight = 700, lineHeight = 56f),
        displayMedium = VText.doto(40f, weight = 700, lineHeight = 44f),
        displaySmall = VText.title,
        headlineLarge = VText.doto(28f, weight = 700, lineHeight = 32f),
        headlineMedium = VText.inter(22f, weight = 600, lineHeight = 28f),
        headlineSmall = VText.inter(18f, weight = 600, lineHeight = 24f),
        titleLarge = VText.inter(18f, weight = 600, lineHeight = 24f),
        titleMedium = VText.rowTitle,
        titleSmall = VText.inter(14f, weight = 600, lineHeight = 20f),
        bodyLarge = VText.inter(15f, weight = 400, lineHeight = 22f),
        bodyMedium = VText.body,
        bodySmall = VText.secondary,
        labelLarge = VText.button,
        labelMedium = VText.chip,
        labelSmall = VText.eyebrow,
    )
}

/**
 * Convenience accessor for LiquidGlass tokens inside Compose hierarchies.
 */
object LiquidGlassTheme {
    val tokens: LiquidGlassTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalLiquidGlass.current
}

/**
 * Root Material 3 LiquidGlass theme.
 *
 * Provides semi-transparent glass [ColorScheme] tokens, [LiquidGlassTypography], [LiquidGlassShapes],
 * [LocalLiquidGlass] extended frost/blur/specular tokens, and Carbon's [LocalVPalette] / [LocalVInk].
 *
 * @param darkTheme Whether dark LiquidGlass styling is active.
 * @param glassIntensity Frost/blur strength in `[0f, 1f]` (default `0.65f`).
 * @param dynamicColor When true on Android 12+, blends system Material You accents into the LiquidGlass scheme.
 */
@Composable
fun LiquidGlassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    glassIntensity: Float = 0.65f,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = remember(darkTheme, glassIntensity) {
        VPalette(isDark = darkTheme, glass = glassIntensity.coerceIn(0f, 1f))
    }
    val glassTokens = remember(darkTheme, glassIntensity) {
        LiquidGlassTokens.create(isDark = darkTheme, glassIntensity = glassIntensity)
    }

    val context = LocalContext.current
    val colorScheme = remember(darkTheme, glassTokens, dynamicColor, context) {
        val base = if (darkTheme) {
            liquidGlassDarkColorScheme(glassTokens)
        } else {
            liquidGlassLightColorScheme(glassTokens)
        }
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamic = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            base.copy(
                primary = dynamic.primary,
                onPrimary = dynamic.onPrimary,
                secondary = dynamic.secondary,
                onSecondary = dynamic.onSecondary,
                tertiary = dynamic.tertiary,
                onTertiary = dynamic.onTertiary,
            )
        } else {
            base
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LiquidGlassTypography,
        shapes = LiquidGlassShapes,
    ) {
        CompositionLocalProvider(
            LocalLiquidGlass provides glassTokens,
            LocalVPalette provides palette,
            LocalVInk provides palette.ink,
            LocalContentColor provides palette.ink,
            content = content,
        )
    }
}

/**
 * Carbon launcher theme entry point backed by [LiquidGlassTheme].
 */
@Composable
fun CarinoTheme(
    palette: VPalette = VPalette(isDark = isSystemInDarkTheme(), glass = 0.65f),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    LiquidGlassTheme(
        darkTheme = palette.isDark,
        glassIntensity = palette.glass,
        dynamicColor = dynamicColor,
        content = content,
    )
}

/**
 * Applies a hardware-accelerated Gaussian blur on Android 12+ (API 31+) while degrading gracefully
 * on older API levels (API 26–30) where RenderEffect is unavailable.
 */
fun Modifier.liquidGlassBlur(
    radius: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Unbounded,
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && radius > 0.dp) {
    this.blur(radius = radius, edgeTreatment = edgeTreatment)
} else {
    this
}

/**
 * Styles any composable with a soft LiquidGlass surface: clipped shape, semi-transparent directional
 * frost gradient, subtle top specular sheen, and luminous refractive glass rim border.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(VRadius.card),
    elevated: Boolean = false,
    borderWidth: Dp? = null,
    showSpecularSheen: Boolean = true,
): Modifier = composed {
    val tokens = LiquidGlassTheme.tokens
    val fillBrush = if (elevated) tokens.elevatedSurfaceBrush else tokens.surfaceBrush
    val strokeWidth = borderWidth ?: tokens.borderWidth
    val darkBacking = if (tokens.isDark) Color(0x28120F1E) else Color(0x18FFFFFF)

    this
        .clip(shape)
        .background(darkBacking)
        .background(fillBrush)
        .then(
            if (showSpecularSheen) {
                Modifier.drawWithContent {
                    drawContent()
                    // Luminous top-edge glass meniscus reflection
                    drawRect(
                        brush = tokens.specularSheenBrush,
                        topLeft = Offset.Zero,
                        size = size.copy(height = size.height * 0.44f),
                    )
                    // Subtle bottom internal refraction bounce
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.24f),
                            ),
                            startY = size.height * 0.72f,
                            endY = size.height,
                        ),
                        topLeft = Offset(0f, size.height * 0.72f),
                        size = size.copy(height = size.height * 0.28f),
                    )
                }
            } else {
                Modifier
            },
        )
        .border(strokeWidth, tokens.borderBrush, shape)
}

/**
 * High-refraction LiquidGlass capsule style designed for floating bars, search pills, and lenses.
 */
fun Modifier.liquidGlassPill(
    shape: Shape = RoundedCornerShape(VRadius.pill),
    smoked: Boolean = false,
): Modifier = composed {
    val tokens = LiquidGlassTheme.tokens
    val backing = when {
        smoked && tokens.isDark -> Color(0x66161326)
        smoked && !tokens.isDark -> Color(0x88F4F2FA)
        tokens.isDark -> Color(0x361A162B)
        else -> Color(0x45FFFFFF)
    }
    val rimBrush = if (tokens.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.72f),
                Color(0x44D6C4F0),
                Color.White.copy(alpha = 0.16f),
                Color(0x558EDCE0),
                Color.White.copy(alpha = 0.54f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0x28564A72),
                Color.White.copy(alpha = 0.75f),
            ),
        )
    }

    this
        .clip(shape)
        .background(backing)
        .background(tokens.searchBarBrush)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (tokens.isDark) 0.22f else 0.50f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.52f,
                ),
                topLeft = Offset.Zero,
                size = size.copy(height = size.height * 0.52f),
            )
        }
        .border(1.25.dp, rimBrush, shape)
}

/**
 * A ready-to-use Material 3 LiquidGlass container with semi-transparent frost gradient,
 * inner top specular sheen, and soft glass-morphism rim.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    elevated: Boolean = false,
    showSpecularSheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = shape,
            elevated = elevated,
            showSpecularSheen = showSpecularSheen,
        ),
        content = content,
    )
}
