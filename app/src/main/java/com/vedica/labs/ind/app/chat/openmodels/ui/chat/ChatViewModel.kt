package com.vedica.labs.ind.app.chat.openmodels.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatMessage
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatSession
import com.vedica.labs.ind.app.chat.openmodels.data.model.InferenceParams
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ChatRepository
import com.vedica.labs.ind.app.chat.openmodels.data.repository.ModelRepository
import com.vedica.labs.ind.app.chat.openmodels.data.repository.SettingsRepository
import com.vedica.labs.ind.app.chat.openmodels.domain.inference.ChatMessage as InferenceChatMessage
import com.vedica.labs.ind.app.chat.openmodels.domain.inference.HybridModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ChatUiState(
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingContent: String = "",
    val isLoadingSessions: Boolean = true,
    val error: String? = null,
    val hasReachedMax: Boolean = false,
    val params: InferenceParams = InferenceParams()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val settingsRepository: SettingsRepository,
    private val modelManager: HybridModelManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var generationJob: Job? = null
    private val pageSize = 20

    init {
        Timber.tag("ChatVM").d("ChatViewModel initialized")
        loadSessions()
        viewModelScope.launch {
            settingsRepository.getInferenceParams().collect { params ->
                Timber.tag("ChatVM").d("Inference params updated: temp=%.2f, maxTokens=%d, systemPromptEnabled=%b",
                    params.temperature, params.maxTokens, params.systemPromptEnabled)
                _state.update { it.copy(params = params) }
            }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingSessions = true) }
            chatRepository.getAllSessions().collect { sessions ->
                _state.update { it.copy(sessions = sessions, isLoadingSessions = false) }
            }
        }
    }

    fun createNewSession(modelName: String? = null) {
        viewModelScope.launch {
            try {
                val name = modelName ?: modelManager.activeModelId ?: "local-model"
                val session = chatRepository.createSession(name)
                _state.update { it.copy(activeSessionId = session.id, messages = emptyList(), streamingContent = "", hasReachedMax = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            _state.update { it.copy(activeSessionId = sessionId, messages = emptyList(), hasReachedMax = false) }
            loadMessages(sessionId)
        }
    }

    private suspend fun loadMessages(sessionId: String) {
        val messages = chatRepository.getMessagesPaginated(sessionId, pageSize, 0)
        val count = chatRepository.getMessageCount(sessionId)
        _state.update { it.copy(messages = messages.reversed(), hasReachedMax = messages.size >= count) }
    }

    fun loadMoreMessages() {
        val sessionId = _state.value.activeSessionId ?: return
        if (_state.value.hasReachedMax) return
        viewModelScope.launch {
            val offset = _state.value.messages.size
            val older = chatRepository.getMessagesPaginated(sessionId, pageSize, offset)
            if (older.isEmpty()) {
                _state.update { it.copy(hasReachedMax = true) }
            } else {
                _state.update { it.copy(messages = older.reversed() + _state.value.messages) }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_state.value.activeSessionId == sessionId) {
                _state.update { it.copy(activeSessionId = null, messages = emptyList()) }
            }
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _state.value.activeSessionId
        val params = _state.value.params
        val currentState = _state.value
        if (text.isBlank() || currentState.isGenerating) {
            Timber.tag("ChatVM").w("sendMessage blocked: blank=%b, generating=%b", text.isBlank(), currentState.isGenerating)
            return
        }
        if (!modelManager.isLoaded) {
            Timber.tag("ChatVM").w("sendMessage blocked: no model loaded")
            _state.update { it.copy(error = "No model loaded. Download and load a model first.") }
            return
        }
        Timber.tag("ChatVM").i("sendMessage: len=%d, session=%s", text.length, sessionId)

        viewModelScope.launch {
            try {
                var activeSessionId = sessionId
                if (activeSessionId == null) {
                    val modelName = modelManager.activeModelId ?: "local-model"
                    val session = chatRepository.createSession(modelName)
                    activeSessionId = session.id
                    Timber.tag("ChatVM").d("Created new session: %s", activeSessionId)
                    _state.update { it.copy(activeSessionId = activeSessionId) }
                }

                Timber.tag("ChatVM").d("Inserting user message")
                val userMessage = chatRepository.insertMessage(activeSessionId, "user", text)
                val currentMessages = _state.value.messages + userMessage
                _state.update { it.copy(messages = currentMessages, isGenerating = true, streamingContent = "", error = null) }

                // Build inference context
                val allMsgs = chatRepository.getMessagesPaginated(activeSessionId, 30, 0).reversed()
                val recentMessages = (allMsgs + listOf(userMessage)).takeLast(30)

                val inferenceMessages = mutableListOf<InferenceChatMessage>()
                if (params.systemPromptEnabled && params.systemPrompt.isNotBlank()) {
                    inferenceMessages.add(InferenceChatMessage("system", params.systemPrompt))
                }
                inferenceMessages.addAll(recentMessages.map {
                    InferenceChatMessage(it.role, it.content)
                })

                // RAG context from files
                val fileContexts = modelRepository.getAllFileContexts()
                var ragContext = ""
                fileContexts.firstOrNull()?.let { files ->
                    if (files.isNotEmpty()) {
                        ragContext = "Context from local files:\n" + files.joinToString("\n\n") { "--- ${it.filename} ---\n${it.content}" }
                    }
                }

                val fullMessages = if (ragContext.isNotBlank()) {
                    listOf(InferenceChatMessage("system", ragContext)) + inferenceMessages
                } else inferenceMessages

                val template = modelManager.currentTemplate
                val contentBuilder = StringBuilder()
                var completed = false
                // Throttle UI updates to ~20/s (50ms interval) to prevent the main thread
                // from being overwhelmed by rapid Compose relayouts triggered by every token.
                // Without throttling, a model generating 50+ tokens/s causes the frame backlog
                // to exceed 5s, resulting in an "Input dispatching timed out" ANR.
                var lastUiUpdate = 0L
                val uiUpdateInterval = 50L

                Timber.tag("ChatVM").d("Starting generation: inferenceMsgs=%d, rag=%b, template=%s", fullMessages.size, ragContext.isNotBlank(), template)

                generationJob = viewModelScope.launch {
                    try {
                        modelManager.generateChat(fullMessages, template, params).collect { token ->
                            if (token == "[DONE]") {
                                Timber.tag("ChatVM").d("Generation [DONE] received, content=%d chars", contentBuilder.length)
                                if (!completed) {
                                    completed = true
                                    val finalContent = contentBuilder.toString()
                                    val savedMessage = chatRepository.insertMessage(activeSessionId, "assistant", finalContent)
                                    val updatedMessages = _state.value.messages + savedMessage
                                    _state.update { it.copy(messages = updatedMessages, isGenerating = false, streamingContent = "") }
                                }
                            } else {
                                contentBuilder.append(token)
                                val now = System.currentTimeMillis()
                                if (now - lastUiUpdate >= uiUpdateInterval) {
                                    lastUiUpdate = now
                                    _state.update { it.copy(streamingContent = contentBuilder.toString()) }
                                }
                            }
                        }
                        // Flush the final content to UI
                        if (!completed && contentBuilder.isNotEmpty()) {
                            _state.update { it.copy(streamingContent = contentBuilder.toString()) }
                        }
                        // Flow completed without [DONE] - save whatever we have
                        if (!completed && contentBuilder.isNotEmpty()) {
                            Timber.tag("ChatVM").w("Flow ended without [DONE], saving %d chars", contentBuilder.length)
                            completed = true
                            val finalContent = contentBuilder.toString()
                            val savedMessage = chatRepository.insertMessage(activeSessionId, "assistant", finalContent)
                            val updatedMessages = _state.value.messages + savedMessage
                            _state.update { it.copy(messages = updatedMessages, isGenerating = false, streamingContent = "") }
                        }
                    } catch (e: Exception) {
                        Timber.tag("ChatVM").e(e, "Generation failed after %d chars", contentBuilder.length)
                        if (!completed && contentBuilder.isNotEmpty()) {
                            chatRepository.insertMessage(activeSessionId, "assistant", contentBuilder.toString())
                        }
                        _state.update { it.copy(isGenerating = false, error = e.message) }
                    } finally {
                        if (!completed) {
                            Timber.tag("ChatVM").d("Generation finally, not completed, resetting state")
                            _state.update { it.copy(isGenerating = false) }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = e.message) }
            }
        }
    }

    fun stopGeneration() {
        Timber.tag("ChatVM").d("stopGeneration called")
        generationJob?.cancel()
        val prevJob = generationJob
        generationJob = null
        viewModelScope.launch {
            try {
                modelManager.stopGeneration()
                Timber.tag("ChatVM").d("stopGeneration OK")
            } catch (e: Exception) {
                Timber.tag("ChatVM").w(e, "stopGeneration exception")
            }
            _state.update { it.copy(isGenerating = false) }
            prevJob?.cancel()
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
