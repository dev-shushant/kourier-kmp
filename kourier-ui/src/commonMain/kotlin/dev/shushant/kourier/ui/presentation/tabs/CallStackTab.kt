package dev.shushant.kourier.ui.presentation.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.core.model.CallStackElement
import dev.shushant.kourier.core.model.HttpTransaction
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

@Composable
fun CallStackTab(
    transaction: HttpTransaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()
    val callStack = transaction.callStack

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dispatch call stack  ·  ${callStack.size} frames",
                style = KourierTypography.subtitle,
                color = colors.textSecondary
            )
            if (callStack.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val text = callStack.joinToString("\n") { it.toString() }
                        clipboard.copyText(text)
                    },
                    modifier = Modifier.height(24.dp).width(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy call stack",
                        tint = colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (callStack.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No caller frames captured for this request.",
                    style = KourierTypography.caption,
                    color = colors.textMuted
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            ) {
                callStack.forEachIndexed { index, frame ->
                    CallStackItem(
                        frame = frame,
                        onClick = {
                            clipboard.copyText(frame.toString())
                        }
                    )
                    if (index < callStack.lastIndex) {
                        Divider(color = colors.divider)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallStackItem(
    frame: CallStackElement,
    onClick: () -> Unit
) {
    val colors = LocalKourierColors.current
    val methodColor = if (frame.isAppCode) colors.methodPost else colors.textSecondary
    val classColor  = if (frame.isAppCode) colors.textPrimary else colors.textMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (frame.isAppCode) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.methodPost.copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "APP",
                    style = KourierTypography.badge,
                    color = colors.methodPost
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${frame.className}.${frame.methodName}()",
                style = KourierTypography.codeSmall,
                color = methodColor
            )
            val location = when {
                frame.fileName != null && frame.lineNumber >= 0 -> "${frame.fileName}:${frame.lineNumber}"
                frame.fileName != null -> frame.fileName
                else -> null
            }
            if (location != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = location,
                    style = KourierTypography.caption,
                    color = classColor
                )
            }
        }
    }
}
