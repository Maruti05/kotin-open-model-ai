package com.vedica.labs.ind.app.chat.openmodels.data.repository

import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.ChatMessageDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.dao.ChatSessionDao
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.ChatMessageEntity
import com.vedica.labs.ind.app.chat.openmodels.data.local.entity.ChatSessionEntity
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatMessage
import com.vedica.labs.ind.app.chat.openmodels.data.model.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val sessionDao: ChatSessionDao,
    private val messageDao: ChatMessageDao
) {
    fun getAllSessions(): Flow<List<ChatSession>> = sessionDao.getAllSessions().map { entities ->
        entities.map { entity ->
            ChatSession(
                id = entity.id,
                modelName = entity.modelName,
                createdAt = entity.createdAt,
                systemPromptOverride = entity.systemPromptOverride
            )
        }
    }

    suspend fun getSessionById(sessionId: String): ChatSession? {
        val entity = sessionDao.getSessionById(sessionId) ?: return null
        val count = messageDao.getMessageCount(sessionId)
        val lastPreview = messageDao.getLastAssistantMessage(sessionId)
        return ChatSession(
            id = entity.id,
            modelName = entity.modelName,
            createdAt = entity.createdAt,
            systemPromptOverride = entity.systemPromptOverride,
            messageCount = count,
            lastPreview = lastPreview
        )
    }

    suspend fun createSession(modelName: String, systemPromptOverride: String? = null): ChatSession {
        val session = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            modelName = modelName,
            createdAt = System.currentTimeMillis(),
            systemPromptOverride = systemPromptOverride
        )
        sessionDao.insertSession(session)
        return ChatSession(
            id = session.id,
            modelName = session.modelName,
            createdAt = session.createdAt,
            systemPromptOverride = session.systemPromptOverride
        )
    }

    suspend fun deleteSession(sessionId: String) {
        sessionDao.deleteSessionById(sessionId)
    }

    suspend fun deleteAllSessions() {
        // Would need a DAO method for this
    }

    fun getMessagesBySession(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesBySession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getMessagesPaginated(sessionId: String, limit: Int, offset: Int): List<ChatMessage> {
        return messageDao.getMessagesPaginated(sessionId, limit, offset).map { it.toDomain() }
    }

    suspend fun getMessageCount(sessionId: String): Int {
        return messageDao.getMessageCount(sessionId)
    }

    suspend fun insertMessage(message: ChatMessage) {
        messageDao.insertMessage(message.toEntity())
    }

    suspend fun insertMessage(
        sessionId: String,
        role: String,
        content: String,
        tokensPerSecond: Double? = null
    ): ChatMessage {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            tokensPerSecond = tokensPerSecond
        )
        messageDao.insertMessage(message.toEntity())
        return message
    }

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id, sessionId = sessionId, role = role,
        content = content, timestamp = timestamp,
        tokensPerSecond = tokensPerSecond
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id, sessionId = sessionId, role = role,
        content = content, timestamp = timestamp,
        tokensPerSecond = tokensPerSecond
    )
}
