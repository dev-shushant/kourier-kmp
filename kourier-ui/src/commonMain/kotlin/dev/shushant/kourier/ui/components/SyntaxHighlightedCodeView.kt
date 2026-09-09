package dev.shushant.kourier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.shushant.kourier.ui.theme.KourierColorScheme
import dev.shushant.kourier.ui.theme.KourierTypography
import dev.shushant.kourier.ui.theme.LocalKourierColors
import dev.shushant.kourier.ui.utils.rememberKourierClipboard

@Composable
fun SyntaxHighlightedCodeView(
    rawCode: String?,
    contentType: String? = "application/json",
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalKourierColors.current
    val clipboard = rememberKourierClipboard()
    val code = rawCode ?: "/* No body content */"
    val isLargePayload = code.length > 100_000

    val prettyCode = remember(code, isLargePayload) {
        if (isLargePayload) code else formatCode(code, contentType)
    }
    val annotatedString = remember(prettyCode, contentType, colors, isLargePayload) {
        if (isLargePayload) {
            AnnotatedString(prettyCode)
        } else {
            highlightSyntax(prettyCode, contentType, colors)
        }
    }
    val lineCount = remember(prettyCode) {
        prettyCode.count { it == '\n' } + 1
    }
    val lineNumbersText = remember(lineCount) {
        val sb = StringBuilder()
        for (i in 1..lineCount) {
            sb.append(i).append('\n')
        }
        sb.toString()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.codeBackground)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
    ) {
        Column {
            // ── Header bar ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contentType?.substringBefore(";") ?: "text/plain",
                    style = KourierTypography.caption,
                    color = colors.textSecondary
                )
                if (isLargePayload) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${code.length / 1024} KB (Plain View)",
                        style = KourierTypography.caption,
                        color = colors.methodPut
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$lineCount lines",
                    style = KourierTypography.caption,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        clipboard.copyText(prettyCode)
                        onCopy?.invoke()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Thin border line between header and code
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

            // ── Code content with line numbers ─────────────────────────────
            SelectionContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Line numbers rendered as a single efficient Text composable
                    Text(
                        text = lineNumbersText,
                        style = KourierTypography.codeSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    // Highlighted code
                    Text(text = annotatedString, style = KourierTypography.code)
                }
            }
        }
    }
}

/** Pretty-formats JSON strings. */
private fun formatCode(raw: String, contentType: String?): String {
    val isJson = contentType?.contains("json", ignoreCase = true) == true &&
            (raw.trim().startsWith("{") || raw.trim().startsWith("["))
    if (!isJson) return raw
    return try {
        val indent = "  "
        val sb = StringBuilder()
        var indentLevel = 0
        var inQuotes = false
        for (i in raw.indices) {
            val c = raw[i]
            when (c) {
                '"'      -> { sb.append(c); if (i == 0 || raw[i - 1] != '\\') inQuotes = !inQuotes }
                '{', '[' -> { sb.append(c); if (!inQuotes) { sb.append("\n"); indentLevel++; sb.append(indent.repeat(indentLevel)) } }
                '}', ']' -> { if (!inQuotes) { sb.append("\n"); indentLevel = (indentLevel - 1).coerceAtLeast(0); sb.append(indent.repeat(indentLevel)) }; sb.append(c) }
                ','      -> { sb.append(c); if (!inQuotes) { sb.append("\n"); sb.append(indent.repeat(indentLevel)) } }
                ':'      -> { sb.append(c); if (!inQuotes) sb.append(" ") }
                else     -> sb.append(c)
            }
        }
        sb.toString()
    } catch (_: Exception) { raw }
}

/** Tokenizes and applies syntax colors using theme-aware scheme. */
private fun highlightSyntax(code: String, contentType: String?, colors: KourierColorScheme): AnnotatedString {
    return buildAnnotatedString {
        if (contentType?.contains("json", ignoreCase = true) == true) {
            val regex = Regex("""("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)""")
            var lastIndex = 0
            regex.findAll(code).forEach { match ->
                val range = match.range
                if (range.first > lastIndex) {
                    withStyle(SpanStyle(color = colors.syntaxPunctuation)) {
                        append(code.substring(lastIndex, range.first))
                    }
                }
                val token = match.value
                val style = when {
                    token.endsWith(":")                      -> SpanStyle(color = colors.syntaxKey)
                    token.startsWith("\"")                   -> SpanStyle(color = colors.syntaxString)
                    token == "true" || token == "false"      -> SpanStyle(color = colors.syntaxBoolean)
                    token == "null"                          -> SpanStyle(color = colors.statusServerError)
                    else                                     -> SpanStyle(color = colors.syntaxNumber)
                }
                withStyle(style) { append(token) }
                lastIndex = range.last + 1
            }
            if (lastIndex < code.length) {
                withStyle(SpanStyle(color = colors.syntaxPunctuation)) {
                    append(code.substring(lastIndex))
                }
            }
        } else {
            withStyle(SpanStyle(color = colors.textPrimary)) { append(code) }
        }
    }
}
