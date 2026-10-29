package com.example.domain.model

/**
 * Global practice problem.
 * Maps to `practice_problems` table in Supabase.
 * `isSolved` is computed at runtime from `user_problem_progress`.
 */
data class PracticeProblem(
    val id: String,
    val title: String,
    val category: String, // Coding, Aptitude, Technical, Interview
    val difficulty: String, // Easy, Medium, Hard
    val description: String,
    val isSolved: Boolean = false,
    val xpReward: Int = 20,
    val careerId: String? = null,
    val topic: String? = null,
    val exampleInput: String? = null,
    val exampleOutput: String? = null,
    val explanation: String? = null,
    val constraints: String? = null,
    val hint: String? = null,
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val example: String? = null,
    val solutionSnippet: String? = null
)

/**
 * User-specific solved status for practice problems.
 * Maps to `user_problem_progress` table in Supabase.
 */
data class UserProblemProgress(
    val id: String? = null,
    val userId: String,
    val problemId: String,
    val isSolved: Boolean = true,
    val solvedAt: Long? = null
)

/**
 * Global interview prep question.
 * Maps to `interview_questions` table in Supabase.
 */
data class InterviewQuestion(
    val id: String,
    val question: String,
    val category: String, // HR, Technical, System Design
    val answerKey: String,
    val tip: String,
    val careerId: String? = null,
    val topic: String? = null
)

/**
 * Practice Category Constants and Category Matching Helpers.
 */
object PracticeCategories {
    const val CODING = "Coding"
    const val APTITUDE = "Aptitude"
    const val TECHNICAL = "Technical"
    const val INTERVIEW = "Interview"

    fun isCoding(category: String): Boolean = category.equals(CODING, ignoreCase = true)
    fun isAptitude(category: String): Boolean = category.equals(APTITUDE, ignoreCase = true)
    fun isTechnical(category: String): Boolean = category.equals(TECHNICAL, ignoreCase = true)

    fun matchesTab(category: String, tabIndex: Int): Boolean {
        return when (tabIndex) {
            0 -> isCoding(category)
            1 -> isAptitude(category)
            2 -> isTechnical(category)
            else -> false
        }
    }
}

/**
 * Practice and Gamification XP Constants.
 */
object PracticeConstants {
    const val DAILY_SPEED_TEST_MAX_XP = 50
    const val SPEED_CHALLENGE_MAX_XP = 100
    const val PRACTICE_PROBLEM_SOLVED_XP = 20
    const val TOPIC_DAILY_SPEED_TEST = "daily_speed_test"
    const val TOPIC_SPEED_CHALLENGE = "speed_challenge"
}

/**
 * Universal XP Calculator adhering to the Core CGA Principle:
 * earnedXP = round((correctAnswers / totalQuestions) * maximumXP)
 *
 * Rules:
 * - 0 correct -> 0 XP
 * - full marks -> maximumXP
 * - partial marks -> proportional XP (whole number)
 * - never exceed maximumXP
 * - never return negative XP
 * - handles totalQuestions <= 0 safely (returns 0)
 */
object PracticeXpCalculator {
    fun calculateEarnedXp(
        correctAnswers: Int,
        totalQuestions: Int,
        maximumXp: Int
    ): Int {
        if (totalQuestions <= 0 || correctAnswers <= 0 || maximumXp <= 0) {
            return 0
        }
        val boundedCorrect = minOf(correctAnswers, totalQuestions)
        val ratio = boundedCorrect.toDouble() / totalQuestions.toDouble()
        val earned = kotlin.math.round(ratio * maximumXp).toInt()
        return earned.coerceIn(0, maximumXp)
    }
}

/**
 * Quiz operation mode with associated metadata and maximum XP reward.
 */
enum class QuizMode(
    val displayName: String,
    val defaultTopic: String,
    val maximumXp: Int
) {
    DAILY_SPEED_TEST(
        displayName = "Daily Speed Test",
        defaultTopic = PracticeConstants.TOPIC_DAILY_SPEED_TEST,
        maximumXp = PracticeConstants.DAILY_SPEED_TEST_MAX_XP
    ),
    SPEED_CHALLENGE(
        displayName = "Speed Challenge",
        defaultTopic = PracticeConstants.TOPIC_SPEED_CHALLENGE,
        maximumXp = PracticeConstants.SPEED_CHALLENGE_MAX_XP
    );

    fun calculateEarnedXp(correctAnswers: Int, totalQuestions: Int): Int {
        return PracticeXpCalculator.calculateEarnedXp(
            correctAnswers = correctAnswers,
            totalQuestions = totalQuestions,
            maximumXp = maximumXp
        )
    }
}

/**
 * Global speed placement quiz question.
 * Maps to `quiz_questions` table in Supabase.
 */
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val careerId: String? = null,
    val topic: String? = null
)

/**
 * User-specific quiz completion attempt.
 * Maps to `quiz_attempts` table in Supabase.
 */
data class QuizAttempt(
    val id: String? = null,
    val userId: String,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val xpEarned: Int,
    val completedAt: Long? = null,
    val careerId: String? = null
)

/**
 * User-specific portfolio project.
 * Maps to `student_projects` table in Supabase.
 */
data class ProjectItem(
    val id: String? = null,
    val userId: String,
    val title: String,
    val description: String,
    val techStack: List<String> = emptyList(),
    val githubUrl: String = "",
    val liveDemoUrl: String? = null,
    val stars: Int = 0,
    val createdAt: Long? = null
)

/**
 * User-specific resume analysis result.
 * Maps to `resume_analyses` table in Supabase.
 */
data class ResumeAnalysis(
    val id: String? = null,
    val userId: String = "",
    val atsScore: Int = 0,
    val targetCareer: String = "AI / Machine Learning Engineer",
    val matchedSkills: List<String> = listOf("Python", "Machine Learning", "Data Analysis", "Git", "SQL"),
    val missingSkills: List<String> = listOf("PyTorch / TensorFlow", "Docker / MLOps", "System Design"),
    val tips: List<String> = listOf(
        "Quantify your project results (e.g., 'Achieved 92% accuracy on 50k test records')",
        "Add links to your live demo applications and GitHub repositories",
        "Highlight your problem solving achievements and contest ratings"
    ),
    val analyzedAt: Long? = null
)

/**
 * Result of completing or submitting an answer to a practice problem.
 */
data class PracticeProblemSolveResult(
    val isCorrect: Boolean,
    val xpAwarded: Int,
    val isFirstSolve: Boolean,
    val isSolved: Boolean = true,
    val totalXp: Int? = null,
    val message: String? = null
)

data class AdminOverview(
    val totalStudents: Int = 485,
    val averageReadiness: Int = 76,
    val placementRate: Int = 89,
    val totalAssessmentsCompleted: Int = 412,
    val topRole: String = "AI / Machine Learning Engineer",
    val careerDistribution: Map<String, Int> = mapOf(
        "AI / Machine Learning" to 34,
        "Software Engineering" to 28,
        "Data Analytics" to 18,
        "Cloud & DevOps" to 12,
        "Cybersecurity" to 8
    )
)
