package com.example.data.repository

import com.example.domain.model.CareerRecommendation
import com.example.domain.model.OtherCareerMatch
import com.example.domain.repository.AssessmentRepository
import com.example.domain.repository.CareerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// TODO: Replace MockCareerRepository with SupabaseCareerRepository
class MockCareerRepository(
    private val assessmentRepository: AssessmentRepository
) : CareerRepository {

    private val _latestRecommendation = MutableStateFlow<CareerRecommendation?>(null)

    // Pre-calculated career meta info
    private val careerDetailsMap = mapOf(
        "aiml_engineer" to CareerRecommendation(
            careerId = "aiml_engineer",
            title = "AI / Machine Learning Engineer",
            matchPercentage = 91,
            reasonForRecommendation = "Your answers indicate strong interest in programming, mathematics, problem solving, neural networks, and artificial intelligence innovations.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("data_scientist", "Data Scientist", 86, "Strong synergy in statistical modeling"),
                OtherCareerMatch("software_dev", "Software Developer", 82, "Solid programming foundation"),
                OtherCareerMatch("cloud_engineer", "Cloud Engineer", 75, "High relevance for model deployment")
            ),
            recommendedSkills = listOf("Python", "PyTorch / TensorFlow", "Mathematics & Linear Algebra", "Machine Learning Algorithms", "MLOps & Docker", "Deep Learning & NLP"),
            roleOverview = "Architect, train, and deploy intelligent algorithms and generative AI models that solve complex analytical challenges at scale.",
            averageSalary = "$110,000 - $165,000",
            jobGrowth = "+36% (Much faster than average)"
        ),
        "software_dev" to CareerRecommendation(
            careerId = "software_dev",
            title = "Software Developer",
            matchPercentage = 89,
            reasonForRecommendation = "Your responses demonstrate high proficiency in structured programming, algorithmic problem solving, clean system design, and building user-centric features.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("cloud_engineer", "Cloud Engineer", 83, "Natural extension into distributed systems"),
                OtherCareerMatch("aiml_engineer", "AI / ML Engineer", 79, "Complements backend model integration"),
                OtherCareerMatch("cyber_analyst", "Cybersecurity Analyst", 74, "Strong focus on secure coding practices")
            ),
            recommendedSkills = listOf("Kotlin / Java", "Data Structures & Algorithms", "Git & GitHub", "REST APIs & Databases", "System Design", "Unit & Integration Testing"),
            roleOverview = "Design, construct, test, and maintain robust client and server software systems that power digital products.",
            averageSalary = "$95,000 - $145,000",
            jobGrowth = "+25% (High Demand)"
        ),
        "data_analyst" to CareerRecommendation(
            careerId = "data_analyst",
            title = "Data Analyst",
            matchPercentage = 88,
            reasonForRecommendation = "Your profile shows sharp intuition for business metrics, statistical deduction, data storytelling, and translating metrics into strategic clarity.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("data_scientist", "Data Scientist", 85, "Advanced statistical progression"),
                OtherCareerMatch("aiml_engineer", "AI / ML Engineer", 80, "Applies models to analyzed datasets"),
                OtherCareerMatch("uiux_designer", "UI/UX Designer", 76, "Visualizing user journey data")
            ),
            recommendedSkills = listOf("SQL & Database Querying", "Tableau / PowerBI", "Python (Pandas, Numpy)", "Statistical Analysis", "Data Storytelling & Reporting", "Excel Advanced"),
            roleOverview = "Transform complex raw corporate data into actionable visualizations and strategic insights that drive key business leadership decisions.",
            averageSalary = "$80,000 - $115,000",
            jobGrowth = "+23% (High Demand)"
        ),
        "data_scientist" to CareerRecommendation(
            careerId = "data_scientist",
            title = "Data Scientist",
            matchPercentage = 90,
            reasonForRecommendation = "Your answers exhibit strong mathematical logic, predictive experimentation, and deep curiosity for finding hidden patterns inside massive datasets.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("aiml_engineer", "AI / ML Engineer", 88, "Overlapping algorithm engineering"),
                OtherCareerMatch("data_analyst", "Data Analyst", 84, "Foundational exploratory analysis"),
                OtherCareerMatch("software_dev", "Software Developer", 78, "Production data engineering pipelines")
            ),
            recommendedSkills = listOf("Python & R", "Applied Statistics & Probability", "Machine Learning Models", "Feature Engineering", "Data Visualization", "Big Data (Spark)"),
            roleOverview = "Combine computer science and statistics to analyze data, build predictive systems, and discover transformative organizational insights.",
            averageSalary = "$105,000 - $155,000",
            jobGrowth = "+35% (Exceptional Growth)"
        ),
        "uiux_designer" to CareerRecommendation(
            careerId = "uiux_designer",
            title = "UI/UX Designer",
            matchPercentage = 92,
            reasonForRecommendation = "Your responses highlight deep user empathy, visual creativity, intuitive aesthetics, and passion for crafting accessible digital journeys.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("software_dev", "Software Developer", 80, "Frontend & design systems implementation"),
                OtherCareerMatch("data_analyst", "Data Analyst", 75, "User behavioral analytics"),
                OtherCareerMatch("aiml_engineer", "AI / ML Engineer", 70, "Human-AI interaction design")
            ),
            recommendedSkills = listOf("Figma & FigJam", "Wireframing & Prototyping", "User Research & Usability Testing", "Design Systems & Typography", "Information Architecture", "Micro-interactions"),
            roleOverview = "Craft meaningful, intuitive, and accessible user experiences that bridge human needs with cutting-edge technology.",
            averageSalary = "$85,000 - $130,000",
            jobGrowth = "+19% (Steady Demand)"
        ),
        "cyber_analyst" to CareerRecommendation(
            careerId = "cyber_analyst",
            title = "Cybersecurity Analyst",
            matchPercentage = 91,
            reasonForRecommendation = "Your answers demonstrate keen analytical vigilance, interest in cryptography, threat modeling, and passion for defending critical digital infrastructure.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("cloud_engineer", "Cloud Engineer", 85, "Cloud security & IAM architecture"),
                OtherCareerMatch("software_dev", "Software Developer", 79, "Secure application development"),
                OtherCareerMatch("data_analyst", "Data Analyst", 73, "SIEM log analysis and forensic telemetry")
            ),
            recommendedSkills = listOf("Network Security & Protocols", "Ethical Hacking & Penetration Testing", "SIEM & Log Analysis", "Cryptography", "Linux & Bash Scripting", "Compliance & Risk Management"),
            roleOverview = "Monitor, prevent, and respond to cyber threats across enterprise networks, securing confidential data from unauthorized access.",
            averageSalary = "$95,000 - $145,000",
            jobGrowth = "+32% (Critical Demand)"
        ),
        "cloud_engineer" to CareerRecommendation(
            careerId = "cloud_engineer",
            title = "Cloud Engineer",
            matchPercentage = 90,
            reasonForRecommendation = "Your responses reflect strong passion for distributed infrastructure, scalability, cloud virtualization, DevOps automation, and resilient network architectures.",
            otherSuitableCareers = listOf(
                OtherCareerMatch("software_dev", "Software Developer", 84, "Backend & microservices development"),
                OtherCareerMatch("cyber_analyst", "Cybersecurity Analyst", 82, "Cloud governance & defense"),
                OtherCareerMatch("aiml_engineer", "AI / ML Engineer", 78, "Cloud MLOps infrastructure")
            ),
            recommendedSkills = listOf("AWS / GCP / Azure", "Docker & Kubernetes", "Terraform (IaC)", "Linux System Administration", "CI/CD Pipelines", "Networking & Load Balancing"),
            roleOverview = "Architect and manage reliable, scalable cloud infrastructure and continuous deployment pipelines that keep global platforms online 24/7.",
            averageSalary = "$100,000 - $150,000",
            jobGrowth = "+27% (High Demand)"
        )
    )

    override suspend fun calculateRecommendation(answers: Map<Int, String>): CareerRecommendation {
        val questions = assessmentRepository.getAssessmentQuestions()
        val scoreAccumulator = mutableMapOf<String, Int>(
            "aiml_engineer" to 0,
            "software_dev" to 0,
            "data_analyst" to 0,
            "data_scientist" to 0,
            "uiux_designer" to 0,
            "cyber_analyst" to 0,
            "cloud_engineer" to 0
        )

        val maxPossibleScores = mutableMapOf<String, Int>()
        scoreAccumulator.keys.forEach { maxPossibleScores[it] = 0 }

        // Calculate total possible score for each career across all questions
        for (q in questions) {
            val careerMaxInQuestion = mutableMapOf<String, Int>()
            for (opt in q.options) {
                opt.scores.forEach { (career, score) ->
                    careerMaxInQuestion[career] = maxOf(careerMaxInQuestion[career] ?: 0, score)
                }
            }
            careerMaxInQuestion.forEach { (career, maxScore) ->
                maxPossibleScores[career] = (maxPossibleScores[career] ?: 0) + maxScore
            }
        }

        // Score based on student answers
        for (q in questions) {
            val selectedOptionId = answers[q.id]
            val selectedOption = q.options.find { it.id == selectedOptionId }
            selectedOption?.scores?.forEach { (career, points) ->
                scoreAccumulator[career] = (scoreAccumulator[career] ?: 0) + points
            }
        }

        // Calculate percentage for each career (normalized between 55% and 96% for natural student feel)
        val percentages = scoreAccumulator.mapValues { (career, rawScore) ->
            val maxPoints = maxPossibleScores[career] ?: 1
            if (maxPoints == 0 || rawScore == 0) {
                // If unanswered or zero, return baseline based on career interest
                75
            } else {
                val ratio = rawScore.toFloat() / maxPoints.toFloat()
                // Map ratio (0..1) to realistic range 65%..96%
                (65 + (ratio * 31)).toInt().coerceIn(65, 96)
            }
        }

        // Find top career
        val sortedCareers = percentages.entries.sortedByDescending { it.value }
        val topCareerEntry = sortedCareers.firstOrNull() ?: MapEntry("aiml_engineer", 91)
        val topCareerKey = topCareerEntry.key
        val topPercentage = topCareerEntry.value

        val baseInfo = careerDetailsMap[topCareerKey] ?: careerDetailsMap["aiml_engineer"]!!

        // Construct secondary careers
        val secondaryCareers = sortedCareers.drop(1).take(3).map { entry ->
            val info = careerDetailsMap[entry.key]
            OtherCareerMatch(
                careerId = entry.key,
                title = info?.title ?: entry.key,
                matchPercentage = entry.value,
                shortDesc = "Strong secondary alignment (${entry.value}% Match)"
            )
        }

        val finalRecommendation = baseInfo.copy(
            id = null,
            matchPercentage = topPercentage,
            otherSuitableCareers = secondaryCareers,
            createdAt = System.currentTimeMillis()
        )

        _latestRecommendation.value = finalRecommendation
        return finalRecommendation
    }

    override fun getLatestRecommendation(): Flow<CareerRecommendation?> = _latestRecommendation.asStateFlow()

    override suspend fun loadLatestRecommendation(userId: String?): Result<CareerRecommendation?> {
        return Result.success(_latestRecommendation.value)
    }

    override suspend fun getCareerDetails(careerId: String): CareerRecommendation? {
        return careerDetailsMap[careerId]
    }

    override fun getAllCareers(): List<CareerRecommendation> {
        return careerDetailsMap.values.toList()
    }

    override suspend fun saveRecommendation(recommendation: CareerRecommendation) {
        _latestRecommendation.value = recommendation
    }

    private data class MapEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>
}
