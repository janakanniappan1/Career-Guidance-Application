package com.example.data.repository

import com.example.domain.model.AssessmentAttempt
import com.example.domain.model.AssessmentOption
import com.example.domain.model.AssessmentQuestion
import com.example.domain.repository.AssessmentRepository

// TODO: Replace MockAssessmentRepository with SupabaseAssessmentRepository
class MockAssessmentRepository : AssessmentRepository {

    private val userAnswers = mutableMapOf<Int, String>()
    private val assessmentAttempts = mutableListOf<AssessmentAttempt>()

    private val questions: List<AssessmentQuestion> = listOf(
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

    override fun getAssessmentQuestions(): List<AssessmentQuestion> = questions

    override suspend fun saveAnswer(questionId: Int, optionId: String) {
        userAnswers[questionId] = optionId
    }

    override suspend fun getUserAnswers(): Map<Int, String> = userAnswers.toMap()

    override suspend fun saveAssessmentAttempt(attempt: AssessmentAttempt): Result<Unit> {
        val resolvedAttempt = attempt.copy(
            id = attempt.id ?: "att_${assessmentAttempts.size + 1}",
            completedAt = attempt.completedAt ?: System.currentTimeMillis()
        )
        assessmentAttempts.add(resolvedAttempt)
        return Result.success(Unit)
    }

    override suspend fun clearAnswers() {
        userAnswers.clear()
    }
}
