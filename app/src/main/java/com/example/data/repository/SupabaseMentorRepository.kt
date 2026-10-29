package com.example.data.repository

import android.util.Log
import com.example.data.model.ChatConversationDto
import com.example.data.model.ChatMessageDto
import com.example.data.model.SendMentorMessageRequestDto
import com.example.data.model.SendMentorMessageResponseDto
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.CGAMentorContext
import com.example.domain.model.MentorChatMessage
import com.example.domain.model.MentorConversation
import com.example.domain.model.MentorRole
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.MentorRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Production implementation of [MentorRepository] backed by Supabase PostgREST and
 * the `chat-with-career-assistant` Edge Function.
 *
 * Security & Design Guarantees:
 * - Session Authoritative: All queries and Edge Function invocations use the authenticated JWT token.
 * - Single Source of Truth: Conversations and messages are persisted in PostgreSQL (`chat_conversations`, `chat_messages`).
 * - No Duplicate Message Inserts: The Edge Function atomically handles user message persistence and model response creation.
 * - No Gemini Secret Exposure: Gemini API interaction is completely isolated server-side.
 */
class SupabaseMentorRepository(
    private val authRepository: AuthRepository? = null,
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
) : MentorRepository {

    companion object {
        private const val TAG = "SupabaseMentorRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private suspend fun getAuthenticatedUserId(): String? {
        return try {
            authRepository?.getCurrentUser()?.firstOrNull()?.id
                ?: client.auth.currentUserOrNull()?.id
        } catch (_: Throwable) {
            client.auth.currentUserOrNull()?.id
        }
    }

    override suspend fun createConversation(
        title: String
    ): Result<MentorConversation> = withContext(ioDispatcher) {
        val userId = getAuthenticatedUserId()
        if (userId.isNullOrBlank()) {
            Log.e(TAG, "createConversation failed: No authenticated user session found")
            return@withContext Result.failure(IllegalStateException("User must be signed in to create a conversation."))
        }

        val cleanTitle = title.trim().ifBlank { "New Conversation" }
        val generatedId = UUID.randomUUID().toString()

        val dto = ChatConversationDto(
            id = generatedId,
            userId = userId,
            title = cleanTitle
        )

        try {
            client.from("chat_conversations").insert(dto)
            Log.d(TAG, "Created conversation '$generatedId' with title '$cleanTitle' for user '$userId'")
            Result.success(
                MentorConversation(
                    id = generatedId,
                    userId = userId,
                    title = cleanTitle,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to create conversation in Supabase: ${e.message}", e)
            Result.failure(Exception("Unable to create new conversation: ${e.message ?: "Database error"}"))
        }
    }

    override fun getConversations(): Flow<List<MentorConversation>> = flow {
        val userId = getAuthenticatedUserId()
        if (userId.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }

        try {
            val dtoList = client.from("chat_conversations")
                .select {
                    order("updated_at", Order.DESCENDING)
                }
                .decodeList<ChatConversationDto>()

            val domainList = dtoList.map { it.toDomain() }
            emit(domainList)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to load conversations for user $userId: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)

    override suspend fun getConversation(
        conversationId: String
    ): Result<MentorConversation> = withContext(ioDispatcher) {
        if (conversationId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Conversation ID cannot be blank."))
        }

        try {
            val list = client.from("chat_conversations")
                .select {
                    filter { eq("id", conversationId) }
                    limit(1)
                }
                .decodeList<ChatConversationDto>()

            val item = list.firstOrNull()
            if (item != null) {
                Result.success(item.toDomain())
            } else {
                Result.failure(NoSuchElementException("Conversation not found."))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to get conversation $conversationId: ${e.message}", e)
            Result.failure(Exception("Failed to retrieve conversation: ${e.message ?: "Network error"}"))
        }
    }

    override fun getMessages(
        conversationId: String
    ): Flow<List<MentorChatMessage>> = flow {
        if (conversationId.isBlank()) {
            emit(emptyList())
            return@flow
        }

        try {
            val dtoList = client.from("chat_messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ChatMessageDto>()

            val domainList = dtoList.map { it.toDomain() }
            emit(domainList)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to load messages for conversation $conversationId: ${e.message}", e)
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)

    override suspend fun sendMessage(
        conversationId: String,
        message: String,
        context: CGAMentorContext?
    ): Result<MentorChatMessage> = withContext(ioDispatcher) {
        val cleanMessage = message.trim()
        if (cleanMessage.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Message cannot be empty."))
        }
        if (conversationId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Conversation ID is required."))
        }

        val sessionToken = client.auth.currentSessionOrNull()?.accessToken
            ?: client.auth.currentAccessTokenOrNull()

        if (sessionToken.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("You must be logged in to chat with the AI Mentor."))
        }

        val baseUrl = SupabaseClientProvider.effectiveUrl.trimEnd('/')
        val endpointUrl = "$baseUrl/functions/v1/chat-with-career-assistant"
        val anonKey = SupabaseClientProvider.effectivePublishableKey

        val requestDto = SendMentorMessageRequestDto(
            conversationId = conversationId,
            message = cleanMessage,
            context = context
        )

        val requestBodyString = try {
            json.encodeToString(requestDto)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Failed to serialize chat request: ${e.message}"))
        }

        val requestBody = requestBodyString.toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $sessionToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        Log.d(TAG, "Sending message to AI Mentor Edge Function at $endpointUrl (conv: $conversationId)")

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (responseBody.isBlank()) {
                    Log.w(TAG, "Empty response from AI Mentor Edge Function (HTTP ${response.code})")
                    return@withContext Result.failure(Exception("Received empty response from AI Mentor service."))
                }

                val responseDto = try {
                    json.decodeFromString<SendMentorMessageResponseDto>(responseBody)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse Edge Function response: $responseBody", e)
                    return@withContext Result.failure(Exception("Unable to process AI response. Please try again."))
                }

                if (response.isSuccessful && responseDto.success && responseDto.message != null) {
                    val replyContent = responseDto.message.content.trim()
                    if (replyContent.isBlank()) {
                        return@withContext Result.failure(Exception("AI Mentor returned an empty message."))
                    }

                    val resultMessage = MentorChatMessage(
                        id = UUID.randomUUID().toString(),
                        conversationId = conversationId,
                        userId = getAuthenticatedUserId() ?: "",
                        role = MentorRole.MODEL,
                        content = replyContent,
                        createdAt = System.currentTimeMillis()
                    )
                    Log.d(TAG, "Successfully received AI Mentor response for conv: $conversationId")
                    Result.success(resultMessage)
                } else {
                    val errorMsg = responseDto.error
                        ?: if (response.code == 401) "Session expired. Please log in again."
                        else if (response.code == 404) "Conversation not found."
                        else "AI Mentor service error (HTTP ${response.code})."
                    Log.w(TAG, "AI Mentor Edge Function returned error: $errorMsg (body: $responseBody)")
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Network failure communicating with AI Mentor Edge Function", e)
            Result.failure(Exception("Network error connecting to AI Mentor: ${e.message ?: "Please check your connection"}"))
        }
    }

    override suspend fun deleteConversation(
        conversationId: String
    ): Result<Unit> = withContext(ioDispatcher) {
        if (conversationId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Conversation ID cannot be blank."))
        }

        try {
            client.from("chat_conversations").delete {
                filter { eq("id", conversationId) }
            }
            Log.d(TAG, "Deleted conversation: $conversationId")
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to delete conversation $conversationId: ${e.message}", e)
            Result.failure(Exception("Unable to delete conversation: ${e.message ?: "Database error"}"))
        }
    }
}
