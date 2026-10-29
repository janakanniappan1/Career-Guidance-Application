package com.example.domain.model.adaptive

import com.example.domain.model.MaterialSourceType
import com.example.domain.model.TutorSource
import kotlinx.serialization.Serializable

/**
 * Question types supported in Adaptive Assessment.
 */
@Serializable
enum class AssessmentQuestionType(val displayName: String) {
    MCQ("Multiple Choice"),
    SHORT_ANSWER("Short Answer");

    companion object {
        fun fromString(value: String?): AssessmentQuestionType = when (value?.trim()?.uppercase()) {
            "SHORT_ANSWER", "SHORTANSWER" -> SHORT_ANSWER
            else -> MCQ
        }
    }
}

/**
 * Difficulty levels for Adaptive Assessment generation.
 */
@Serializable
enum class AssessmentDifficulty(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    MIXED("Mixed");

    companion object {
        fun fromString(value: String?): AssessmentDifficulty = when (value?.trim()?.uppercase()) {
            "EASY" -> EASY
            "HARD" -> HARD
            "MIXED" -> MIXED
            else -> MEDIUM
        }
    }
}

/**
 * Domain representation of a validated, source-grounded assessment question.
 */
@Serializable
data class AssessmentQuestion(
    val id: String,
    val question: String,
    val type: AssessmentQuestionType = AssessmentQuestionType.MCQ,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String,
    val topic: String,
    val concept: String = "Core Concept",
    val difficulty: AssessmentDifficulty = AssessmentDifficulty.MEDIUM,
    val materialId: String,
    val documentName: String,
    val pageNumber: Int,
    val sourceChunkId: String,
    val sourceType: MaterialSourceType = MaterialSourceType.PDF,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
) {
    val unitLabel: String
        get() = when (sourceType) {
            MaterialSourceType.PPTX -> "Slide $pageNumber"
            MaterialSourceType.VIDEO -> {
                val startMin = startTimeMs / 60000
                val startSec = (startTimeMs % 60000) / 1000
                val endMin = endTimeMs / 60000
                val endSec = (endTimeMs % 60000) / 1000
                String.format("%02d:%02d–%02d:%02d", startMin, startSec, endMin, endSec)
            }
            else -> "Page $pageNumber"
        }

    val sourceCitation: String
        get() = "$documentName • $unitLabel"

    val toTutorSource: TutorSource
        get() = TutorSource(
            materialId = materialId,
            documentName = documentName,
            pageNumber = pageNumber,
            chunkId = sourceChunkId,
            previewText = explanation,
            sourceType = sourceType,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs
        )
}

/**
 * Complete adaptive assessment container.
 */
@Serializable
data class Assessment(
    val id: String,
    val materialId: String,
    val title: String,
    val topic: String,
    val questions: List<AssessmentQuestion>,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Individual evaluated question result.
 */
@Serializable
data class QuestionResult(
    val questionId: String,
    val questionText: String,
    val selectedAnswer: String,
    val correctAnswer: String,
    val correct: Boolean,
    val explanation: String,
    val topic: String,
    val concept: String,
    val documentName: String,
    val pageNumber: Int,
    val sourceChunkId: String,
    val sourceType: MaterialSourceType = MaterialSourceType.PDF,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
) {
    val unitLabel: String
        get() = when (sourceType) {
            MaterialSourceType.PPTX -> "Slide $pageNumber"
            MaterialSourceType.VIDEO -> {
                val startMin = startTimeMs / 60000
                val startSec = (startTimeMs % 60000) / 1000
                val endMin = endTimeMs / 60000
                val endSec = (endTimeMs % 60000) / 1000
                String.format("%02d:%02d–%02d:%02d", startMin, startSec, endMin, endSec)
            }
            else -> "Page $pageNumber"
        }

    val sourceCitation: String
        get() = "$documentName • $unitLabel"

    val toTutorSource: TutorSource
        get() = TutorSource(
            materialId = "",
            documentName = documentName,
            pageNumber = pageNumber,
            chunkId = sourceChunkId,
            previewText = explanation,
            sourceType = sourceType,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs
        )
}

/**
 * Topic accuracy performance breakdown.
 */
@Serializable
data class TopicPerformance(
    val topic: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val accuracyPercentage: Int
) {
    val isWeak: Boolean
        get() = accuracyPercentage < 60
}

/**
 * Evaluated assessment score, topic analytics, and weak-topic detection.
 */
@Serializable
data class AssessmentResult(
    val assessmentId: String,
    val totalQuestions: Int,
    val correctAnswers: Int,
    val scorePercentage: Int,
    val topicPerformance: List<TopicPerformance>,
    val weakTopics: List<String>,
    val questionResults: List<QuestionResult>
) {
    val hasWeakTopics: Boolean
        get() = weakTopics.isNotEmpty()
}
