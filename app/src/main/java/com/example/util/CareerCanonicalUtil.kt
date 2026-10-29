package com.example.util

/**
 * Utility for resolving and canonicalizing career identifiers across CGA.
 *
 * Guarantees:
 * - Action/Navigation IDs (e.g., "take_career_assessment") are NEVER treated as career keys.
 * - All career IDs match canonical Supabase database keys (e.g., "data_analyst", "aiml_engineer").
 * - Missing or non-canonical career strings resolve safely without exposing internal placeholders.
 */
object CareerCanonicalUtil {

    const val DEFAULT_CAREER_ID = "aiml_engineer"
    const val DEFAULT_CAREER_TITLE = "AI / Machine Learning Engineer"

    const val CAREER_AIML = "aiml_engineer"
    const val CAREER_SOFTWARE_DEV = "software_dev"
    const val CAREER_DATA_ANALYST = "data_analyst"
    const val CAREER_DATA_SCIENTIST = "data_scientist"
    const val CAREER_UIUX = "uiux_designer"
    const val CAREER_CYBER_SECURITY = "cyber_analyst"
    const val CAREER_CLOUD_ENGINEER = "cloud_engineer"

    fun getCareerTitle(careerId: String?): String = toCanonicalCareerTitle(careerId)

    /**
     * Set of canonical database career IDs.
     */
    val CANONICAL_CAREER_IDS = setOf(
        "aiml_engineer",
        "software_dev",
        "data_analyst",
        "data_scientist",
        "uiux_designer",
        "cyber_analyst",
        "cloud_engineer"
    )

    /**
     * Map of canonical IDs to standardized display titles.
     */
    val CAREER_TITLES = mapOf(
        "aiml_engineer" to "AI / Machine Learning Engineer",
        "software_dev" to "Software Developer",
        "data_analyst" to "Data Analyst",
        "data_scientist" to "Data Scientist",
        "uiux_designer" to "UI/UX Designer",
        "cyber_analyst" to "Cybersecurity Analyst",
        "cloud_engineer" to "Cloud & DevOps Engineer"
    )

    /**
     * Detects if an identifier represents a navigation action or placeholder rather than a valid career.
     */
    fun isActionOrPlaceholderId(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        val clean = id.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
        return clean.isBlank() ||
                clean == "null" ||
                clean == "undefined" ||
                clean == "none" ||
                clean.contains("takecareerassessment") ||
                clean.contains("takeassessment") ||
                clean.contains("careerassessment") ||
                clean.contains("notassessed") ||
                clean.contains("careerassessed") ||
                clean.contains("assessment") ||
                clean.contains("dashboard") ||
                clean.contains("explore")
    }

    /**
     * Normalizes an arbitrary career string into snake_case.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        return input.trim().lowercase()
            .replace("&", "and")
            .replace("/", " ")
            .replace("-", "_")
            .replace(Regex("[^a-z0-9_\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .replace(Regex("_+"), "_")
            .replace("ai_ml", "aiml")
    }

    /**
     * Resolves any career ID, title, or alias into a canonical career identifier, or null if unrecognized.
     */
    fun toCanonicalCareerIdOrNull(input: String?): String? {
        if (isActionOrPlaceholderId(input)) {
            return null
        }

        val normalized = normalize(input)

        if (CANONICAL_CAREER_IDS.contains(normalized)) {
            return normalized
        }

        return when {
            // AI / ML
            normalized == "aiml" ||
            normalized == "ai_engineer" ||
            normalized.contains("machine_learning") ||
            normalized.contains("artificial_intelligence") ||
            normalized.contains("ai_machine_learning") -> "aiml_engineer"

            // Software Development
            normalized == "software_developer" ||
            normalized == "software_engineering" ||
            normalized.contains("full_stack") ||
            normalized.contains("fullstack") ||
            normalized.contains("software_dev") -> "software_dev"

            // Data Science
            normalized.contains("data_scien") ||
            normalized == "data_scientist" -> "data_scientist"

            // Data Analytics
            normalized.contains("data_anal") ||
            normalized.contains("business_anal") ||
            normalized == "data_analytics" -> "data_analyst"

            // UI/UX
            normalized.contains("uiux") ||
            normalized.contains("ui_ux") ||
            normalized.contains("product_designer") ||
            normalized.contains("user_experience") -> "uiux_designer"

            // Cybersecurity
            normalized.contains("cyber") ||
            normalized.contains("infosec") ||
            normalized.contains("cybersecurity") -> "cyber_analyst"

            // Cloud & DevOps
            normalized.contains("cloud") ||
            normalized.contains("devops") -> "cloud_engineer"

            else -> null
        }
    }

    /**
     * Resolves any career ID, title, or alias into a canonical career identifier.
     * If invalid or unknown, falls back to [DEFAULT_CAREER_ID].
     */
    fun toCanonicalCareerId(input: String?): String {
        return toCanonicalCareerIdOrNull(input) ?: DEFAULT_CAREER_ID
    }

    /**
     * Resolves a canonical title for a given career ID or string.
     */
    fun toCanonicalCareerTitle(input: String?): String {
        val canonicalId = toCanonicalCareerId(input)
        return CAREER_TITLES[canonicalId] ?: DEFAULT_CAREER_TITLE
    }
}
