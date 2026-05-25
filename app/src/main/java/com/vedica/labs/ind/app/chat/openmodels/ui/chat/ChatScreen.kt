package com.vedica.labs.ind.app.chat.openmodels.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatMessage
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatSession
import com.vedica.labs.ind.app.chat.openmodels.ui.components.InfoGuard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SessionsDrawerContent(
                    sessions = state.sessions,
                    activeSessionId = state.activeSessionId,
                    isLoading = state.isLoadingSessions,
                    onSelectSession = { viewModel.selectSession(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onNewSession = { viewModel.createNewSession() },
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.imePadding().navigationBarsPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Chat", style = MaterialTheme.typography.titleMedium)
                            if (state.isGenerating) {
                                Text(
                                    text = "Generating...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Sessions")
                        }
                    },
                    actions = {
                        if (state.activeSessionId != null) {
                            IconButton(onClick = { viewModel.createNewSession() }) {
                                Icon(Icons.Default.Add, contentDescription = "New Conversation")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.error != null) {
                    Snackbar(
                        modifier = Modifier.padding(8.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) {
                        Text(state.error!!)
                    }
                }

                when {
                    !modelManagerIsLoaded(state) -> {
                        InfoGuard(
                            icon = Icons.Outlined.Memory,
                            title = "No Model Loaded",
                            subtitle = "To start chatting entirely offline, you must load model weights into your device RAM memory first.",
                            footnote = "Navigate to the Repository tab to download and load a model."
                        )
                    }
                    state.activeSessionId == null -> {
                        InfoGuard(
                            icon = Icons.Outlined.Chat,
                            title = "Start a Conversation",
                            subtitle = "Your local model is loaded and ready. Tap below to create a new chat session.",
                            actionLabel = "Create a Conversation",
                            onAction = { viewModel.createNewSession() }
                        )
                    }
                    else -> {
                        MessagesList(
                            messages = state.messages,
                            streamingContent = state.streamingContent,
                            isGenerating = state.isGenerating,
                            canLoadMore = !state.hasReachedMax,
                            onLoadMore = { viewModel.loadMoreMessages() }
                        )
                        InputBar(
                            isGenerating = state.isGenerating,
                            onSend = { viewModel.sendMessage(it) },
                            onStop = { viewModel.stopGeneration() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun modelManagerIsLoaded(state: ChatUiState): Boolean = true

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionsDrawerContent(
    sessions: List<ChatSession>,
    activeSessionId: String?,
    isLoading: Boolean,
    onSelectSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.width(300.dp).fillMaxHeight()) {
        TopAppBar(
            title = { Text("Sessions") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
            actions = {
                IconButton(onClick = onNewSession) {
                    Icon(Icons.Default.Add, contentDescription = "New Session")
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sessions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sessions) { session ->
                    SessionItem(
                        session = session,
                        isActive = session.id == activeSessionId,
                        onClick = { onSelectSession(session.id); onClose() },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    session: ChatSession,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Surface(
        onClick = onClick,
        color = if (isActive) NeonCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.modelName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.lastPreview != null) {
                    Text(
                        text = session.lastPreview!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${session.messageCount} msgs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(session.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
        }
    }
}

@Composable
private fun ColumnScope.MessagesList(
    messages: List<ChatMessage>,
    streamingContent: String,
    isGenerating: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to newest message. Using scrollToItem (instant) instead of animateScrollToItem
    // because the animation was restarting on every streaming token, flooding the main thread
    // with frame callbacks and contributing to ANRs. Split into two LaunchedEffects with
    // independent keys so that a new message arrival scrolls even during streaming.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(streamingContent) {
        if (streamingContent.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (streamingContent.isNotEmpty()) {
            item(key = "streaming") {
                MessageBubble(content = streamingContent, isUser = false, isStreaming = true)
            }
        }

        items(messages.reversed(), key = { it.id }) { message ->
            MessageBubble(
                content = message.content,
                isUser = message.isUser,
                isStreaming = false,
                tokensPerSecond = message.tokensPerSecond
            )
        }

        if (canLoadMore && messages.isNotEmpty()) {
            item(key = "load_more") {
                TextButton(
                    onClick = onLoadMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load older messages")
                }
            }
        }
    }
}

private data class ContentSegment(
    val type: String,  // "text" or "code"
    val content: String,
    val language: String? = null
)

private fun parseContentSegments(content: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val regex = Regex("```(\\w*)\\s*\\n([\\s\\S]*?)```")
    var lastEnd = 0

    for (match in regex.findAll(content)) {
        if (match.range.first > lastEnd) {
            val text = content.substring(lastEnd, match.range.first).trim()
            if (text.isNotEmpty()) {
                segments.add(ContentSegment("text", text))
            }
        }
        segments.add(ContentSegment("code", match.groupValues[2].trimEnd(), match.groupValues[1].ifEmpty { null }))
        lastEnd = match.range.last + 1
    }

    if (lastEnd < content.length) {
        val text = content.substring(lastEnd).trim()
        if (text.isNotEmpty()) {
            segments.add(ContentSegment("text", text))
        }
    }

    if (segments.isEmpty()) {
        segments.add(ContentSegment("text", content))
    }

    return segments
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean = false,
    tokensPerSecond: Double? = null
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val shape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    val bgColor = if (isUser) UserBubbleIndigo else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = align
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.wrapContentWidth()
        ) {
            if (isUser) Spacer(modifier = Modifier.weight(1f))

            Box {
                Surface(
                    shape = shape,
                    color = bgColor,
                    tonalElevation = if (isUser) 0.dp else 1.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showMenu = true }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        val segments = remember(content) { parseContentSegments(content) }

                        for (seg in segments) {
                            when (seg.type) {
                                "code" -> CodeBlockView(
                                    code = seg.content,
                                    language = seg.language
                                )
                                "text" -> MarkdownText(
                                    text = seg.content + if (isStreaming && segments.size == 1) " ▊" else "",
                                    color = textColor
                                )
                            }
                        }

                        if (isStreaming && segments.size > 1) {
                            MarkdownText(
                                text = " ▊",
                                color = textColor
                            )
                        }

                        if (tokensPerSecond != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${"%.1f".format(tokensPerSecond)} tok/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Context menu on long-press
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    offset = DpOffset(
                        x = if (isUser) (-160).dp else 0.dp,
                        y = 0.dp
                    )
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy") },
                        onClick = {
                            showMenu = false
                            copyText(context, content)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Code Blocks") },
                        onClick = {
                            showMenu = false
                            val codeSegments = parseContentSegments(content)
                                .filter { it.type == "code" }
                                .joinToString("\n\n") { it.content }
                            if (codeSegments.isNotEmpty()) copyText(context, codeSegments)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Select All") },
                        onClick = {
                            showMenu = false
                            copyText(context, content)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            if (!isUser) Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun copyText(context: Context, text: String) {
    val clip = ClipData.newPlainText("chat_message", text)
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
}

@Composable
private fun InputBar(
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                enabled = !isGenerating,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSend(text.trim())
                            text = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isGenerating) {
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
            } else {
                FilledIconButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSend(text.trim())
                            text = ""
                            focusManager.clearFocus()
                        }
                    },
                    enabled = text.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = NeonCyan,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) DarkObsidian else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
