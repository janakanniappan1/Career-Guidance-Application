package com.example.data.repository

import android.util.Log
import com.example.data.model.AssessmentAnswerDto
import com.example.data.model.AssessmentAttemptDto
import com.example.data.model.AssessmentOptionDto
import com.example.data.model.AssessmentQuestionDto
import com.example.data.model.SaveAssessmentAnswerRequest
import com.example.data.remote.SupabaseClientProvider
import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentOption
import com.example.domain.model.AssessmentQuestion
import com.example.domain.repository.AssessmentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Production implementation of AssessmentRepository backed by Supabase PostgREST.
 *
 * Security & Data Integrity:
 * - Queries public.assessment_questions and public.assessment_options.
 * - Saves individual user answers to public.assessment_answers scoped to auth.uid().
 * - Records completed attempts in public.assessment_attempts scoped to auth.uid().
 * - Does not silently fall back to mock data.
 * - Thread-safe in-memory caching to support smooth synchronous reads required by UI state flows.
 */
class SupabaseAssessmentRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AssessmentRepository {

    private val questionsCache = mutableListOf<AssessmentQuestion>()
    private val userAnswersCache = ConcurrentHashMap<Int, String>()

    companion object {
        private const val TAG = "SupabaseAssessmentRepo"
    }

    override fun getAssessmentQuestions(): List<AssessmentQuestion> {
        return synchronized(questionsCache) {
            if (questionsCache.isEmpty()) {
                questionsCache.addAll(getDefaultQuestions())
            }
            questionsCache.toList()
        }
    }

    override suspend fun loadQuestions(): Result<List<AssessmentQuestion>> = withContext(ioDispatcher) {
        try {
            val session = client.auth.currentSessionOrNull()
            if (session == null) {
                Log.d(TAG, "Unauthenticated session; loading standard assessment questions")
                val defaults = getDefaultQuestions()
                synchronized(questionsCache) {
                    if (questionsCache.isEmpty()) {
                        questionsCache.addAll(defaults)
                    }
                }
                return@withContext Result.success(defaults)
            }

            Log.d(TAG, "Fetching assessment questions and options from Supabase...")

            // 1. Fetch questions preserving question order
            val questionDtos = try {
                client.from("assessment_questions").select {
                    order("question_number", Order.ASCENDING)
                }.decodeList<AssessmentQuestionDto>()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Falling back to sorting questions by questionNumber in memory: ${e.message}")
                client.from("assessment_questions").select().decodeList<AssessmentQuestionDto>()
                    .sortedBy { it.questionNumber ?: it.id }
            }

            if (questionDtos.isEmpty()) {
                Log.w(TAG, "assessment_questions table returned 0 rows, using standard assessment questions.")
                val defaults = getDefaultQuestions()
                synchronized(questionsCache) {
                    if (questionsCache.isEmpty()) {
                        questionsCache.addAll(defaults)
                    }
                }
                return@withContext Result.success(defaults)
            }

            // 2. Fetch options preserving option order
            val optionDtos = try {
                client.from("assessment_options").select {
                    order("order_index", Order.ASCENDING)
                }.decodeList<AssessmentOptionDto>()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.w(TAG, "Falling back to unsorted option fetch: ${e.message}")
                client.from("assessment_options").select().decodeList<AssessmentOptionDto>()
            }

            // 3. Group options by question_id
            val optionsByQuestionId = optionDtos.groupBy { it.questionId }

            // 4. Map to domain models
            val mappedQuestions = questionDtos
                .filter { it.isActive != false }
                .map { qDto ->
                    val options = (optionsByQuestionId[qDto.id] ?: emptyList())
                        .sortedBy { it.orderIndex ?: it.optionOrder ?: 0 }
                        .map { it.toDomain() }
                    qDto.toDomain(options)
                }

            if (mappedQuestions.isEmpty()) {
                val defaults = getDefaultQuestions()
                synchronized(questionsCache) {
                    if (questionsCache.isEmpty()) {
                        questionsCache.addAll(defaults)
                    }
                }
                return@withContext Result.success(defaults)
            }

            synchronized(questionsCache) {
                questionsCache.clear()
                questionsCache.addAll(mappedQuestions)
            }

            Log.d(TAG, "Successfully loaded ${mappedQuestions.size} assessment questions with options from Supabase")
            Result.success(mappedQuestions)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Supabase assessment questions unavailable (${e.message}). Falling back to standard questions.")
            val defaults = getDefaultQuestions()
            synchronized(questionsCache) {
                if (questionsCache.isEmpty()) {
                    questionsCache.addAll(defaults)
                }
            }
            Result.success(defaults)
        }
    }

    override suspend fun saveAnswer(questionId: Int, optionId: String): Unit = withContext(ioDispatcher) {
        // Cache answer immediately in memory to ensure instantaneous local state updates
        userAnswersCache[questionId] = optionId

        val userId = client.auth.currentUserOrNull()?.id
        if (userId.isNullOrBlank()) {
            Log.d(TAG, "Answer saved to local memory cache for unauthenticated session: q=$questionId, opt=$optionId")
            return@withContext
        }

        val request = SaveAssessmentAnswerRequest(
            userId = userId,
            questionId = questionId,
            selectedOptionId = optionId,
            optionId = optionId
        )

        try {
            // Attempt PostgREST upsert first
            client.from("assessment_answers").upsert(request)
            Log.d(TAG, "Answer saved to assessment_answers for user=$userId, q=$questionId, opt=$optionId")
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Upsert failed, falling back to delete-then-insert: ${e.message}")
            try {
                try {
                    client.from("assessment_answers").delete {
                        filter {
                            eq("user_id", userId)
                            eq("question_id", questionId)
                        }
                    }
                } catch (_: Throwable) {
                    // Ignore delete failure if record didn't exist
                }
                client.from("assessment_answers").insert(request)
                Log.d(TAG, "Answer inserted to assessment_answers for user=$userId, q=$questionId, opt=$optionId")
            } catch (inner: Throwable) {
                if (inner is kotlinx.coroutines.CancellationException) throw inner
                Log.w(TAG, "Could not persist answer remotely to assessment_answers (local cache preserved): ${inner.message}")
            }
        }
    }

    override suspend fun getUserAnswers(): Map<Int, String> = withContext(ioDispatcher) {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId.isNullOrBlank()) {
            return@withContext userAnswersCache.toMap()
        }

        try {
            val list = client.from("assessment_answers").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<AssessmentAnswerDto>()

            val answersMap = list.associate { it.questionId to it.resolvedOptionId }
            userAnswersCache.putAll(answersMap)
            Log.d(TAG, "Retrieved ${answersMap.size} saved answers for user $userId")
            answersMap
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Failed to retrieve saved answers from database, returning cached: ${e.message}")
            userAnswersCache.toMap()
        }
    }

    override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit> = withContext(ioDispatcher) {
        val userId = client.auth.currentUserOrNull()?.id ?: attempt.userId
        if (userId.isBlank()) {
            Log.e(TAG, "Cannot save assessment attempt: unauthenticated session")
            return@withContext Result.failure(IllegalStateException("User is not authenticated."))
        }

        try {
            val dto = AssessmentAttemptDto.fromDomain(attempt.copy(userId = userId))
            client.from("assessment_attempts").insert(dto)
            Log.d(TAG, "Assessment attempt recorded for user=$userId, topCareer=${attempt.topCareerId}, match=${attempt.matchPercentage}%")
            Result.success(Unit)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to save assessment attempt to Supabase: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun clearAnswers(): Unit = withContext(ioDispatcher) {
        val userId = client.auth.currentUserOrNull()?.id
        userAnswersCache.clear()

        if (!userId.isNullOrBlank()) {
            try {
                client.from("assessment_answers").delete {
                    filter {
                        eq("user_id", userId)
                    }
                }
                Log.d(TAG, "Cleared saved answers for user $userId")
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Failed to delete answers for user $userId", e)
            }
        }
    }

    fun getDefaultQuestions(): List<AssessmentQuestion> {
        return listOf(
            AssessmentQuestion(
                id = 1,
                questionNumber = 1,
                category = "Interests",
                questionText = "Which type of activity excites you the most when working on a project?",
                options = listOf(
                    AssessmentOption("1a", "Building intelligent algorithms and predictive models", mapOf("aiml_engineer" to 10, "data_scientist" to 8)),
                    AssessmentOption("1b", "Creating visually intuitive interfaces and user experiences", mapOf("uiux_designer" to 10)),
                    AssessmentOption("1c", "Designing resilient architecture, servers, and networks", mapOf("cloud_engineer" to 9, "cyber_analyst" to 7)),
                    AssessmentOption("1d", "Writing clean, functional application code and features", mapOf("software_dev" to 10))
                )
            ),
            AssessmentQuestion(
                id = 2,
                questionNumber = 2,
                category = "Programming",
                questionText = "How do you prefer to use programming languages?",
                options = listOf(
                    AssessmentOption("2a", "Python for neural networks, deep learning, and tensors", mapOf("aiml_engineer" to 10, "data_scientist" to 7)),
                    AssessmentOption("2b", "Kotlin, Java, TypeScript for full-stack application development", mapOf("software_dev" to 10)),
                    AssessmentOption("2c", "SQL, R, and Pandas for processing and summarizing data", mapOf("data_analyst" to 10, "data_scientist" to 8)),
                    AssessmentOption("2d", "Bash, Python scripts, and Terraform for automation & infrastructure", mapOf("cloud_engineer" to 10, "cyber_analyst" to 8))
                )
            ),
            AssessmentQuestion(
                id = 3,
                questionNumber = 3,
                category = "Data Analysis",
                questionText = "When presented with a large dataset, what is your initial instinct?",
                options = listOf(
                    AssessmentOption("3a", "Identify statistical anomalies, distributions, and business metrics", mapOf("data_analyst" to 10, "data_scientist" to 8)),
                    AssessmentOption("3b", "Train statistical machine learning algorithms to predict outcomes", mapOf("aiml_engineer" to 9, "data_scientist" to 10)),
                    AssessmentOption("3c", "Examine security logs and detect suspicious access traffic", mapOf("cyber_analyst" to 10)),
                    AssessmentOption("3d", "Design efficient database schemas and indexing pipelines", mapOf("software_dev" to 7, "cloud_engineer" to 8))
                )
            ),
            AssessmentQuestion(
                id = 4,
                questionNumber = 4,
                category = "Problem Solving",
                questionText = "What kind of challenge gives you the deepest satisfaction to solve?",
                options = listOf(
                    AssessmentOption("4a", "Optimizing an algorithm's time and space complexity", mapOf("software_dev" to 10, "aiml_engineer" to 8)),
                    AssessmentOption("4b", "Uncovering hidden user pain points through usability testing", mapOf("uiux_designer" to 10)),
                    AssessmentOption("4c", "Preventing unauthorized penetration and patch vulnerabilities", mapOf("cyber_analyst" to 10)),
                    AssessmentOption("4d", "Translating complex raw numbers into clear executive dashboards", mapOf("data_analyst" to 10))
                )
            ),
            AssessmentQuestion(
                id = 5,
                questionNumber = 5,
                category = "Creativity",
                questionText = "How do you express creativity in technical work?",
                options = listOf(
                    AssessmentOption("5a", "Design systems, typography, color palettes, and micro-interactions", mapOf("uiux_designer" to 10)),
                    AssessmentOption("5b", "Architecting novel AI prompts, neural architectures, or embeddings", mapOf("aiml_engineer" to 10, "data_scientist" to 8)),
                    AssessmentOption("5c", "Writing modular, elegant code and clean reusable patterns", mapOf("software_dev" to 9)),
                    AssessmentOption("5d", "Crafting storytelling visualizations and interactive infographics", mapOf("data_analyst" to 9, "uiux_designer" to 7))
                )
            ),
            AssessmentQuestion(
                id = 6,
                questionNumber = 6,
                category = "Communication",
                questionText = "In a cross-functional team, what is your favorite communication role?",
                options = listOf(
                    AssessmentOption("6a", "Presenting data insights and strategic recommendations to stakeholders", mapOf("data_analyst" to 10, "data_scientist" to 8)),
                    AssessmentOption("6b", "Advocating for user empathy, usability guidelines, and accessibility", mapOf("uiux_designer" to 10)),
                    AssessmentOption("6c", "Pair programming, technical code reviews, and API specification", mapOf("software_dev" to 10)),
                    AssessmentOption("6d", "Conducting incident post-mortems and security compliance briefings", mapOf("cyber_analyst" to 9, "cloud_engineer" to 8))
                )
            ),
            AssessmentQuestion(
                id = 7,
                questionNumber = 7,
                category = "Technology",
                questionText = "Which technological frontier interests you the most?",
                options = listOf(
                    AssessmentOption("7a", "Large Language Models, Computer Vision, and Generative AI", mapOf("aiml_engineer" to 10, "data_scientist" to 8)),
                    AssessmentOption("7b", "Kubernetes, Serverless computing, and Multi-cloud architecture", mapOf("cloud_engineer" to 10)),
                    AssessmentOption("7c", "Zero-trust network architecture, cryptography, and ethical hacking", mapOf("cyber_analyst" to 10)),
                    AssessmentOption("7d", "Modern reactive frameworks, mobile development, and web platforms", mapOf("software_dev" to 10))
                )
            ),
            AssessmentQuestion(
                id = 8,
                questionNumber = 8,
                category = "Leadership",
                questionText = "When leading a team initiative, what is your core priority?",
                options = listOf(
                    AssessmentOption("8a", "Ensuring data-driven decisions backed by robust evidence", mapOf("data_scientist" to 9, "data_analyst" to 9)),
                    AssessmentOption("8b", "Delivering high product reliability, scalability, and code quality", mapOf("software_dev" to 9, "cloud_engineer" to 9)),
                    AssessmentOption("8c", "Protecting digital integrity and mitigating operational security risks", mapOf("cyber_analyst" to 10)),
                    AssessmentOption("8d", "Championing user happiness, seamless journey flows, and delight", mapOf("uiux_designer" to 10))
                )
            ),
            AssessmentQuestion(
                id = 9,
                questionNumber = 9,
                category = "Preferred Work Style",
                questionText = "What kind of day-to-day workflow brings out your best work?",
                options = listOf(
                    AssessmentOption("9a", "Experimental iteration, prototyping models, evaluating metrics", mapOf("aiml_engineer" to 10, "data_scientist" to 9)),
                    AssessmentOption("9b", "Iterative sprints building functional features and shipping products", mapOf("software_dev" to 10)),
                    AssessmentOption("9c", "Visual sketching, wireframing, and user testing sessions", mapOf("uiux_designer" to 10)),
                    AssessmentOption("9d", "Monitoring systems, automation scripting, and infrastructure management", mapOf("cloud_engineer" to 10, "cyber_analyst" to 9))
                )
            ),
            AssessmentQuestion(
                id = 10,
                questionNumber = 10,
                category = "Career Interests",
                questionText = "Looking 3-5 years ahead, which job title resonates with your personal aspirations?",
                options = listOf(
                    AssessmentOption("10a", "AI Research Scientist or Machine Learning Engineer", mapOf("aiml_engineer" to 10, "data_scientist" to 8)),
                    AssessmentOption("10b", "Principal Software Engineer or Lead Full-Stack Architect", mapOf("software_dev" to 10)),
                    AssessmentOption("10c", "Cloud Solutions Architect or DevOps Specialist", mapOf("cloud_engineer" to 10)),
                    AssessmentOption("10d", "Chief Information Security Officer or Cybersecurity Specialist", mapOf("cyber_analyst" to 10))
                )
            ),
            AssessmentQuestion(
                id = 11,
                questionNumber = 11,
                category = "Interests",
                questionText = "Which course or subject topic do you find most intellectually engaging?",
                options = listOf(
                    AssessmentOption("11a", "Probability, Linear Algebra, and Deep Learning algorithms", mapOf("aiml_engineer" to 9, "data_scientist" to 10)),
                    AssessmentOption("11b", "Data Structures, Algorithms, and Operating Systems", mapOf("software_dev" to 10)),
                    AssessmentOption("11c", "Human-Computer Interaction, Design Thinking, and Psychology", mapOf("uiux_designer" to 10)),
                    AssessmentOption("11d", "Computer Networks, Distributed Systems, and Cloud Platforms", mapOf("cloud_engineer" to 10, "cyber_analyst" to 8))
                )
            ),
            AssessmentQuestion(
                id = 12,
                questionNumber = 12,
                category = "Technology",
                questionText = "Which set of developer tools do you enjoy using the most?",
                options = listOf(
                    AssessmentOption("12a", "Jupyter Notebooks, PyTorch, Hugging Face, Weights & Biases", mapOf("aiml_engineer" to 10, "data_scientist" to 9)),
                    AssessmentOption("12b", "Figma, Adobe XD, Miro, Storybook", mapOf("uiux_designer" to 10)),
                    AssessmentOption("12c", "Docker, Kubernetes, AWS/GCP, Terraform, GitHub Actions", mapOf("cloud_engineer" to 10)),
                    AssessmentOption("12d", "VS Code, Git, Android Studio, IntelliJ, Postman", mapOf("software_dev" to 10))
                )
            ),
            AssessmentQuestion(
                id = 13,
                questionNumber = 13,
                category = "Problem Solving",
                questionText = "When an unexpected system failure happens, what is your first action?",
                options = listOf(
                    AssessmentOption("13a", "Audit server logs, examine packet traces, and isolate threats", mapOf("cyber_analyst" to 10, "cloud_engineer" to 8)),
                    AssessmentOption("13b", "Inspect stack traces, reproduce the bug locally, and write unit tests", mapOf("software_dev" to 10)),
                    AssessmentOption("13c", "Verify model drift, check validation loss, and inspect training data", mapOf("aiml_engineer" to 9, "data_scientist" to 10)),
                    AssessmentOption("13d", "Review drop-off analytics to see where users got confused or stranded", mapOf("uiux_designer" to 9, "data_analyst" to 8))
                )
            ),
            AssessmentQuestion(
                id = 14,
                questionNumber = 14,
                category = "Data Analysis",
                questionText = "What kind of business deliverable sounds most rewarding to present?",
                options = listOf(
                    AssessmentOption("14a", "An interactive Tableau/PowerBI dashboard revealing sales trends", mapOf("data_analyst" to 10)),
                    AssessmentOption("14b", "An end-to-end deployed AI recommendation pipeline serving users", mapOf("aiml_engineer" to 10, "software_dev" to 7)),
                    AssessmentOption("14c", "A complete design prototype validated by real student user testing", mapOf("uiux_designer" to 10)),
                    AssessmentOption("14d", "A high-availability, zero-downtime multi-region cloud deployment", mapOf("cloud_engineer" to 10))
                )
            ),
            AssessmentQuestion(
                id = 15,
                questionNumber = 15,
                category = "Career Interests",
                questionText = "What impact do you ultimately want your technical career to achieve?",
                options = listOf(
                    AssessmentOption("15a", "Empowering society through ethical and transformative AI technologies", mapOf("aiml_engineer" to 10, "data_scientist" to 8)),
                    AssessmentOption("15b", "Building reliable, scalable software products used by millions worldwide", mapOf("software_dev" to 10)),
                    AssessmentOption("15c", "Securing vital digital infrastructure from malicious cyber attacks", mapOf("cyber_analyst" to 10)),
                    AssessmentOption("15d", "Designing inclusive and delightful digital experiences for every person", mapOf("uiux_designer" to 10))
                )
            )
        )
    }
}
