package com.isg32.carino.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText

@Composable
fun VTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val ink = LocalVInk.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VSpace.titleGutter, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                VIconButton(
                    icon = VIcon.back,
                    contentDescription = "Back",
                    onClick = onBack,
                    outlined = true,
                    tag = "header_back_button",
                )
                Spacer(Modifier.weight(1f))
            } else if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow.uppercase(),
                    style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (trailing != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = trailing,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = VText.title.copy(color = ink),
        )
    }
}

@Composable
fun VCloseHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val ink = LocalVInk.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VSpace.titleGutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = VText.title.copy(color = ink),
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        }
        VIconButton(
            icon = VIcon.close,
            contentDescription = "Close",
            onClick = onClose,
            outlined = true,
            tag = "header_close_button",
        )
    }
}

@Composable
fun VSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val ink = LocalVInk.current
    Text(
        text = text.uppercase(),
        style = VText.eyebrow.copy(color = ink.copy(alpha = 0.6f)),
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
fun VSectionFootnote(
    text: String,
    modifier: Modifier = Modifier,
) {
    val ink = LocalVInk.current
    Text(
        text = text,
        style = VText.footnote.copy(color = ink.copy(alpha = 0.6f)),
        modifier = modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
fun VScrollPage(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = VSpace.gutter)
            .padding(bottom = 32.dp),
        content = content,
    )
}
