package com.vedica.labs.ind.app.chat.openmodels.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val modelName: String,
    val createdAt: Long,
    val systemPromptOverride: String? = null
)
