package com.isg32.carino.ui.features

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.isg32.carino.data.CarinoViewModel
import com.isg32.carino.platform.CrashFiles
import com.isg32.carino.platform.CrashReport
import com.isg32.carino.ui.components.CarinoPopupDialog
import com.isg32.carino.ui.components.VButton
import com.isg32.carino.ui.theme.LocalVInk
import com.isg32.carino.ui.theme.VRadius
import com.isg32.carino.ui.theme.VSpace
import com.isg32.carino.ui.theme.VText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CrashOverlay(
    report: CrashReport,
    vm: CarinoViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val ink = LocalVInk.current

    CarinoPopupDialog(onDismiss = onDismiss) {
        Text(
            text = report.title,
            style = VText.doto(22f, weight = 700).copy(color = ink),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(VRadius.chip))
                .background(ink.copy(alpha = 0.06f))
                .border(VSpace.hairline, ink.copy(alpha = 0.2f), RoundedCornerShape(VRadius.chip))
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
        ) {
            Text(
                text = report.body,
                style = VText.mono.copy(color = ink.copy(alpha = 0.88f)),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VButton(
                label = "Copy log",
                primary = false,
                modifier = Modifier.weight(1f),
                tag = "crash_copy_button",
                onClick = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("Carbon crash log", report.body))
                    vm.showToast("Copied to clipboard")
                },
            )
            VButton(
                label = "Save",
                primary = true,
                modifier = Modifier.weight(1f),
                tag = "crash_save_button",
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                    runCatching {
                        CrashFiles.saveToDownloads(context, "carbon-crash-$stamp.txt", report.body)
                    }.onSuccess { path ->
                        vm.showToast("Saved to $path")
                    }.onFailure { err ->
                        vm.showToast(err.message ?: "Could not save log")
                    }
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        VButton(
            label = "Close",
            primary = false,
            tag = "crash_close_button",
            onClick = onDismiss,
        )
    }
}
