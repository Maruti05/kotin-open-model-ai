package com.vedica.labs.ind.app.chat.openmodels.data.model

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val tokensPerSecond: Double? = null
) {
    val isUser: Boolean get() = role == "user"
    val isAssistant: Boolean get() = role == "assistant"
}
