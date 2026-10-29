package com.example.domain.model

import kotlinx.serialization.Serializable

/**
 * Role of the participant in a CGA Mentor conversation turn.
 * Aligns strictly with Gemini role specification.
 */
enum class MentorRole {
    USER,
    MODEL;

    companion object {
        fun fromString(role: String?): MentorRole {
            return when (role?.trim()?.lowercase()) {
                "user" -> USER
                "model", "assistant" -> MODEL
                else -> MODEL
            }
        }
    }
}

/**
 * Domain representation of a single message turn in a CGA Mentor chat conversation.
 */
data class MentorChatMessage(
    val id: String,
    val conversationId: String,
    val userId: String,
    val role: MentorRole,
    val content: String,
    val createdAt: Long? = null
)

/**
 * Domain representation of a persistent CGA Mentor conversation thread.
 */
data class MentorConversation(
    val id: String,
    val userId: String,
    val title: String = "New Conversation",
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

/**
 * Relevant CGA student context forwarded to the AI Mentor to provide personalized guidance.
 * Nullable fields represent data points that are legitimately unassessed or unavailable.
 * Missing data is never defaulted to zero to avoid hallucinated metrics.
 */
@Serializable
data class CGAMentorContext(
    // 1. Profile Context
    val studentName: String? = null,
    val department: String? = null,
    val yearOfStudy: String? = null,
    val careerInterest: String? = null,

    // 2. Assessment & Career Context
    val recommendedCareer: String? = null,
    val matchPercentage: Int? = null,

    // 3. Roadmap Progress Context
    val currentRoadmapStage: String? = null,
    val roadmapProgress: Int? = null,
    val completedMilestones: List<String>? = null,
    val nextPendingMilestones: List<String>? = null,

    // 4. Practice Progress Context
    val solvedPracticeCount: Int? = null,
    val totalPracticeCount: Int? = null,

    // 5. Gamification Context
    val currentStreak: Int? = null,
    val xp: Int? = null,

    // 6. Resume Context
    val resumeAtsScore: Int? = null,
    val resumeMatchedSkills: List<String>? = null,
    val resumeMissingSkills: List<String>? = null
)

/**
 * Lightweight proactive focus task derived from verified student progress data.
 */
data class MentorFocusItem(
    val title: String,
    val description: String,
    val priority: String = "MEDIUM",
    val source: String = "GENERAL"
)

