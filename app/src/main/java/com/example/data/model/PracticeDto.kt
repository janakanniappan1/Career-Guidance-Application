package com.example.data.model

import com.example.data.repository.DefaultPracticeCatalog
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.PracticeConstants
import com.example.domain.model.PracticeProblem
import com.example.domain.model.ProjectItem
import com.example.domain.model.QuizAttempt
import com.example.domain.model.QuizQuestion
import com.example.domain.model.UserProblemProgress
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Data Transfer Object for `practice_problems` table in Supabase.
 */
@Serializable
data class PracticeProblemDto(
    @SerialName("id")
    val id: String,
    @SerialName("title")
    val title: String,
    @SerialName("category")
    val category: String, // Coding, Aptitude, Technical
    @SerialName("difficulty")
    val difficulty: String = "Medium", // Easy, Medium, Hard
    @SerialName("description")
    val description: String = "",
    @SerialName("career_id")
    val careerId: String? = null,
    @SerialName("topic")
    val topic: String? = null,
    @SerialName("xp_reward")
    val xpReward: Int? = 20,
    @SerialName("example_input")
    val exampleInput: String? = null,
    @SerialName("example_output")
    val exampleOutput: String? = null,
    @SerialName("explanation")
    val explanation: String? = null,
    @SerialName("constraints")
    val constraints: String? = null,
    @SerialName("hint")
    val hint: String? = null,
    @SerialName("options")
    val options: JsonElement? = null,
    @SerialName("correct_index")
    val correctIndex: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(isSolved: Boolean = false): PracticeProblem {
        val catalogProblem = DefaultPracticeCatalog.getProblemById(id)
            ?: DefaultPracticeCatalog.getProblemById(title)
            ?: DefaultPracticeCatalog.getProblemByQuery(title, description, topic)

        val finalId = catalogProblem?.id ?: id
        val finalTitle = catalogProblem?.title ?: title
        val finalDescription = catalogProblem?.description ?: description
        val finalCategory = catalogProblem?.category ?: category
        val finalDifficulty = catalogProblem?.difficulty ?: difficulty
        val finalCareerId = catalogProblem?.careerId ?: careerId
        val finalTopic = catalogProblem?.topic ?: topic
        val finalXpReward = catalogProblem?.xpReward ?: 20

        val parsedOptions = QuizQuestionDto.parseOptions(options)
        val finalOptions = if (!catalogProblem?.options.isNullOrEmpty() && catalogProblem?.options?.size == 4) {
            catalogProblem!!.options
        } else if (parsedOptions.size == 4) {
            parsedOptions
        } else {
            DefaultPracticeCatalog.getProblemById("tech_01")?.options ?: emptyList()
        }

        val finalCorrectIndex = catalogProblem?.correctIndex ?: correctIndex ?: 0
        val finalExampleInput = catalogProblem?.exampleInput ?: exampleInput?.ifBlank { null }
        val finalExampleOutput = catalogProblem?.exampleOutput ?: exampleOutput?.ifBlank { null }
        val finalExplanation = catalogProblem?.explanation ?: explanation?.ifBlank { null }
        val finalConstraints = catalogProblem?.constraints ?: constraints?.ifBlank { null }
        val finalHint = catalogProblem?.hint ?: hint?.ifBlank { null }

        return PracticeProblem(
            id = finalId,
            title = finalTitle,
            category = finalCategory,
            difficulty = finalDifficulty,
            description = finalDescription,
            isSolved = isSolved,
            xpReward = finalXpReward,
            careerId = finalCareerId,
            topic = finalTopic,
            exampleInput = finalExampleInput,
            exampleOutput = finalExampleOutput,
            explanation = finalExplanation,
            constraints = finalConstraints,
            hint = finalHint,
            options = finalOptions,
            correctIndex = finalCorrectIndex,
            example = catalogProblem?.example,
            solutionSnippet = catalogProblem?.solutionSnippet
        )
    }

    companion object {
        fun fromDomain(domain: PracticeProblem): PracticeProblemDto {
            return PracticeProblemDto(
                id = domain.id,
                title = domain.title,
                category = domain.category,
                difficulty = domain.difficulty,
                description = domain.description,
                careerId = domain.careerId,
                topic = domain.topic,
                xpReward = domain.xpReward,
                exampleInput = domain.exampleInput,
                exampleOutput = domain.exampleOutput,
                explanation = domain.explanation,
                constraints = domain.constraints,
                hint = domain.hint,
                options = if (domain.options.isNotEmpty()) JsonArray(domain.options.map { JsonPrimitive(it) }) else null,
                correctIndex = domain.correctIndex
            )
        }
    }
}

/**
 * Data Transfer Object for `user_problem_progress` table in Supabase.
 */
@Serializable
data class UserProblemProgressDto(
    @SerialName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("problem_id")
    val problemId: String,
    @SerialName("is_solved")
    val isSolved: Boolean = true,
    @SerialName("solved_at")
    val solvedAt: String? = null
) {
    fun toDomain(): UserProblemProgress {
        return UserProblemProgress(
            id = id,
            userId = userId,
            problemId = problemId,
            isSolved = isSolved,
            solvedAt = try {
                if (!solvedAt.isNullOrBlank()) {
                    java.time.Instant.parse(solvedAt).toEpochMilli()
                } else null
            } catch (_: Throwable) {
                null
            }
        )
    }

    companion object {
        fun fromDomain(domain: UserProblemProgress): UserProblemProgressDto {
            return UserProblemProgressDto(
                id = domain.id ?: java.util.UUID.randomUUID().toString(),
                userId = domain.userId,
                problemId = domain.problemId,
                isSolved = domain.isSolved,
                solvedAt = domain.solvedAt?.let {
                    java.time.Instant.ofEpochMilli(it).toString()
                }
            )
        }
    }
}

/**
 * Data Transfer Object for `quiz_questions` table in Supabase.
 */
@Serializable
data class QuizQuestionDto(
    @SerialName("id")
    val id: String = "",
    @SerialName("career_id")
    val careerId: String? = null,
    @SerialName("topic")
    val topic: String? = null,
    @SerialName("question")
    val question: String? = null,
    @SerialName("options")
    val options: JsonElement? = null,
    @SerialName("correct_index")
    val correctIndex: Int? = null,
    @SerialName("explanation")
    val explanation: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("difficulty")
    val difficulty: String? = null,
    @SerialName("category")
    val category: String? = null
) {
    fun toDomain(): QuizQuestion {
        val parsedOptions = parseOptions(options)
        val safeIndex = if (parsedOptions.isNotEmpty()) {
            (correctIndex ?: 0).coerceIn(0, parsedOptions.size - 1)
        } else {
            0
        }
        return QuizQuestion(
            id = id.ifBlank { "q_${System.identityHashCode(this)}" },
            question = question?.trim() ?: "",
            options = parsedOptions,
            correctIndex = safeIndex,
            explanation = explanation?.trim() ?: "",
            careerId = careerId,
            topic = topic
        )
    }

    companion object {
        fun parseOptions(element: JsonElement?): List<String> {
            if (element == null || element is JsonNull) return emptyList()
            return when (element) {
                is JsonArray -> {
                    element.mapNotNull { item ->
                        when (item) {
                            is JsonPrimitive -> item.content.trim()
                            else -> item.toString().trim().removeSurrounding("\"")
                        }
                    }.filter { it.isNotBlank() }
                }
                is JsonPrimitive -> {
                    val raw = element.content.trim()
                    if (raw.startsWith("[") && raw.endsWith("]")) {
                        try {
                            val array = Json.parseToJsonElement(raw) as? JsonArray
                            array?.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }?.filter { it.isNotBlank() }
                                ?: raw.removeSurrounding("[", "]")
                                    .split(",")
                                    .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                    .filter { it.isNotBlank() }
                        } catch (_: Exception) {
                            raw.removeSurrounding("[", "]")
                                .split(",")
                                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                                .filter { it.isNotBlank() }
                        }
                    } else if (raw.contains(",")) {
                        raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    } else if (raw.isNotBlank()) {
                        listOf(raw)
                    } else {
                        emptyList()
                    }
                }
                else -> emptyList()
            }
        }

        fun fromDomain(domain: QuizQuestion): QuizQuestionDto {
            return QuizQuestionDto(
                id = domain.id,
                careerId = domain.careerId,
                topic = domain.topic,
                question = domain.question,
                options = JsonArray(domain.options.map { JsonPrimitive(it) }),
                correctIndex = domain.correctIndex,
                explanation = domain.explanation,
                isActive = true
            )
        }
    }
}

/**
 * Data Transfer Object for `quiz_attempts` table in Supabase.
 */
@Serializable
data class QuizAttemptDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("career_id")
    val careerId: String? = null,
    @SerialName("topic")
    val topic: String = "general",
    @SerialName("score")
    val score: Int = 0,
    @SerialName("total_questions")
    val totalQuestions: Int = 5,
    @SerialName("xp_earned")
    val xpEarned: Int = 0,
    @SerialName("completed_at")
    val completedAt: String? = null
) {
    fun toDomain(): QuizAttempt {
        return QuizAttempt(
            id = id,
            userId = userId,
            topic = topic,
            score = score,
            totalQuestions = totalQuestions,
            xpEarned = xpEarned,
            careerId = careerId,
            completedAt = try {
                if (!completedAt.isNullOrBlank()) {
                    java.time.Instant.parse(completedAt).toEpochMilli()
                } else null
            } catch (_: Throwable) {
                null
            }
        )
    }

    companion object {
        fun fromDomain(domain: QuizAttempt): QuizAttemptDto {
            return QuizAttemptDto(
                id = domain.id ?: java.util.UUID.randomUUID().toString(),
                userId = domain.userId,
                careerId = domain.careerId,
                topic = domain.topic,
                score = domain.score,
                totalQuestions = domain.totalQuestions,
                xpEarned = domain.xpEarned,
                completedAt = domain.completedAt?.let {
                    java.time.Instant.ofEpochMilli(it).toString()
                } ?: java.time.Instant.now().toString()
            )
        }
    }
}

/**
 * Data Transfer Object for `interview_questions` table in Supabase.
 */
@Serializable
data class InterviewQuestionDto(
    @SerialName("id")
    val id: String,
    @SerialName("career_id")
    val careerId: String? = null,
    @SerialName("category")
    val category: String = "Technical",
    @SerialName("question")
    val question: String,
    @SerialName("answer_key")
    val answerKey: String = "",
    @SerialName("tip")
    val tip: String = "",
    @SerialName("topic")
    val topic: String? = null
) {
    fun toDomain(): InterviewQuestion {
        return InterviewQuestion(
            id = id,
            question = question,
            category = category,
            answerKey = answerKey,
            tip = tip,
            careerId = careerId,
            topic = topic
        )
    }
}

/**
 * Data Transfer Object for `student_projects` table in Supabase.
 */
@Serializable
data class StudentProjectDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String = "",
    @SerialName("tech_stack")
    val techStack: List<String> = emptyList(),
    @SerialName("github_url")
    val githubUrl: String = "",
    @SerialName("live_demo_url")
    val liveDemoUrl: String? = null,
    @SerialName("stars")
    val stars: Int? = 0,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): ProjectItem {
        return ProjectItem(
            id = id,
            userId = userId,
            title = title,
            description = description,
            techStack = techStack,
            githubUrl = githubUrl,
            liveDemoUrl = liveDemoUrl,
            stars = stars ?: 0,
            createdAt = try {
                if (!createdAt.isNullOrBlank()) {
                    java.time.Instant.parse(createdAt).toEpochMilli()
                } else null
            } catch (_: Throwable) {
                null
            }
        )
    }

    companion object {
        fun fromDomain(domain: ProjectItem, currentUserId: String): StudentProjectDto {
            return StudentProjectDto(
                id = domain.id,
                userId = if (domain.userId.isNotBlank()) domain.userId else currentUserId,
                title = domain.title,
                description = domain.description,
                techStack = domain.techStack,
                githubUrl = domain.githubUrl,
                liveDemoUrl = domain.liveDemoUrl,
                stars = domain.stars,
                createdAt = domain.createdAt?.let {
                    java.time.Instant.ofEpochMilli(it).toString()
                } ?: java.time.Instant.now().toString()
            )
        }
    }
}

/**
 * Data Transfer Object for `user_daily_activity` table in Supabase.
 */
@Serializable
data class UserDailyActivityDto(
    @SerialName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerialName("user_id")
    val userId: String,
    @SerialName("activity_date")
    val activityDate: String,
    @SerialName("daily_test_completed")
    val dailyTestCompleted: Boolean = false,
    @SerialName("xp_earned")
    val xpEarned: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Data Transfer Object for `resume_analyses` table in Supabase.
 */
@Serializable
data class ResumeAnalysisDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("ats_score")
    val atsScore: Int = 0,
    @SerialName("target_career")
    val targetCareer: String = "",
    @SerialName("matched_skills")
    val matchedSkills: List<String> = emptyList(),
    @SerialName("missing_skills")
    val missingSkills: List<String> = emptyList(),
    @SerialName("tips")
    val tips: List<String> = emptyList(),
    @SerialName("created_at")
    val createdAt: String? = null
) {
    fun toDomain(): com.example.domain.model.ResumeAnalysis {
        return com.example.domain.model.ResumeAnalysis(
            id = id,
            userId = userId,
            atsScore = atsScore,
            targetCareer = targetCareer,
            matchedSkills = matchedSkills,
            missingSkills = missingSkills,
            tips = tips,
            analyzedAt = try {
                if (!createdAt.isNullOrBlank()) {
                    java.time.Instant.parse(createdAt).toEpochMilli()
                } else null
            } catch (_: Throwable) {
                null
            }
        )
    }
}

/**
 * Data Transfer Object for `complete_practice_problem_v2` RPC response.
 */
@Serializable
data class CompleteProblemRpcV2ResponseDto(
    @SerialName("success")
    val success: Boolean = false,
    @SerialName("newly_solved")
    val newlySolved: Boolean = false,
    @SerialName("xp_awarded")
    val xpAwarded: Int = 0,
    @SerialName("problem_id")
    val problemId: String = "",
    @SerialName("total_xp")
    val totalXp: Int? = null,
    @SerialName("message")
    val message: String? = null
) {
    fun toDomain(isCorrect: Boolean = true): com.example.domain.model.PracticeProblemSolveResult {
        return com.example.domain.model.PracticeProblemSolveResult(
            isCorrect = isCorrect,
            xpAwarded = xpAwarded,
            isFirstSolve = newlySolved,
            isSolved = success,
            totalXp = totalXp,
            message = message ?: if (newlySolved) "First solve success" else "Already completed"
        )
    }
}



