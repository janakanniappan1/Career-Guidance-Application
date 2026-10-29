package com.example.data.model

import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data Transfer Object for mapping `public.chat_conversations` table.
 */
@Serializable
data class ChatConversationDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("title")
    val title: String = "New Conversation",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun toDomain(): MentorConversation {
        return MentorConversation(
            id = id ?: "",
            userId = userId ?: "",
            title = title.ifBlank { "New Conversation" },
            createdAt = parseIsoTimestamp(createdAt),
            updatedAt = parseIsoTimestamp(updatedAt)
        )
    }

    companion object {
        fun fromDomain(conv: MentorConversation): ChatConversationDto {
            return ChatConversationDto(
                id = conv.id.ifBlank { null },
                userId = conv.userId.ifBlank { null },
                title = conv.title.ifBlank { "New Conversation" }
            )
        }

        private fun parseIsoTimestamp(isoString: String?): Long? {
            if (isoString.isNullOrBlank()) return null
            return try {
                Instant.parse(isoString).toEpochMilli()
            } catch (_: Throwable) {
                null
            }
        }
    }
}

/**
 * Data Transfer Object for mapping `public.chat_messages` table.
 */
@Serializable
data class ChatMessageDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): MentorChatMessage {
        return MentorChatMessage(
            id = id ?: "",
            conversationId = conversationId,
            userId = userId ?: "",
            role = MentorRole.fromString(role),
            content = content,
            createdAt = parseIsoTimestamp(createdAt)
        )
    }

    companion object {
        fun fromDomain(msg: MentorChatMessage): ChatMessageDto {
            return ChatMessageDto(
                id = msg.id.ifBlank { null },
                conversationId = msg.conversationId,
                userId = msg.userId.ifBlank { null },
                role = when (msg.role) {
                    MentorRole.USER -> "user"
                    MentorRole.MODEL -> "model"
                },
                content = msg.content
            )
        }

        private fun parseIsoTimestamp(isoString: String?): Long? {
            if (isoString.isNullOrBlank()) return null
            return try {
                Instant.parse(isoString).toEpochMilli()
            } catch (_: Throwable) {
                null
            }
        }
    }
}

/**
 * Request payload sent to Supabase Edge Function `chat-with-career-assistant`.
 */
@Serializable
data class SendMentorMessageRequestDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("message")
    val message: String,
    @SerialName("context")
    val context: CGAMentorContext? = null
)

/**
 * Model response content nested inside successful Edge Function response.
 */
@Serializable
data class ChatMessageContentDto(
    @SerialName("role")
    val role: String = "model",
    @SerialName("content")
    val content: String
)

/**
 * Response payload received from Supabase Edge Function `chat-with-career-assistant`.
 */
@Serializable
data class SendMentorMessageResponseDto(
    @SerialName("success")
    val success: Boolean,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    @SerialName("message")
    val message: ChatMessageContentDto? = null,
    @SerialName("error")
    val error: String? = null
)
