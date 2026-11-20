package com.isg32.carino.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.isg32.carino.R

/**
 * Type scale from Veronica's design system:
 * Doto for titles, numbers and times; Inter for everything else.
 * Both are bundled as variable .ttf fonts in res/font/.
 */
@OptIn(ExperimentalTextApi::class)
object VText {

    private fun dotoFamily(weight: Int): FontFamily = FontFamily(
        Font(
            resId = R.font.doto,
            weight = FontWeight(weight.coerceIn(100, 900)),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )

    private fun interFamily(weight: Int): FontFamily = FontFamily(
        Font(
            resId = R.font.inter,
            weight = FontWeight(weight.coerceIn(100, 900)),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        ),
    )

    fun doto(
        size: Float,
        weight: Int = 700,
        lineHeight: Float? = null,
        letterSpacing: Float? = null,
    ): TextStyle = TextStyle(
        fontFamily = dotoFamily(weight),
        fontSize = size.sp,
        fontWeight = FontWeight(weight.coerceIn(100, 900)),
        lineHeight = lineHeight?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = letterSpacing?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
    )

    fun inter(
        size: Float,
        weight: Int = 400,
        lineHeight: Float? = null,
        letterSpacing: Float? = null,
    ): TextStyle = TextStyle(
        fontFamily = interFamily(weight),
        fontSize = size.sp,
        fontWeight = FontWeight(weight.coerceIn(100, 900)),
        lineHeight = lineHeight?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        letterSpacing = letterSpacing?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
    )

    val title: TextStyle = doto(34f, weight = 700, lineHeight = 36f)
    val eyebrow: TextStyle = inter(11f, weight = 500, letterSpacing = 1.1f)
    val body: TextStyle = inter(14f, weight = 400)
    val rowTitle: TextStyle = inter(15f, weight = 500)
    val secondary: TextStyle = inter(12f, weight = 300, lineHeight = 16f)
    val footnote: TextStyle = inter(12f, weight = 300, lineHeight = 18f)
    val button: TextStyle = inter(16f, weight = 600)
    val buttonSecondary: TextStyle = inter(15f, weight = 500)
    val chip: TextStyle = inter(11f, weight = 500)
    val mono: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.5f.sp,
        lineHeight = 17f.sp,
    )
}
