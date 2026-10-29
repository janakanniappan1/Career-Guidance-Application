package com.example.domain.repository

import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract managing CGA AI Mentor persistent conversations and message turns.
 */
interface MentorRepository {

    /**
     * Creates a new conversation thread for the authenticated student.
     *
     * @param title Initial human-readable title for the conversation (default: "New Conversation").
     * @return Result containing the created [MentorConversation] domain model with generated UUID.
     */
    suspend fun createConversation(title: String = "New Conversation"): Result<MentorConversation>

    /**
     * Observes the list of all persistent conversation threads belonging to the authenticated student,
     * ordered by most recent activity (`updated_at DESC`).
     */
    fun getConversations(): Flow<List<MentorConversation>>

    /**
     * Retrieves a single conversation by its unique identifier.
     */
    suspend fun getConversation(conversationId: String): Result<MentorConversation>

    /**
     * Observes all chronological message turns within a specific conversation (`created_at ASC`).
     *
     * @param conversationId Unique identifier of the conversation.
     */
    fun getMessages(conversationId: String): Flow<List<MentorChatMessage>>

    /**
     * Transmits a user message turn to the AI Mentor backend Edge Function,
     * which atomically persists the user message, invokes Gemini, saves the model reply,
     * and returns the resulting [MentorChatMessage].
     *
     * @param conversationId Target conversation UUID.
     * @param message Non-empty text input from the student.
     * @param context Optional verified CGA student context (roadmap, skills, streak, XP).
     * @return Result containing the generated [MentorChatMessage] turn.
     */
    suspend fun sendMessage(
        conversationId: String,
        message: String,
        context: CGAMentorContext? = null
    ): Result<MentorChatMessage>

    /**
     * Permanently deletes a conversation thread and its associated message turns via database cascade.
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
