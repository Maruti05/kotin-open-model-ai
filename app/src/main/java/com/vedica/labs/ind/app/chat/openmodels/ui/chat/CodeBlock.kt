package com.vedica.labs.ind.app.chat.openmodels.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CODE_BG = Color(0xFF1A1B2E)
private val CODE_LANG_BG = Color(0xFF2D2D44)
private val CODE_BORDER = Color(0xFF3D3D5C)
private val KEYWORD_COLOR = Color(0xFFFF79C6)
private val STRING_COLOR = Color(0xFFA5D6FF)
private val COMMENT_COLOR = Color(0xFF6D6D8B)
private val NUMBER_COLOR = Color(0xFF79C6FF)
private val FUNCTION_COLOR = Color(0xFFDCDCAA)
private val TYPE_COLOR = Color(0xFF4EC9B0)
private val ANNOTATION_COLOR = Color(0xFFC586C0)
private val PLAIN_COLOR = Color(0xFFE8E8F0)

private val LANGUAGE_KEYWORDS = mapOf(
    "kotlin" to setOf(
        "val", "var", "fun", "class", "object", "interface", "enum", "sealed",
        "data", "override", "open", "abstract", "private", "protected", "public",
        "internal", "if", "else", "when", "for", "while", "do", "try", "catch",
        "finally", "return", "throw", "import", "package", "as", "is", "in",
        "true", "false", "null", "super", "this", "companion", "init", "constructor",
        "suspend", "inline", "infix", "operator", "tailrec", "external", "annotation",
        "reified", "crossinline", "noinline", "expect", "actual", "typealias"
    ),
    "java" to setOf(
        "public", "private", "protected", "static", "final", "class", "interface",
        "enum", "extends", "implements", "abstract", "synchronized", "volatile",
        "transient", "native", "strictfp", "if", "else", "for", "while", "do",
        "switch", "case", "break", "continue", "return", "throw", "try", "catch",
        "finally", "new", "this", "super", "import", "package", "void", "int",
        "long", "double", "float", "boolean", "char", "byte", "short",
        "true", "false", "null", "instanceof", "var", "record", "sealed", "yield",
        "module", "requires", "exports", "opens", "provides", "with", "to"
    ),
    "python" to setOf(
        "def", "class", "if", "elif", "else", "for", "while", "try", "except",
        "finally", "with", "as", "import", "from", "return", "yield", "raise",
        "pass", "break", "continue", "and", "or", "not", "in", "is", "lambda",
        "True", "False", "None", "self", "async", "await", "global", "nonlocal",
        "match", "case", "type", "assert", "del", "elif"
    ),
    "javascript" to setOf(
        "function", "const", "let", "var", "class", "if", "else", "for", "while",
        "do", "switch", "case", "break", "continue", "return", "throw", "try",
        "catch", "finally", "new", "this", "typeof", "instanceof", "async",
        "await", "import", "export", "default", "from", "true", "false", "null",
        "undefined", "yield", "delete", "in", "of", "super", "static", "get", "set"
    ),
    "typescript" to setOf(
        "function", "const", "let", "var", "class", "interface", "type", "enum",
        "if", "else", "for", "while", "do", "switch", "case", "break", "continue",
        "return", "throw", "try", "catch", "finally", "new", "this", "typeof",
        "instanceof", "async", "await", "import", "export", "default", "from",
        "true", "false", "null", "undefined", "any", "void", "never", "unknown",
        "as", "is", "keyof", "extends", "implements", "readonly", "declare",
        "namespace", "module", "infer", "satisfies", "using"
    ),
    "xml" to setOf(
        "xml", "version", "encoding", "standalone", "schema", "stylesheet",
        "transform", "template", "value-of", "for-each", "if", "choose",
        "when", "otherwise"
    ),
    "html" to setOf(
        "html", "head", "body", "div", "span", "p", "a", "img", "input",
        "button", "form", "label", "select", "option", "textarea", "table",
        "tr", "td", "th", "thead", "tbody", "tfoot", "ul", "ol", "li",
        "h1", "h2", "h3", "h4", "h5", "h6", "header", "footer", "nav",
        "section", "article", "aside", "main", "figure", "figcaption",
        "link", "script", "style", "meta", "title", "br", "hr", "pre",
        "code", "blockquote", "em", "strong", "i", "b", "u", "s", "small",
        "sub", "sup", "mark", "ins", "del", "video", "audio", "canvas",
        "iframe", "embed", "object", "param", "source", "picture",
        "details", "summary", "dialog", "data", "time", "progress",
        "meter", "datalist", "fieldset", "legend", "optgroup",
        "col", "colgroup", "caption", "abbr", "address", "cite",
        "wbr", "slot", "template", "portal"
    ),
    "css" to setOf(
        "color", "background", "margin", "padding", "border", "display",
        "position", "width", "height", "font", "text", "flex", "grid",
        "important", "none", "auto", "inherit", "initial", "relative",
        "absolute", "fixed", "sticky", "block", "inline", "inline-block",
        "flex-start", "flex-end", "center", "space-between", "space-around",
        "hidden", "visible", "scroll", "auto", "wrap", "nowrap", "column",
        "row", "column-reverse", "row-reverse", "baseline", "stretch"
    ),
    "sql" to setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
        "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "JOIN", "LEFT",
        "RIGHT", "INNER", "OUTER", "FULL", "CROSS", "ON", "AND", "OR", "NOT",
        "IN", "LIKE", "BETWEEN", "IS", "NULL", "AS", "ORDER", "BY", "GROUP",
        "HAVING", "LIMIT", "OFFSET", "DISTINCT", "COUNT", "SUM", "AVG", "MIN",
        "MAX", "UNION", "ALL", "EXISTS", "CASE", "WHEN", "THEN", "ELSE", "END",
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "CONSTRAINT", "UNIQUE",
        "CHECK", "DEFAULT", "CASCADE", "VIEW", "TRIGGER", "PROCEDURE",
        "FUNCTION", "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "GRANT",
        "REVOKE", "SCHEMA", "DATABASE"
    ),
    "json" to emptySet(),
    "yaml" to emptySet(),
    "shell" to setOf(
        "if", "then", "else", "elif", "fi", "for", "while", "do", "done",
        "case", "esac", "in", "function", "return", "exit", "export",
        "local", "readonly", "declare", "typeset", "unset", "set",
        "echo", "printf", "read", "source", "shift", "select", "until",
        "continue", "break", "trap", "exec", "eval", "let", "test"
    ),
    "bash" to setOf(
        "if", "then", "else", "elif", "fi", "for", "while", "do", "done",
        "case", "esac", "in", "function", "return", "exit", "export",
        "local", "readonly", "declare", "typeset", "unset", "set",
        "echo", "printf", "read", "source", "shift", "select", "until",
        "continue", "break", "trap", "exec", "eval", "let", "test"
    ),
    "c" to setOf(
        "int", "long", "float", "double", "char", "void", "struct", "union",
        "enum", "if", "else", "for", "while", "do", "switch", "case", "break",
        "continue", "return", "sizeof", "typedef", "const", "static", "extern",
        "volatile", "register", "signed", "unsigned", "include", "define",
        "goto", "auto", "short"
    ),
    "cpp" to setOf(
        "int", "long", "float", "double", "char", "void", "bool", "class",
        "struct", "union", "enum", "if", "else", "for", "while", "do",
        "switch", "case", "break", "continue", "return", "new", "delete",
        "this", "virtual", "override", "const", "static", "template",
        "typename", "namespace", "using", "include", "define", "public",
        "private", "protected", "friend", "explicit", "mutable", "noexcept",
        "constexpr", "auto", "decltype", "nullptr", "string", "vector", "map",
        "short", "unsigned", "signed", "goto", "try", "catch", "throw",
        "dynamic_cast", "static_cast", "reinterpret_cast", "const_cast",
        "typeid", "alignas", "alignof", "thread_local"
    ),
    "dart" to setOf(
        "class", "enum", "extends", "implements", "with", "abstract", "static",
        "final", "const", "var", "void", "int", "double", "String", "bool",
        "dynamic", "if", "else", "for", "while", "do", "switch", "case",
        "break", "continue", "return", "throw", "try", "catch", "finally",
        "import", "export", "library", "part", "of", "as", "is", "in",
        "true", "false", "null", "this", "super", "new", "async", "await",
        "yield", "mixin", "late", "required", "factory", "covariant",
        "base", "sealed", "final", "interface"
    ),
    "rust" to setOf(
        "fn", "let", "mut", "const", "static", "if", "else", "for", "while",
        "loop", "match", "return", "break", "continue", "struct", "enum",
        "impl", "trait", "type", "pub", "use", "mod", "crate", "super",
        "self", "where", "as", "in", "ref", "move", "async", "await",
        "unsafe", "extern", "fn", "dyn", "true", "false", "Some", "None",
        "Ok", "Err", "abstract", "become", "box", "do", "final", "macro",
        "override", "priv", "typeof", "unsized", "virtual", "yield"
    ),
    "go" to setOf(
        "package", "import", "func", "type", "struct", "interface", "map",
        "chan", "var", "const", "if", "else", "for", "range", "switch",
        "case", "default", "break", "continue", "return", "go", "defer",
        "select", "fallthrough", "true", "false", "nil", "make", "new",
        "append", "len", "cap", "copy", "close", "delete", "panic",
        "recover", "print", "println"
    ),
    "ruby" to setOf(
        "def", "class", "module", "if", "elsif", "else", "unless", "case",
        "when", "for", "while", "until", "do", "end", "begin", "rescue",
        "ensure", "return", "break", "next", "redo", "retry", "yield",
        "self", "true", "false", "nil", "super", "and", "or", "not",
        "in", "then", "defined?", "alias", "undef", "require", "include",
        "extend", "prepend", "raise", "throw", "catch", "lambda", "proc"
    ),
    "swift" to setOf(
        "func", "var", "let", "class", "struct", "enum", "protocol",
        "extension", "if", "else", "for", "while", "repeat", "switch",
        "case", "default", "break", "continue", "return", "throw", "try",
        "catch", "guard", "defer", "in", "as", "is", "where", "true",
        "false", "nil", "self", "super", "import", "typealias", "associatedtype",
        "mutating", "nonmutating", "inout", "throws", "rethrows",
        "open", "public", "internal", "fileprivate", "private",
        "static", "final", "override", "required", "optional",
        "lazy", "weak", "unowned", "indirect", "convenience",
        "dynamic", "infix", "prefix", "postfix", "precedencegroup",
        "actor", "async", "await", "nonisolated", "mainactor"
    ),
    "php" to setOf(
        "echo", "print", "if", "else", "elseif", "for", "foreach", "while",
        "do", "switch", "case", "break", "continue", "return", "require",
        "include", "require_once", "include_once", "function", "class",
        "interface", "trait", "abstract", "final", "public", "private",
        "protected", "static", "const", "var", "global", "new", "this",
        "parent", "self", "true", "false", "null", "isset", "unset",
        "empty", "die", "exit", "array", "list", "as", "try", "catch",
        "throw", "finally", "namespace", "use", "implements", "extends",
        "clone", "instanceof", "yield", "match", "enum", "readonly"
    ),
    "r" to setOf(
        "function", "if", "else", "for", "while", "repeat", "return",
        "next", "break", "TRUE", "FALSE", "NULL", "NA", "NaN", "Inf",
        "library", "require", "source", "setwd", "getwd", "install.packages",
        "c", "list", "data.frame", "matrix", "factor", "length", "nrow",
        "ncol", "names", "rownames", "colnames", "summary", "mean", "median",
        "sd", "var", "cor", "cov", "lm", "glm", "plot", "ggplot"
    ),
    "scala" to setOf(
        "def", "val", "var", "class", "object", "trait", "enum", "case",
        "if", "else", "for", "while", "do", "match", "return", "throw",
        "try", "catch", "finally", "import", "package", "new", "this",
        "super", "true", "false", "null", "sealed", "abstract", "final",
        "private", "protected", "override", "implicit", "explicit",
        "lazy", "macro", "type", "with", "extends", "yield"
    )
)

@Composable
fun CodeBlockView(
    code: String,
    language: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val lang = language?.lowercase()?.trim() ?: ""
    val displayLang = lang.ifEmpty { "code" }
    val keywords = LANGUAGE_KEYWORDS[lang] ?: emptySet()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = CODE_BG,
        tonalElevation = 0.dp
    ) {
        Column {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CODE_LANG_BG,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayLang,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    if (copied) {
                        Text(
                            text = "Copied!",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = {
                            copyText(context, code)
                            copied = true
                            scope.launch {
                                delay(2000)
                                copied = false
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (copied) SuccessGreen else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            val hScroll = rememberScrollState()
            val vScroll = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .horizontalScroll(hScroll)
                    .verticalScroll(vScroll)
                    .padding(12.dp)
            ) {
                val highlighted = remember(code, keywords) {
                    highlightCode(code, keywords)
                }

                Text(
                    text = highlighted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun highlightCode(code: String, keywords: Set<String>): AnnotatedString {
    return buildAnnotatedString {
        var pos = 0
        val len = code.length

        while (pos < len) {
            val ch = code[pos]

            when {
                // Line comment: // or #
                ch == '/' && pos + 1 < len && code[pos + 1] == '/' -> {
                    val end = code.indexOf('\n', pos).let { if (it == -1) len else it }
                    withStyle(SpanStyle(color = COMMENT_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                ch == '#' && isLineStart(code, pos) -> {
                    val end = code.indexOf('\n', pos).let { if (it == -1) len else it }
                    withStyle(SpanStyle(color = COMMENT_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Block comment: /* */
                ch == '/' && pos + 1 < len && code[pos + 1] == '*' -> {
                    val end = code.indexOf("*/", pos + 2).let { if (it == -1) len else it + 2 }
                    withStyle(SpanStyle(color = COMMENT_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // HTML comment <!-- -->
                ch == '<' && pos + 3 < len && code[pos + 1] == '!' && code[pos + 2] == '-' && code[pos + 3] == '-' -> {
                    val end = code.indexOf("-->", pos + 4).let { if (it == -1) len else it + 3 }
                    withStyle(SpanStyle(color = COMMENT_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Double-quoted string
                ch == '"' -> {
                    val end = findStringEnd(code, pos, '"')
                    withStyle(SpanStyle(color = STRING_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Single-quoted string
                ch == '\'' -> {
                    val end = findStringEnd(code, pos, '\'')
                    withStyle(SpanStyle(color = STRING_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Backtick string (JS template literals)
                ch == '`' -> {
                    val end = findStringEnd(code, pos, '`')
                    withStyle(SpanStyle(color = STRING_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Annotation @identifier
                ch == '@' -> {
                    var end = pos + 1
                    while (end < len && (code[end].isLetterOrDigit() || code[end] == '.')) end++
                    withStyle(SpanStyle(color = ANNOTATION_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Number literal
                ch.isDigit() && (pos == 0 || !isIdentChar(code[pos - 1])) -> {
                    var end = pos
                    if (end + 1 < len && code[end] == '0' && (code[end + 1] == 'x' || code[end + 1] == 'X')) {
                        end += 2
                        while (end < len && (code[end].isDigit() || code[end] in 'a'..'f' || code[end] in 'A'..'F')) end++
                    } else {
                        while (end < len && (code[end].isDigit() || code[end] == '.' || code[end] == 'f' || code[end] == 'F' || code[end] == 'L' || code[end] == 'l')) end++
                    }
                    withStyle(SpanStyle(color = NUMBER_COLOR)) { append(code.substring(pos, end)) }
                    pos = end
                }
                // Identifier or keyword
                ch.isLetter() || ch == '_' -> {
                    var end = pos
                    while (end < len && isIdentChar(code[end])) end++
                    val word = code.substring(pos, end)
                    val upperCount = word.count { it.isUpperCase() }

                    // Heuristic: any word starting with uppercase and length > 1 is a type name.
                    // This catches common single-word types like String, List, Map, System, Integer
                    // which the stricter PascalCase-only check (upperCount > 1) would miss.
                    val style = when {
                        keywords.contains(word) -> SpanStyle(color = KEYWORD_COLOR)
                        word[0].isUpperCase() && word.length > 1 -> SpanStyle(
                            color = TYPE_COLOR,
                            fontWeight = FontWeight.Medium
                        )
                        end < len && code[end] == '(' -> SpanStyle(color = FUNCTION_COLOR)
                        else -> null
                    }

                    if (style != null) {
                        withStyle(style) { append(word) }
                    } else {
                        append(word)
                    }
                    pos = end
                }
                else -> {
                    append(ch)
                    pos++
                }
            }
        }
    }
}

private fun isLineStart(text: String, pos: Int): Boolean {
    if (pos == 0) return true
    var i = pos - 1
    while (i >= 0 && text[i].isWhitespace()) i--
    return i < 0 || text[i] == '\n'
}

private fun isIdentChar(ch: Char): Boolean = ch.isLetterOrDigit() || ch == '_'

private fun findStringEnd(text: String, start: Int, quote: Char): Int {
    var i = start + 1
    while (i < text.length) {
        when (text[i]) {
            '\\' -> i += 2
            quote -> return i + 1
            '\n' -> return i
            else -> i++
        }
    }
    return text.length
}

private fun copyText(context: Context, text: String) {
    val clip = ClipData.newPlainText("code", text)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}
