package com.example.util

import com.example.domain.model.PracticeProblem

/**
 * Validates practice problems for content completeness, MCQ integrity,
 * and absence of placeholder artifacts.
 */
object PracticeContentValidator {

    private val PLACEHOLDER_PATTERNS = listOf(
        "todo",
        "placeholder",
        "lorem ipsum",
        "tbd",
        "standard problem parameters",
        "coming soon",
        "n/a",
        "sample text"
    )

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    /**
     * Validates a single practice problem.
     */
    fun validate(problem: PracticeProblem): ValidationResult {
        val errors = mutableListOf<String>()

        if (problem.id.isBlank()) {
            errors.add("Problem ID must not be blank")
        }

        if (problem.title.isBlank()) {
            errors.add("Problem '${problem.id}' title must not be blank")
        }

        if (problem.category.isBlank()) {
            errors.add("Problem '${problem.id}' category must not be blank")
        }

        if (problem.difficulty.isBlank()) {
            errors.add("Problem '${problem.id}' difficulty must not be blank")
        }

        if (problem.description.isBlank()) {
            errors.add("Problem '${problem.id}' description must not be blank")
        } else if (problem.description.trim().length < 20) {
            errors.add("Problem '${problem.id}' description is too short (${problem.description.trim().length} chars). Must be a comprehensive statement.")
        }

        // XP Policy
        if (problem.xpReward != 20) {
            errors.add("Problem '${problem.id}' xpReward must be 20, but was ${problem.xpReward}")
        }

        // MCQ Options: Exactly 4 distinct options
        if (problem.options.size != 4) {
            errors.add("Problem '${problem.id}' must have exactly 4 options, but has ${problem.options.size}")
        } else {
            problem.options.forEachIndexed { index, option ->
                if (option.isBlank()) {
                    errors.add("Problem '${problem.id}' option $index must not be blank")
                }
            }
            val distinctOptions = problem.options.map { it.trim().lowercase() }.toSet()
            if (distinctOptions.size != 4) {
                errors.add("Problem '${problem.id}' contains duplicate options: ${problem.options}")
            }
        }

        // Correct Index must be in 0..3
        if (problem.correctIndex !in 0..3) {
            errors.add("Problem '${problem.id}' correctIndex must be between 0 and 3, but was ${problem.correctIndex}")
        }

        // Explanation must be meaningful
        if (problem.explanation.isNullOrBlank()) {
            errors.add("Problem '${problem.id}' explanation must not be blank")
        } else if (problem.explanation.trim().length < 15) {
            errors.add("Problem '${problem.id}' explanation is too brief")
        }

        // Hint must be meaningful
        if (problem.hint.isNullOrBlank()) {
            errors.add("Problem '${problem.id}' hint must not be blank")
        }

        // Examples / Constraints
        val hasExample = !problem.example.isNullOrBlank() ||
                (!problem.exampleInput.isNullOrBlank() && !problem.exampleOutput.isNullOrBlank())
        if (!hasExample) {
            errors.add("Problem '${problem.id}' must have example input/output or example text")
        }

        // Placeholder artifact checks
        val textsToCheck = listOf(
            problem.title,
            problem.description,
            problem.explanation ?: "",
            problem.hint ?: "",
            problem.exampleInput ?: "",
            problem.exampleOutput ?: "",
            problem.example ?: "",
            problem.constraints ?: ""
        ) + problem.options

        for (text in textsToCheck) {
            val lower = text.lowercase()
            for (pattern in PLACEHOLDER_PATTERNS) {
                if (lower.contains(pattern)) {
                    errors.add("Problem '${problem.id}' contains placeholder artifact '$pattern' in text: \"$text\"")
                }
            }
        }

        return ValidationResult(isValid = errors.isEmpty(), errors = errors)
    }

    /**
     * Validates a collection of practice problems and returns a summary.
     */
    fun validateAll(problems: List<PracticeProblem>): Map<String, List<String>> {
        val failureMap = mutableMapOf<String, List<String>>()
        for (problem in problems) {
            val result = validate(problem)
            if (!result.isValid) {
                failureMap[problem.id] = result.errors
            }
        }
        return failureMap
    }
}
