package com.vedica.labs.ind.app.chat.openmodels.ui.chat

// Pure Compose markdown renderer using AnnotatedString + ClickableText.
// Chosen over library alternatives (Markwon, Coil-based markdown) to avoid
// dependency conflicts and keep full control over styling in our dark theme.
// Supports: **bold**, *italic*, ~~strikethrough~~, `code`, [links](url).

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.NeonCyan

private const val TAG_URL = "url"

private data class MarkdownToken(
    val text: String,
    val style: SpanStyle? = null,
    val url: String? = null
)

private fun tokenizeMarkdown(
    text: String,
    inlineCodeBg: Color
): List<MarkdownToken> {
    val tokens = mutableListOf<MarkdownToken>()
    var remaining = text

    while (remaining.isNotEmpty()) {
        val pattern = findNextPattern(remaining, inlineCodeBg)
        if (pattern == null) {
            tokens.add(MarkdownToken(remaining))
            break
        }

        if (pattern.start > 0) {
            tokens.add(MarkdownToken(remaining.substring(0, pattern.start)))
        }

        tokens.add(pattern.token)
        remaining = remaining.substring(pattern.end)
    }

    return tokens
}

private data class PatternMatch(
    val start: Int,
    val end: Int,
    val token: MarkdownToken
)

private fun findNextPattern(text: String, inlineCodeBg: Color): PatternMatch? {
    var best: PatternMatch? = null

    // bold **text**
    val boldRe = Regex("\\*\\*(.+?)\\*\\*")
    val boldMatch = boldRe.find(text)
    if (boldMatch != null) {
        best = PatternMatch(boldMatch.range.first, boldMatch.range.last + 1,
            MarkdownToken(boldMatch.groupValues[1], SpanStyle(fontWeight = FontWeight.Bold)))
    }

    // italic *text* (not **)
    val italicRe = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")
    val italicMatch = italicRe.find(text)
    if (italicMatch != null && (best == null || italicMatch.range.first < best.start)) {
        best = PatternMatch(italicMatch.range.first, italicMatch.range.last + 1,
            MarkdownToken(italicMatch.groupValues[1], SpanStyle(fontStyle = FontStyle.Italic)))
    }

    // strikethrough ~~text~~
    val strikeRe = Regex("~~(.+?)~~")
    val strikeMatch = strikeRe.find(text)
    if (strikeMatch != null && (best == null || strikeMatch.range.first < best.start)) {
        best = PatternMatch(strikeMatch.range.first, strikeMatch.range.last + 1,
            MarkdownToken(strikeMatch.groupValues[1], SpanStyle(textDecoration = TextDecoration.LineThrough)))
    }

    // inline code `text`
    val codeRe = Regex("`([^`]+)`")
    val codeMatch = codeRe.find(text)
    if (codeMatch != null && (best == null || codeMatch.range.first < best.start)) {
        val codeSpanStyle = SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = inlineCodeBg,
            color = NeonCyan
        )
        best = PatternMatch(codeMatch.range.first, codeMatch.range.last + 1,
            MarkdownToken(codeMatch.groupValues[1], codeSpanStyle))
    }

    // link [text](url)
    val linkRe = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    val linkMatch = linkRe.find(text)
    if (linkMatch != null && (best == null || linkMatch.range.first < best.start)) {
        best = PatternMatch(linkMatch.range.first, linkMatch.range.last + 1,
            MarkdownToken(linkMatch.groupValues[1], url = linkMatch.groupValues[2]))
    }

    return best
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = NeonCyan
) {
    val context = LocalContext.current
    val inlineCodeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val tokens = remember(text, inlineCodeBg) { tokenizeMarkdown(text, inlineCodeBg) }
    val annotatedString = remember(tokens, color, linkColor) {
        buildAnnotatedString {
            tokens.forEach { token ->
                if (token.url != null) {
                    pushStringAnnotation(TAG_URL, token.url)
                    withStyle(SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )) {
                        append(token.text)
                    }
                    pop()
                } else if (token.style != null) {
                    withStyle(token.style + SpanStyle(color = color)) {
                        append(token.text)
                    }
                } else {
                    withStyle(SpanStyle(color = color)) {
                        append(token.text)
                    }
                }
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(TAG_URL, offset, offset).firstOrNull()?.let { annotation ->
                val url = annotation.item
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            }
        }
    )
}
