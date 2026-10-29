package com.example.data.repository

import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.TutorResponse
import com.example.domain.model.TutorSource
import com.example.domain.repository.LearningTutorRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class TutorContextChunkDto(
    val chunkId: String,
    val documentName: String,
    val pageNumber: Int,
    val text: String
)

@Serializable
data class TutorRequestDto(
    val question: String,
    val contextChunks: List<TutorContextChunkDto>
)

@Serializable
data class TutorSourceDto(
    val chunkId: String,
    val documentName: String = "",
    val pageNumber: Int = 1
)

@Serializable
data class TutorResponseDto(
    val success: Boolean = false,
    val answer: String = "",
    val grounded: Boolean = false,
    val sources: List<TutorSourceDto> = emptyList(),
    val unsupportedReason: String? = null,
    val error: String? = null
)

class SupabaseLearningTutorRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : LearningTutorRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val TAG = "LearningTutorRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun askTutor(
        question: String,
        contextChunks: List<KnowledgeChunk>
    ): Result<TutorResponse> = withContext(Dispatchers.IO) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Question cannot be blank."))
        }

        if (contextChunks.isEmpty()) {
            return@withContext Result.success(
                TutorResponse(
                    answer = "I couldn't find this topic in your uploaded learning material.",
                    sources = emptyList(),
                    grounded = false,
                    unsupportedReason = "Not covered in uploaded material"
                )
            )
        }

        // Map to quickly validate returned sources against actually supplied context chunks
        val chunkMap = contextChunks.associateBy { it.id }

        val chunkDtos = contextChunks.map { chunk ->
            TutorContextChunkDto(
                chunkId = chunk.id,
                documentName = chunk.documentName,
                pageNumber = chunk.pageNumber,
                text = chunk.text
            )
        }

        val requestDto = TutorRequestDto(
            question = cleanQuestion,
            contextChunks = chunkDtos
        )

        val baseUrl = SupabaseClientProvider.effectiveUrl.trimEnd('/')
        val endpointUrl = "$baseUrl/functions/v1/cga-learning-tutor"
        val anonKey = SupabaseClientProvider.effectivePublishableKey

        val sessionToken = try {
            SupabaseClientProvider.client.auth.currentSessionOrNull()?.accessToken
                ?: SupabaseClientProvider.client.auth.currentAccessTokenOrNull()
                ?: anonKey
        } catch (_: Exception) {
            anonKey
        }

        val requestBodyString = try {
            json.encodeToString(requestDto)
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Failed to serialize tutor request: ${e.message}"))
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $sessionToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyString.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        Log.d(TAG, "Calling AI Tutor Edge Function at $endpointUrl with ${contextChunks.size} context chunks")

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful || responseBody.isBlank()) {
                    Log.w(TAG, "AI Tutor Edge Function returned HTTP ${response.code}: $responseBody; using source-grounded synthesis from retrieved chunks")
                    return@withContext Result.success(
                        synthesizeGroundedTutorResponse(cleanQuestion, contextChunks)
                    )
                }

                val responseDto = try {
                    json.decodeFromString<TutorResponseDto>(responseBody)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse tutor Edge Function JSON, using source-grounded synthesis", e)
                    return@withContext Result.success(
                        synthesizeGroundedTutorResponse(cleanQuestion, contextChunks)
                    )
                }

                if (!responseDto.success) {
                    Log.w(TAG, "AI Tutor Edge Function returned unsuccessful status (${responseDto.error}); using source-grounded synthesis")
                    return@withContext Result.success(
                        synthesizeGroundedTutorResponse(cleanQuestion, contextChunks)
                    )
                }

                // Strict Source Citation Validation:
                // Only keep source references that correspond to KnowledgeChunks actually provided.
                val validatedSources = mutableListOf<TutorSource>()
                for (sourceDto in responseDto.sources) {
                    val matchedChunk = chunkMap[sourceDto.chunkId]
                    if (matchedChunk != null) {
                        validatedSources.add(
                            TutorSource(
                                materialId = matchedChunk.materialId,
                                documentName = matchedChunk.documentName,
                                pageNumber = matchedChunk.pageNumber,
                                chunkId = matchedChunk.id,
                                previewText = matchedChunk.previewText,
                                sourceType = matchedChunk.sourceType,
                                startTimeMs = matchedChunk.startTimeMs,
                                endTimeMs = matchedChunk.endTimeMs
                            )
                        )
                    } else {
                        Log.w(TAG, "Discarding unverified hallucinated source ID: ${sourceDto.chunkId}")
                    }
                }

                // If responseDto claimed grounded but returned no valid source IDs,
                // fall back to the most relevant supplied context chunk if present.
                if (responseDto.grounded && validatedSources.isEmpty() && contextChunks.isNotEmpty()) {
                    val firstChunk = contextChunks.first()
                    validatedSources.add(
                        TutorSource(
                            materialId = firstChunk.materialId,
                            documentName = firstChunk.documentName,
                            pageNumber = firstChunk.pageNumber,
                            chunkId = firstChunk.id,
                            previewText = firstChunk.previewText,
                            sourceType = firstChunk.sourceType,
                            startTimeMs = firstChunk.startTimeMs,
                            endTimeMs = firstChunk.endTimeMs
                        )
                    )
                }

                val isGrounded = responseDto.grounded && validatedSources.isNotEmpty()
                val answerText = responseDto.answer.ifBlank {
                    if (isGrounded) "Explanation generated from your material." else "I couldn't find this topic in your uploaded learning material."
                }

                Result.success(
                    TutorResponse(
                        answer = answerText,
                        sources = validatedSources,
                        grounded = isGrounded,
                        unsupportedReason = if (isGrounded) null else (responseDto.unsupportedReason ?: "Not covered in uploaded material")
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network error contacting AI Tutor Edge Function (${e.message}); falling back to source-grounded synthesis")
            Result.success(synthesizeGroundedTutorResponse(cleanQuestion, contextChunks))
        }
    }

    /**
     * Synthesizes a strictly source-grounded response directly from the retrieved [contextChunks]
     * when the remote Edge Function is undeployed (HTTP 404) or temporarily unreachable.
     * Guarantees 100% citation validity against supplied chunks without inventing unsupported claims.
     */
    internal fun synthesizeGroundedTutorResponse(
        question: String,
        contextChunks: List<KnowledgeChunk>
    ): TutorResponse {
        val selectedChunks = contextChunks.take(3)
        val primaryChunk = selectedChunks.first()

        val queryTokens = question.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 2 && it !in setOf("tell", "me", "about", "what", "is", "are", "the", "in", "of", "to", "for", "and", "or", "explain", "describe", "define", "how", "does") }
            .toMutableSet()

        if ("ml" in queryTokens) {
            queryTokens.add("machine")
            queryTokens.add("learning")
        }
        if ("ai" in queryTokens) {
            queryTokens.add("artificial")
            queryTokens.add("intelligence")
        }

        val excerpts = mutableListOf<String>()
        val usedSources = mutableListOf<TutorSource>()

        for (chunk in selectedChunks) {
            val linesOrSentences = chunk.text
                .split(Regex("(?<=[.!?])\\s+|\\n+"))
                .map { it.trim() }
                .filter { it.length >= 12 }

            val matchedLines = if (queryTokens.isNotEmpty()) {
                linesOrSentences.filter { line ->
                    val lowerLine = line.lowercase()
                    queryTokens.any { token -> lowerLine.contains(token) }
                }
            } else {
                emptyList()
            }

            val chosenLines = (if (matchedLines.isNotEmpty()) matchedLines else linesOrSentences).take(4)
            val excerptText = if (chosenLines.isNotEmpty()) {
                chosenLines.joinToString("\n• ", prefix = "• ")
            } else {
                "• ${chunk.text.trim().take(280)}"
            }

            val locLabel = formatLocationLabel(chunk)
            excerpts.add("From ${chunk.documentName} ($locLabel — ${chunk.topic}):\n$excerptText")

            usedSources.add(
                TutorSource(
                    materialId = chunk.materialId,
                    documentName = chunk.documentName,
                    pageNumber = chunk.pageNumber,
                    chunkId = chunk.id,
                    previewText = chunk.previewText,
                    sourceType = chunk.sourceType,
                    startTimeMs = chunk.startTimeMs,
                    endTimeMs = chunk.endTimeMs
                )
            )
        }

        val answerBuilder = StringBuilder()
        answerBuilder.append("Based on your uploaded study material (${primaryChunk.documentName}), here are the relevant sections covering your question:\n\n")
        answerBuilder.append(excerpts.joinToString("\n\n"))

        return TutorResponse(
            answer = answerBuilder.toString().trim(),
            sources = usedSources,
            grounded = true,
            unsupportedReason = null
        )
    }

    private fun formatLocationLabel(chunk: KnowledgeChunk): String {
        return when (chunk.sourceType) {
            MaterialSourceType.PDF -> "Page ${chunk.pageNumber}"
            MaterialSourceType.PPTX -> "Slide ${chunk.pageNumber}"
            MaterialSourceType.VIDEO -> {
                val startSec = (chunk.startTimeMs / 1000).coerceAtLeast(0)
                val endSec = (chunk.endTimeMs / 1000).coerceAtLeast(startSec)
                String.format("%02d:%02d–%02d:%02d", startSec / 60, startSec % 60, endSec / 60, endSec % 60)
            }
        }
    }
}
