package com.vedica.labs.ind.app.chat.openmodels.data.model

data class ChatSession(
    val id: String,
    val modelName: String,
    val createdAt: Long,
    val systemPromptOverride: String? = null,
    val messageCount: Int = 0,
    val lastPreview: String? = null
)
