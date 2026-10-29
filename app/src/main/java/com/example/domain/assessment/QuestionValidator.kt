package com.example.domain.assessment

import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType

/**
 * Question fingerprinting utility to prevent duplicate questions.
 * Normalizes question text (lowercase, removes punctuation, collapses whitespace).
 */
object QuestionFingerprint {

    fun generateFingerprint(questionText: String): String {
        return questionText
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}

/**
 * In-memory manager for tracking generated question fingerprints across the current session.
 * Extensible for persistent storage integration in future steps.
 */
class QuestionFingerprintManager {

    private val sessionFingerprints = mutableSetOf<String>()

    fun isSeen(questionText: String): Boolean {
        val fp = QuestionFingerprint.generateFingerprint(questionText)
        return fp in sessionFingerprints
    }

    fun record(questionText: String): Boolean {
        val fp = QuestionFingerprint.generateFingerprint(questionText)
        return sessionFingerprints.add(fp)
    }

    fun clear() {
        sessionFingerprints.clear()
    }
}

/**
 * Strict validator for generated questions.
 * Enforces all 9 validation rules before any question can be shown to a student.
 */
class QuestionValidator(
    private val fingerprintManager: QuestionFingerprintManager = QuestionFingerprintManager()
) {

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String? = null
    )

    fun validateQuestion(
        question: AssessmentQuestion,
        validChunkIds: Set<String>,
        seenInCurrentAssessment: MutableSet<String>
    ): ValidationResult {
        // 1. Question is non-empty
        if (question.question.trim().isBlank()) {
            return ValidationResult(false, "Question text is empty")
        }

        // 2. MCQ has exactly 4 options
        if (question.type == AssessmentQuestionType.MCQ) {
            if (question.options.size != 4) {
                return ValidationResult(false, "MCQ must have exactly 4 options (got ${question.options.size})")
            }
            if (question.options.any { it.trim().isBlank() }) {
                return ValidationResult(false, "MCQ options contain blank text")
            }
            // All 4 options must be distinct
            val distinctOptions = question.options.map { it.trim().lowercase() }.distinct()
            if (distinctOptions.size < 4) {
                return ValidationResult(false, "MCQ options contain duplicates")
            }

            // 3. Correct answer exists in options
            val matchesOption = question.options.any { option ->
                option.trim().equals(question.correctAnswer.trim(), ignoreCase = true)
            }
            if (!matchesOption) {
                return ValidationResult(false, "Correct answer '${question.correctAnswer}' is not in options")
            }
        } else {
            // For SHORT_ANSWER, correct answer must not be blank
            if (question.correctAnswer.trim().isBlank()) {
                return ValidationResult(false, "Short answer correct answer is blank")
            }
        }

        // 4. Explanation exists
        if (question.explanation.trim().isBlank()) {
            return ValidationResult(false, "Explanation is empty")
        }

        // 5. Topic exists
        if (question.topic.trim().isBlank()) {
            return ValidationResult(false, "Topic is empty")
        }

        // 6. Difficulty exists (guaranteed by enum, check not null)
        // AssessmentDifficulty is non-null in data class

        // 7. sourceChunkId exists
        if (question.sourceChunkId.trim().isBlank()) {
            return ValidationResult(false, "sourceChunkId is blank")
        }

        // 8. sourceChunkId belongs to supplied KnowledgeChunks
        if (question.sourceChunkId !in validChunkIds) {
            return ValidationResult(false, "sourceChunkId '${question.sourceChunkId}' does not belong to supplied chunks")
        }

        // 9. No duplicate questions
        val fingerprint = QuestionFingerprint.generateFingerprint(question.question)
        if (fingerprint in seenInCurrentAssessment) {
            return ValidationResult(false, "Duplicate question within assessment")
        }

        return ValidationResult(true)
    }

    /**
     * Filters a list of raw questions, discarding invalid or duplicate items.
     * Records fingerprints of accepted questions.
     */
    fun filterValidQuestions(
        rawQuestions: List<AssessmentQuestion>,
        suppliedChunks: List<KnowledgeChunk>
    ): List<AssessmentQuestion> {
        val validChunkIds = suppliedChunks.map { it.id }.toSet()
        val seenInCurrentAssessment = mutableSetOf<String>()
        val validList = mutableListOf<AssessmentQuestion>()

        for (q in rawQuestions) {
            val result = validateQuestion(q, validChunkIds, seenInCurrentAssessment)
            if (result.isValid) {
                val fp = QuestionFingerprint.generateFingerprint(q.question)
                seenInCurrentAssessment.add(fp)
                fingerprintManager.record(q.question)
                validList.add(q)
            }
        }

        return validList
    }
}
