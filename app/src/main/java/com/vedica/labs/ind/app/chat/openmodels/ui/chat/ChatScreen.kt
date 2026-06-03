package com.vedica.labs.ind.app.chat.openmodels.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatMessage
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatSession
import com.vedica.labs.ind.app.chat.openmodels.domain.parser.LlmOutputParser
import com.vedica.labs.ind.app.chat.openmodels.domain.parser.SegmentType
import com.vedica.labs.ind.app.chat.openmodels.ui.components.InfoGuard
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.DarkObsidian
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.ErrorRed
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.NeonCyan
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.UserBubbleIndigo
import com.vedica.labs.ind.app.chat.openmodels.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(
        checkNotNull<ViewModelStoreOwner>(
            LocalViewModelStoreOwner.current
        ) {
                "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
            }, null
    )
) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val tts = remember {
        TextToSpeech(context.applicationContext) { _ -> }
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    val onSpeak: (String) -> Unit = { text ->
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

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
            modifier = Modifier.imePadding(),
            contentWindowInsets = WindowInsets.ime,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.activeModelName ?: "Chat",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
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
                    !modelManagerIsLoaded() -> {
                        InfoGuard(
                            icon = Icons.Outlined.Memory,
                            title = "No Model Loaded",
                            subtitle = "To start chatting entirely offline, you must load model weights into your device RAM memory first.",
                            footnote = "Navigate to the Repository tab to download and load a model."
                        )
                    }
                    state.activeSessionId == null -> {
                        InfoGuard(
                            icon = Icons.AutoMirrored.Outlined.Chat,
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
                            canLoadMore = !state.hasReachedMax,
                            onLoadMore = { viewModel.loadMoreMessages() },
                            showThinking = state.params.showThinking,
                            showReasoning = state.params.showReasoning,
                            onSpeak = onSpeak
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
private fun modelManagerIsLoaded(): Boolean = true

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
                        text = session.lastPreview,
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
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    showThinking: Boolean,
    showReasoning: Boolean,
    onSpeak: ((String) -> Unit)? = null
) {
    val listState = rememberLazyListState()

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
                MessageBubble(
                    content = streamingContent,
                    isUser = false,
                    isStreaming = true,
                    showThinking = showThinking,
                    showReasoning = showReasoning,
                    onSpeak = onSpeak
                )
            }
        }

        items(messages.reversed(), key = { it.id }) { message ->
            MessageBubble(
                content = message.content,
                isUser = message.isUser,
                isStreaming = false,
                tokensPerSecond = message.tokensPerSecond,
                showThinking = showThinking,
                showReasoning = showReasoning,
                onSpeak = onSpeak
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

@Composable
private fun MessageBubble(
    content: String,
    isUser: Boolean,
    isStreaming: Boolean = false,
    tokensPerSecond: Double? = null,
    showThinking: Boolean = true,
    showReasoning: Boolean = true,
    onSpeak: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val shape = if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    val bgColor = if (isUser) UserBubbleIndigo else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val iconTint = textColor.copy(alpha = 0.7f)
    val hasCodeBlocks = remember(content) {
        LlmOutputParser().extractCodeBlocks(content).isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.wrapContentWidth()
        ) {
            if (isUser) Spacer(modifier = Modifier.weight(1f))

            Surface(
                shape = shape,
                color = bgColor,
                tonalElevation = if (isUser) 0.dp else 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    val parser = remember { LlmOutputParser() }
                    val segments = remember(content, showThinking, showReasoning) {
                        parser.parse(content, showThinking, showReasoning)
                    }

                    for (seg in segments) {
                        when (seg.type) {
                            SegmentType.TEXT -> MarkdownText(
                                text = seg.content + if (isStreaming && segments.size == 1) " ▊" else "",
                                color = textColor
                            )
                            SegmentType.CODE_BLOCK -> CodeBlockView(
                                code = seg.content,
                                language = seg.metadata["language"]
                            )
                            SegmentType.THINKING, SegmentType.REASONING -> MarkdownText(
                                text = seg.content,
                                color = textColor.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            )
                            SegmentType.TOOL_CALL -> MarkdownText(
                                text = seg.content,
                                color = WarningAmber
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

                    if (!isStreaming) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = { copyText(context, content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = iconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (hasCodeBlocks) {
                                IconButton(
                                    onClick = {
                                        val code = LlmOutputParser().extractCodeBlocks(content)
                                            .joinToString("\n\n") { it.second }
                                        if (code.isNotEmpty()) copyText(context, code)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Code,
                                        contentDescription = "Copy code blocks",
                                        tint = iconTint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onSpeak?.invoke(content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "Read aloud",
                                    tint = iconTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
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
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) DarkObsidian else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
