package com.vedica.labs.ind.app.chat.openmodels.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
                // Error banner
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
private fun modelManagerIsLoaded(state: ChatUiState): Boolean {
    // In a real app, this would check the model manager state
    return true // Simplified - the HybridModelManager is injected
}

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

    LaunchedEffect(messages.size, streamingContent) {
        if (messages.isNotEmpty() || streamingContent.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Streaming message at top (newest)
        if (streamingContent.isNotEmpty()) {
            item(key = "streaming") {
                MessageBubble(
                    content = streamingContent,
                    isUser = false,
                    isStreaming = true
                )
            }
        }

        // Regular messages (newest first due to reverseLayout)
        items(messages.reversed(), key = { it.id }) { message ->
            MessageBubble(
                content = message.content,
                isUser = message.isUser,
                isStreaming = false,
                tokensPerSecond = message.tokensPerSecond
            )
        }

        // Load more button
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
    tokensPerSecond: Double? = null
) {
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) UserBubbleIndigo else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = bgColor,
            tonalElevation = if (isUser) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp).fillMaxWidth(0.85f)) {
                if (isUser) {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    // Simple text rendering (markdown would be rendered here in a full implementation)
                    Text(
                        text = content + if (isStreaming) " ▊" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (tokensPerSecond != null) {
                    Text(
                        text = "${"%.1f".format(tokensPerSecond)} tok/s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = !isGenerating,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isGenerating) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = ErrorRed)
                }
            } else {
                IconButton(
                    onClick = { if (text.isNotBlank()) { onSend(text); text = "" } },
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = NeonCyan)
                }
            }
        }
    }
}
