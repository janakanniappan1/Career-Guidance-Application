package com.example.data.model

import com.example.domain.model.AssessmentAnswer
import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentOption
import com.example.domain.model.AssessmentQuestion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data Transfer Objects for Assessment tables in Supabase:
 * - assessment_questions
 * - assessment_options
 * - assessment_answers
 * - assessment_attempts
 */

@Serializable
data class AssessmentQuestionDto(
    @SerialName("id") val id: Int,
    @SerialName("question_number") val questionNumber: Int? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("question_text") val questionText: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("is_active") val isActive: Boolean? = true
) {
    fun toDomain(options: List<AssessmentOption>): AssessmentQuestion {
        val resolvedText = questionText?.ifBlank { text.orEmpty() } ?: text.orEmpty()
        return AssessmentQuestion(
            id = id,
            questionNumber = questionNumber ?: id,
            category = category?.ifBlank { "General" } ?: "General",
            questionText = resolvedText,
            description = description.orEmpty(),
            options = options
        )
    }
}

@Serializable
data class AssessmentOptionDto(
    @SerialName("id") val id: String,
    @SerialName("question_id") val questionId: Int,
    @SerialName("text") val text: String? = null,
    @SerialName("option_text") val optionText: String? = null,
    @SerialName("scores") val scores: Map<String, Int> = emptyMap(),
    @SerialName("order_index") val orderIndex: Int? = null,
    @SerialName("option_order") val optionOrder: Int? = null
) {
    fun toDomain(): AssessmentOption {
        val resolvedText = text?.ifBlank { optionText.orEmpty() } ?: optionText.orEmpty()
        return AssessmentOption(
            id = id,
            text = resolvedText,
            scores = scores
        )
    }
}

@Serializable
data class AssessmentAnswerDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("question_id") val questionId: Int,
    @SerialName("selected_option_id") val selectedOptionId: String? = null,
    @SerialName("option_id") val optionId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val resolvedOptionId: String
        get() = selectedOptionId?.ifBlank { optionId.orEmpty() } ?: optionId.orEmpty()
}

@Serializable
data class SaveAssessmentAnswerRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("question_id") val questionId: Int,
    @SerialName("selected_option_id") val selectedOptionId: String? = null,
    @SerialName("option_id") val optionId: String? = null
)

@Serializable
data class AssessmentAttemptDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("answers") val answers: Map<String, String> = emptyMap(),
    @SerialName("top_career_id") val topCareerId: String,
    @SerialName("match_percentage") val matchPercentage: Int,
    @SerialName("completed_at") val completedAt: String? = null
) {
    fun toDomain(): AssessmentAttempt {
        val mappedAnswers = answers.mapNotNull { (k, v) ->
            k.toIntOrNull()?.let { it to v }
        }.toMap()

        val epochMilli = try {
            if (!completedAt.isNullOrBlank()) {
                java.time.Instant.parse(completedAt).toEpochMilli()
            } else null
        } catch (_: Throwable) {
            null
        }

        return AssessmentAttempt(
            id = id,
            userId = userId,
            answers = mappedAnswers,
            topCareerId = topCareerId,
            matchPercentage = matchPercentage,
            completedAt = epochMilli
        )
    }

    companion object {
        fun fromDomain(attempt: AssessmentAttempt): AssessmentAttemptDto {
            val stringAnswers = attempt.answers.mapKeys { it.key.toString() }
            val isoTime = attempt.completedAt?.let {
                java.time.Instant.ofEpochMilli(it).toString()
            } ?: java.time.Instant.now().toString()

            return AssessmentAttemptDto(
                id = attempt.id,
                userId = attempt.userId,
                answers = stringAnswers,
                topCareerId = attempt.topCareerId,
                matchPercentage = attempt.matchPercentage,
                completedAt = isoTime
            )
        }
    }
}
