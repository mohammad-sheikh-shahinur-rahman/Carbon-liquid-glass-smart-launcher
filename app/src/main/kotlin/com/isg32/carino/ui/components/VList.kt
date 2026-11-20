package com.isg32.carino.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VIcon
import com.isg32.carino.ui.theme.VIconView
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText

@Composable
fun VListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    VSurface(modifier = modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
fun VHairlineDivider(modifier: Modifier = Modifier) {
    val ink = LocalVInk.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = VSpace.rowHorizontal)
            .height(VSpace.hairline)
            .background(ink.copy(alpha = 0.12f)),
    )
}

@Composable
fun VListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    tag: String? = null,
) {
    val ink = LocalVInk.current
    Row(
        modifier = modifier
            .let { if (tag != null) it.testTag(tag) else it }
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = VSpace.rowHorizontal, vertical = VSpace.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = VText.rowTitle.copy(color = ink),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun VToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: VIcon? = null,
    tag: String? = null,
) {
    VListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leading = icon?.let { { VIconView(icon = it, size = 20.dp) } },
        trailing = {
            VSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                tag = tag?.let { "${it}_switch" },
            )
        },
        onClick = { onCheckedChange(!checked) },
        tag = tag,
    )
}

@Composable
fun VDestinationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: VIcon? = null,
    tag: String? = null,
) {
    val ink = LocalVInk.current
    VListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leading = icon?.let { { VIconView(icon = it, size = 20.dp) } },
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        style = VText.secondary.copy(color = ink.copy(alpha = 0.65f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                VIconView(icon = VIcon.chevron, size = 16.dp, color = ink.copy(alpha = 0.55f))
            }
        },
        onClick = onClick,
        tag = tag,
    )
}

@Composable
fun VActionRow(
    title: String,
    icon: VIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tag: String? = null,
) {
    VListRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leading = { VIconView(icon = icon, size = 20.dp) },
        onClick = onClick,
        tag = tag,
    )
}
