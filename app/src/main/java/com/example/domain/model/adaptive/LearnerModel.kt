package com.example.domain.model.adaptive

import kotlinx.serialization.Serializable

/**
 * Subjective confidence level assessed from learning signals and assessment consistency.
 */
@Serializable
enum class ConfidenceLevel(val displayName: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromAccuracyAndSignals(accuracy: Int, clarificationCount: Int): ConfidenceLevel {
            return when {
                clarificationCount >= 2 -> LOW
                accuracy >= 80 -> HIGH
                accuracy >= 50 -> MEDIUM
                else -> LOW
            }
        }
    }
}

/**
 * Objective mastery classification categories.
 * Strict honesty requirement: Does not use misleading labels such as "Expert".
 */
@Serializable
enum class MasteryCategory(val displayName: String) {
    STRONG("Strong"),           // 80–100%
    DEVELOPING("Developing"),   // 60–79%
    NEEDS_PRACTICE("Needs Practice"); // 0–59%

    companion object {
        fun fromScore(score: Int): MasteryCategory = when {
            score >= 80 -> STRONG
            score >= 60 -> DEVELOPING
            else -> NEEDS_PRACTICE
        }
    }
}

/**
 * Interaction signal generated from conversations with the Source-Grounded AI Tutor.
 */
@Serializable
enum class TutorSignalType {
    ASKED_FOR_EXPLANATION,
    ASKED_FOR_EXAMPLE,
    ASKED_FOR_CLARIFICATION
}

/**
 * Conversation signal event captured from tutor interactions.
 */
@Serializable
data class TutorLearningSignal(
    val topic: String,
    val timestamp: Long = System.currentTimeMillis(),
    val signalType: TutorSignalType = TutorSignalType.ASKED_FOR_EXPLANATION
)

/**
 * Topic-specific learner mastery record.
 * Uses the explainable "Weighted Evidence Mastery Model".
 */
@Serializable
data class TopicMastery(
    val materialId: String,
    val topic: String,
    val masteryScore: Int = 50,
    val questionsAttempted: Int = 0,
    val questionsCorrect: Int = 0,
    val lastAttemptAt: Long = System.currentTimeMillis(),
    val recentAccuracy: Int = 0,
    val confidenceLevel: ConfidenceLevel = ConfidenceLevel.MEDIUM
) {
    val category: MasteryCategory
        get() = MasteryCategory.fromScore(masteryScore)

    val isWeak: Boolean
        get() = masteryScore < 60

    val isStrong: Boolean
        get() = masteryScore >= 80

    val formattedAccuracy: String
        get() = "$recentAccuracy% recent accuracy"

    val formattedQuestionsCount: String
        get() = "$questionsAttempted questions"
}
