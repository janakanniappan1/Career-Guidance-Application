package com.example.data.repository

import android.util.Log
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.assessment.QuestionValidator
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.adaptive.Assessment
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.model.adaptive.QuestionResult
import com.example.domain.model.adaptive.TopicPerformance
import com.example.domain.repository.AdaptiveAssessmentRepository
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
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Serializable
data class AssessmentContextChunkDto(
    val chunkId: String,
    val documentName: String,
    val pageNumber: Int,
    val topic: String,
    val concept: String,
    val text: String
)

@Serializable
data class AssessmentRequestDto(
    val scope: String,
    val difficulty: String,
    val questionCount: Int,
    val knowledgeChunks: List<AssessmentContextChunkDto>
)

@Serializable
data class RawQuestionDto(
    val question: String = "",
    val type: String = "MCQ",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val explanation: String = "",
    val topic: String = "General",
    val concept: String = "Core Concept",
    val difficulty: String = "Medium",
    val sourceChunkId: String = ""
)

@Serializable
data class AssessmentResponseDto(
    val success: Boolean = false,
    val questions: List<RawQuestionDto> = emptyList(),
    val error: String? = null
)

class SupabaseAdaptiveAssessmentRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val validator: QuestionValidator = QuestionValidator()
) : AdaptiveAssessmentRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val TAG = "AdaptiveAssessmentRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    override suspend fun generateAssessment(
        materialId: String,
        documentName: String,
        scope: String,
        difficulty: AssessmentDifficulty,
        questionCount: Int,
        contextChunks: List<KnowledgeChunk>
    ): Result<Assessment> = withContext(Dispatchers.IO) {
        if (contextChunks.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("Build a Knowledge Base before creating an assessment.")
            )
        }

        // Map chunks for source metadata lookup
        val chunkMap = contextChunks.associateBy { it.id }

        val chunkDtos = contextChunks.map { c ->
            AssessmentContextChunkDto(
                chunkId = c.id,
                documentName = c.documentName,
                pageNumber = c.pageNumber,
                topic = c.topic,
                concept = c.concept,
                text = c.text
            )
        }

        val requestDto = AssessmentRequestDto(
            scope = scope,
            difficulty = difficulty.displayName,
            questionCount = questionCount,
            knowledgeChunks = chunkDtos
        )

        val baseUrl = SupabaseClientProvider.effectiveUrl.trimEnd('/')
        val endpointUrl = "$baseUrl/functions/v1/cga-adaptive-assessment"
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
            return@withContext Result.failure(Exception("Failed to serialize assessment request: ${e.message}"))
        }

        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $sessionToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyString.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        Log.d(TAG, "Requesting adaptive assessment from $endpointUrl with ${contextChunks.size} context chunks")

        val validatedFromEdge = try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful || responseBody.isBlank()) {
                    Log.w(TAG, "Edge function returned HTTP ${response.code}: $responseBody")
                    emptyList()
                } else {
                    val responseDto = try {
                        json.decodeFromString<AssessmentResponseDto>(responseBody)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse assessment response JSON: $responseBody", e)
                        null
                    }

                    if (responseDto == null || !responseDto.success || responseDto.questions.isEmpty()) {
                        emptyList()
                    } else {
                        val rawQuestions = responseDto.questions.map { raw ->
                            val matchedChunk = chunkMap[raw.sourceChunkId] ?: contextChunks.first()
                            val cleanOptions = raw.options.map { it.trim() }
                            val resolvedCorrect = resolveMatchingOption(raw.correctAnswer.trim(), cleanOptions)
                            AssessmentQuestion(
                                id = UUID.randomUUID().toString(),
                                question = raw.question.trim(),
                                type = AssessmentQuestionType.fromString(raw.type),
                                options = cleanOptions,
                                correctAnswer = resolvedCorrect,
                                explanation = raw.explanation.trim().ifBlank {
                                    "Grounded in ${matchedChunk.documentName} (Page ${matchedChunk.pageNumber})."
                                },
                                topic = raw.topic.trim().ifBlank { matchedChunk.topic.ifBlank { "General" } },
                                concept = raw.concept.trim().ifBlank { matchedChunk.concept.ifBlank { "Core Concept" } },
                                difficulty = AssessmentDifficulty.fromString(raw.difficulty),
                                materialId = matchedChunk.materialId,
                                documentName = matchedChunk.documentName,
                                pageNumber = matchedChunk.pageNumber,
                                sourceChunkId = matchedChunk.id,
                                sourceType = matchedChunk.sourceType,
                                startTimeMs = matchedChunk.startTimeMs,
                                endTimeMs = matchedChunk.endTimeMs
                            )
                        }
                        validator.filterValidQuestions(rawQuestions, contextChunks)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network/Edge Function error during assessment generation (${e.message}); falling back to source-grounded generator")
            emptyList()
        }

        val finalQuestions = if (validatedFromEdge.isNotEmpty()) {
            validatedFromEdge
        } else {
            val synthesized = generateSourceGroundedQuestions(
                contextChunks = contextChunks,
                scope = scope,
                difficulty = difficulty,
                questionCount = questionCount.coerceIn(1, 15),
                materialId = materialId,
                documentName = documentName
            )
            validator.filterValidQuestions(synthesized, contextChunks)
        }

        if (finalQuestions.isEmpty()) {
            return@withContext Result.failure(
                Exception("Could not generate valid source-grounded questions from this material. Please try another topic or scope.")
            )
        }

        val assessmentTitle = if (scope.equals("All Topics", ignoreCase = true)) {
            "$documentName Comprehensive Assessment"
        } else {
            "$scope Practice Assessment"
        }

        val assessment = Assessment(
            id = UUID.randomUUID().toString(),
            materialId = materialId,
            title = assessmentTitle,
            topic = scope,
            questions = finalQuestions,
            createdAt = System.currentTimeMillis()
        )

        Result.success(assessment)
    }

    private fun resolveMatchingOption(rawCorrect: String, options: List<String>): String {
        if (options.isEmpty()) return rawCorrect
        // 1. Direct case-insensitive match
        options.firstOrNull { it.equals(rawCorrect, ignoreCase = true) }?.let { return it }
        // 2. Letter index match ("A", "Option A", "A)")
        val letterMatch = Regex("^(?:Option\\s+)?([A-Da-d])(?:[.):]\\s*(.*))?$").find(rawCorrect)
        if (letterMatch != null) {
            val letter = letterMatch.groupValues[1].uppercase().first()
            val idx = letter - 'A'
            if (idx in options.indices) return options[idx]
        }
        // 3. Stripped prefix match
        val strippedCorrect = rawCorrect.replace(Regex("^[A-Da-d][.):]\\s*"), "").trim()
        options.firstOrNull { opt ->
            val strippedOpt = opt.replace(Regex("^[A-Da-d][.):]\\s*"), "").trim()
            strippedOpt.equals(strippedCorrect, ignoreCase = true) ||
                strippedOpt.contains(strippedCorrect, ignoreCase = true) ||
                strippedCorrect.contains(strippedOpt, ignoreCase = true)
        }?.let { return it }

        return rawCorrect
    }

    /**
     * Generates strictly source-grounded assessment questions directly from supplied [contextChunks]
     * when the remote Edge Function is undeployed (HTTP 404) or unavailable.
     * Supports both standard study materials (textbooks, slides, transcripts) and Question Bank PDFs.
     */
    internal fun generateSourceGroundedQuestions(
        contextChunks: List<KnowledgeChunk>,
        scope: String,
        difficulty: AssessmentDifficulty,
        questionCount: Int,
        materialId: String,
        documentName: String
    ): List<AssessmentQuestion> {
        if (contextChunks.isEmpty()) return emptyList()

        data class GroundedItem(
            val chunk: KnowledgeChunk,
            val statement: String,
            val cleanFocus: String
        )

        // Extract meaningful statements or syllabus/question-bank items across all contextChunks
        val allItems = mutableListOf<GroundedItem>()
        for (chunk in contextChunks) {
            val rawLines = chunk.text
                .split(Regex("(?<=[.!?])\\s+|\\n+"))
                .map { line ->
                    line.replace(Regex("^\\s*(?:Q\\.?\\s*\\d+|\\d+\\s*[.):\\-]|[•*\\-]|[a-zA-Z][.)])\\s*"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }
                .filter { line ->
                    line.length >= 18 &&
                        !line.matches(Regex("^(?:PART|UNIT|MODULE|SECTION|PAGE)\\s+[A-Z0-9IVX]+.*$", RegexOption.IGNORE_CASE))
                }

            if (rawLines.isNotEmpty()) {
                for (line in rawLines) {
                    val focus = extractKeyFocusPhrase(line, chunk.concept, chunk.topic)
                    allItems.add(GroundedItem(chunk = chunk, statement = line.take(140), cleanFocus = focus))
                }
            } else {
                val fallbackStmt = chunk.text.replace(Regex("\\s+"), " ").trim().take(140)
                if (fallbackStmt.isNotBlank()) {
                    allItems.add(
                        GroundedItem(
                            chunk = chunk,
                            statement = fallbackStmt,
                            cleanFocus = chunk.concept.ifBlank { chunk.topic.ifBlank { "Core Concept" } }
                        )
                    )
                }
            }
        }

        if (allItems.isEmpty()) return emptyList()

        val questions = mutableListOf<AssessmentQuestion>()
        val genericDistractorPool = listOf(
            "Replacing all data validation with unindexed manual file pointers in hardware ROM",
            "Bypassing mathematical model evaluation in favor of static assembly register allocation",
            "Eliminating training datasets by hardcoding fixed pixel intensities in BIOS firmware",
            "Restricting computation strictly to single-bit synchronous serial bus interrupts",
            "Disabling algorithmic optimization to force exponential O(2^n) exhaustive enumeration",
            "Storing all model parameters exclusively in volatile L1 instruction cache without feedback"
        )

        for (i in 0 until questionCount) {
            val itemIdx = if (allItems.size >= questionCount) {
                ((i.toDouble() / questionCount) * allItems.size).toInt().coerceIn(0, allItems.lastIndex)
            } else {
                i % allItems.size
            }
            val item = allItems[itemIdx]
            val chunk = item.chunk
            val topicName = chunk.topic.trim().ifBlank { if (scope.equals("All Topics", true)) documentName.take(30).ifBlank { "General" } else scope }
            val conceptName = chunk.concept.trim().ifBlank { item.cleanFocus.ifBlank { "Core Concepts" } }

            val qDifficulty = when (difficulty) {
                AssessmentDifficulty.MIXED -> when (i % 3) {
                    0 -> AssessmentDifficulty.EASY
                    1 -> AssessmentDifficulty.MEDIUM
                    else -> AssessmentDifficulty.HARD
                }
                else -> difficulty
            }

            val locText = when (chunk.sourceType) {
                MaterialSourceType.PDF -> "Page ${chunk.pageNumber}"
                MaterialSourceType.PPTX -> "Slide ${chunk.pageNumber}"
                MaterialSourceType.VIDEO -> "Segment ${chunk.pageNumber}"
            }

            // Construct correct option directly from the chunk's statement
            val correctOption = formatOptionText(item.statement, chunk, i)

            // Collect 3 distinct distractor options from other chunks/items first, then fallback pool
            val distractors = mutableListOf<String>()
            for (offset in 1 until allItems.size) {
                if (distractors.size >= 3) break
                val candidateItem = allItems[(itemIdx + offset) % allItems.size]
                if (candidateItem.chunk.id != chunk.id || candidateItem.statement != item.statement) {
                    val candidateOpt = formatOptionText(candidateItem.statement, candidateItem.chunk, i + offset)
                    if (!candidateOpt.equals(correctOption, ignoreCase = true) &&
                        distractors.none { it.equals(candidateOpt, ignoreCase = true) }
                    ) {
                        distractors.add(candidateOpt)
                    }
                }
            }

            var poolIdx = i
            while (distractors.size < 3) {
                val fallbackOpt = genericDistractorPool[poolIdx % genericDistractorPool.size]
                poolIdx++
                if (!fallbackOpt.equals(correctOption, ignoreCase = true) &&
                    distractors.none { it.equals(fallbackOpt, ignoreCase = true) }
                ) {
                    distractors.add(fallbackOpt)
                }
            }

            // Deterministic ordering of options so correct answer position varies across questions
            val rawFour = listOf(correctOption) + distractors.take(3)
            val shift = abs(chunk.id.hashCode() + i) % 4
            val rotatedOptions = (0 until 4).map { idx -> rawFour[(idx + shift) % 4] }

            val questionPrompt = when (i % 4) {
                0 -> "According to ${chunk.documentName} ($locText) on $topicName, which of the following is directly covered in the study material?"
                1 -> "In ${chunk.documentName} ($locText, Question/Section ${i + 1}), which concept or statement is presented regarding $topicName?"
                2 -> "Based on $locText of ${chunk.documentName} ($conceptName), identify the accurate statement or study item from your material:"
                else -> "Which of the following statements or key problems is explicitly referenced in ${chunk.documentName} ($locText — $topicName, Item ${i + 1})?"
            }

            val explanationText = "Source-grounded in ${chunk.documentName} ($locText, Topic: $topicName): \"${item.statement}\""

            questions.add(
                AssessmentQuestion(
                    id = UUID.randomUUID().toString(),
                    question = questionPrompt,
                    type = AssessmentQuestionType.MCQ,
                    options = rotatedOptions,
                    correctAnswer = correctOption,
                    explanation = explanationText,
                    topic = topicName,
                    concept = conceptName,
                    difficulty = qDifficulty,
                    materialId = chunk.materialId.ifBlank { materialId },
                    documentName = chunk.documentName.ifBlank { documentName },
                    pageNumber = chunk.pageNumber,
                    sourceChunkId = chunk.id,
                    sourceType = chunk.sourceType,
                    startTimeMs = chunk.startTimeMs,
                    endTimeMs = chunk.endTimeMs
                )
            )
        }

        return questions
    }

    private fun extractKeyFocusPhrase(line: String, concept: String, topic: String): String {
        val cleaned = line
            .replace(Regex("^(?:Define|Explain|What is|What are|Describe|Differentiate between|State|List|Compare|Discuss|Write short notes on)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
        val words = cleaned.split(Regex("\\s+")).take(5).joinToString(" ").replace(Regex("[^a-zA-Z0-9\\s\\-]"), "").trim()
        return words.ifBlank { concept.ifBlank { topic.ifBlank { "Core Concept" } } }
    }

    private fun formatOptionText(rawStatement: String, chunk: KnowledgeChunk, seed: Int): String {
        val cleaned = rawStatement.trim().trimEnd('.', ',', ';', ':')
        return if (cleaned.length >= 12) {
            cleaned.take(125)
        } else {
            "${cleaned.ifBlank { chunk.topic }} (${chunk.documentName} p.${chunk.pageNumber}, #${seed + 1})"
        }
    }

    override fun evaluateAssessment(
        assessment: Assessment,
        studentAnswers: Map<String, String>
    ): AssessmentResult {
        val questionResults = mutableListOf<QuestionResult>()

        for (question in assessment.questions) {
            val selected = studentAnswers[question.id]?.trim().orEmpty()
            val isCorrect = when (question.type) {
                AssessmentQuestionType.MCQ -> {
                    selected.equals(question.correctAnswer.trim(), ignoreCase = true)
                }
                AssessmentQuestionType.SHORT_ANSWER -> {
                    val normSelected = selected.lowercase().replace(Regex("[^a-z0-9]"), "")
                    val normCorrect = question.correctAnswer.lowercase().replace(Regex("[^a-z0-9]"), "")
                    normSelected.isNotBlank() && (normSelected == normCorrect || normCorrect.contains(normSelected))
                }
            }

            questionResults.add(
                QuestionResult(
                    questionId = question.id,
                    questionText = question.question,
                    selectedAnswer = selected.ifBlank { "[No Answer Provided]" },
                    correctAnswer = question.correctAnswer,
                    correct = isCorrect,
                    explanation = question.explanation,
                    topic = question.topic,
                    concept = question.concept,
                    documentName = question.documentName,
                    pageNumber = question.pageNumber,
                    sourceChunkId = question.sourceChunkId,
                    sourceType = question.sourceType,
                    startTimeMs = question.startTimeMs,
                    endTimeMs = question.endTimeMs
                )
            )
        }

        val totalQuestions = assessment.questions.size
        val correctAnswers = questionResults.count { it.correct }
        val scorePercentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0

        // Compute topic performance
        val topicGroups = questionResults.groupBy { it.topic }
        val topicPerformances = topicGroups.map { (topic, results) ->
            val totalInTopic = results.size
            val correctInTopic = results.count { it.correct }
            val accuracy = if (totalInTopic > 0) (correctInTopic * 100) / totalInTopic else 0
            TopicPerformance(
                topic = topic,
                totalQuestions = totalInTopic,
                correctAnswers = correctInTopic,
                accuracyPercentage = accuracy
            )
        }.sortedBy { it.accuracyPercentage }

        // A topic is weak when accuracy < 60%
        val weakTopics = topicPerformances.filter { it.isWeak }.map { it.topic }

        return AssessmentResult(
            assessmentId = assessment.id,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            scorePercentage = scorePercentage,
            topicPerformance = topicPerformances,
            weakTopics = weakTopics,
            questionResults = questionResults
        )
    }
}
