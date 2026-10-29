package com.example.data.repository

import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import com.example.domain.repository.MentorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * In-memory Mock implementation of [MentorRepository] for local previewing and unit testing.
 */
class MockMentorRepository(
    private val currentUserId: String = "usr_student_demo"
) : MentorRepository {

    private val _conversations = MutableStateFlow<List<MentorConversation>>(emptyList())
    private val _messages = MutableStateFlow<Map<String, List<MentorChatMessage>>>(emptyMap())

    init {
        // Seed an initial demo conversation
        val initialConvId = "conv_demo_1"
        val now = System.currentTimeMillis()
        val initialConv = MentorConversation(
            id = initialConvId,
            userId = currentUserId,
            title = "Career & Technical Guidance",
            createdAt = now - 3600000,
            updatedAt = now
        )
        val initialMessages = listOf(
            MentorChatMessage(
                id = "msg_1",
                conversationId = initialConvId,
                userId = currentUserId,
                role = MentorRole.USER,
                content = "Hi, how should I start preparing for AI and Machine Learning engineering roles?",
                createdAt = now - 3500000
            ),
            MentorChatMessage(
                id = "msg_2",
                conversationId = initialConvId,
                userId = currentUserId,
                role = MentorRole.MODEL,
                content = "Great question! To build strong foundations for AI/ML roles, start by mastering Linear Algebra and Calculus, followed by Python, NumPy, Pandas, and PyTorch. Focus on building 2-3 end-to-end projects with live deployed models.",
                createdAt = now - 3400000
            )
        )

        _conversations.value = listOf(initialConv)
        _messages.value = mapOf(initialConvId to initialMessages)
    }

    override suspend fun createConversation(title: String): Result<MentorConversation> {
        val newId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val conv = MentorConversation(
            id = newId,
            userId = currentUserId,
            title = title.trim().ifBlank { "New Conversation" },
            createdAt = now,
            updatedAt = now
        )
        _conversations.value = listOf(conv) + _conversations.value
        _messages.value = _messages.value + (newId to emptyList())
        return Result.success(conv)
    }

    override fun getConversations(): Flow<List<MentorConversation>> {
        return _conversations.asStateFlow()
    }

    override suspend fun getConversation(conversationId: String): Result<MentorConversation> {
        val found = _conversations.value.firstOrNull { it.id == conversationId }
        return if (found != null) Result.success(found) else Result.failure(NoSuchElementException("Conversation not found"))
    }

    override fun getMessages(conversationId: String): Flow<List<MentorChatMessage>> {
        return _messages.map { map ->
            map[conversationId] ?: emptyList()
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        message: String,
        context: CGAMentorContext?
    ): Result<MentorChatMessage> {
        val cleanMsg = message.trim()
        if (cleanMsg.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))

        val now = System.currentTimeMillis()
        val userMsg = MentorChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            userId = currentUserId,
            role = MentorRole.USER,
            content = cleanMsg,
            createdAt = now
        )

        val replyText = if (cleanMsg.contains("roadmap", ignoreCase = true)) {
            "Based on your current progress in ${context?.recommendedCareer ?: "your career roadmap"}, your next immediate milestone is to complete your active checkpoints and keep your ${context?.currentStreak ?: 1}-day streak strong!"
        } else {
            "I'm here to support your career journey. Let's break this down into clear actionable steps."
        }

        val modelMsg = MentorChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            userId = currentUserId,
            role = MentorRole.MODEL,
            content = replyText,
            createdAt = now + 100
        )

        val currentList = _messages.value[conversationId] ?: emptyList()
        _messages.value = _messages.value + (conversationId to (currentList + userMsg + modelMsg))

        // Update conversation title and timestamp
        _conversations.value = _conversations.value.map { conv ->
            if (conv.id == conversationId) {
                conv.copy(
                    title = if (conv.title == "New Conversation") cleanMsg.take(30) else conv.title,
                    updatedAt = now + 100
                )
            } else conv
        }.sortedByDescending { it.updatedAt ?: 0L }

        return Result.success(modelMsg)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        _conversations.value = _conversations.value.filterNot { it.id == conversationId }
        _messages.value = _messages.value - conversationId
        return Result.success(Unit)
    }
}
