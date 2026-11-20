package com.isg32.carino.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.LocalVPalette
import com.isg32.carino.ui.theme.VColors
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import kotlinx.coroutines.delay

@Composable
fun CarinoSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val pal = LocalVPalette.current
    val shape = RoundedCornerShape(topStart = VRadius.sheet, topEnd = VRadius.sheet)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pal.popupScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(pal.liquidSheetBrush)
                .border(1.1.dp, pal.liquidBorderBrush, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(pal.ink.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun CarinoPopupDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val pal = LocalVPalette.current
    val shape = RoundedCornerShape(VRadius.card)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pal.popupScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(pal.liquidSheetBrush)
                .border(1.1.dp, pal.liquidBorderBrush, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(22.dp),
            content = content,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = "Cancel",
) {
    val ink = LocalVInk.current
    CarinoPopupDialog(onDismiss = onDismiss) {
        Text(text = title, style = VText.doto(22f, weight = 700).copy(color = ink))
        Spacer(Modifier.height(10.dp))
        Text(text = body, style = VText.body.copy(color = ink.copy(alpha = 0.78f)))
        Spacer(Modifier.height(20.dp))
        VButton(label = confirmLabel, onClick = onConfirm, primary = true, tag = "confirm_dialog_yes")
        Spacer(Modifier.height(8.dp))
        VButton(label = cancelLabel, onClick = onDismiss, primary = false, tag = "confirm_dialog_no")
    }
}

@Composable
fun DotLoader(
    modifier: Modifier = Modifier,
    dotSize: Int = 7,
) {
    val ink = LocalVInk.current
    val transition = rememberInfiniteTransition(label = "dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dot_phase",
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val d = kotlin.math.abs(phase - i).coerceAtMost(1f)
            val a = 0.25f + (1f - d) * 0.75f
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .alpha(a)
                    .clip(CircleShape)
                    .background(ink),
            )
        }
    }
}

@Composable
fun ToastHost(
    message: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(message) {
        if (message != null) {
            delay(2600L)
            onDone()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 88.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val ink = LocalVInk.current
            val bg = ink
            val fg = VColors.onColor(ink)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(VRadius.pill))
                    .background(bg)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.orEmpty(),
                    style = VText.body.copy(color = fg),
                )
            }
        }
    }
}
