package com.example.domain.knowledge

import com.example.domain.model.KnowledgeChunk

data class TaggedTopicConcept(
    val topic: String,
    val concept: String
)

/**
 * Interface for extracting topic and concept metadata from text chunks.
 */
interface KnowledgeTagger {
    fun tag(documentName: String, pageNumber: Int, text: String): TaggedTopicConcept
}

/**
 * Deterministic heuristic-based tagger.
 * Identifies obvious structural headings, chapters, sections, or fallback topics.
 */
class DeterministicKnowledgeTagger : KnowledgeTagger {

    override fun tag(documentName: String, pageNumber: Int, text: String): TaggedTopicConcept {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return TaggedTopicConcept(topic = "General", concept = "General")
        }

        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return TaggedTopicConcept(topic = "General", concept = "General")
        }

        // Check for explicit heading patterns in the first few lines
        val firstLine = lines.first()

        // 1. Check for "Chapter X", "Section X", "Unit X", "Module X"
        val chapterMatch = Regex("^(Chapter|Section|Unit|Module|Part)\\s*\\d*[:\\-\\.]?\\s*(.*)", RegexOption.IGNORE_CASE).find(firstLine)
        if (chapterMatch != null) {
            val headingTitle = chapterMatch.groupValues[2].trim().ifBlank { firstLine }
            val concept = extractConcept(lines.drop(1)) ?: headingTitle
            return TaggedTopicConcept(
                topic = cleanTopic(firstLine.take(50)),
                concept = cleanConcept(concept.take(50))
            )
        }

        // 2. Check for markdown headings (# Heading) or short header lines (< 60 chars, no terminal period)
        val firstHeaderLine = lines.firstOrNull()
        if (firstHeaderLine != null && firstHeaderLine.length in 4..60 && !firstHeaderLine.endsWith(".") && !firstHeaderLine.endsWith(",")) {
            val isMarkdown = firstHeaderLine.startsWith("#")
            val cleanHeading = firstHeaderLine.replace(Regex("^#+\\s*"), "").trim()
            val hasLetters = cleanHeading.any { it.isLetter() }
            val isColonHeader = cleanHeading.contains(":") && cleanHeading.substringBefore(":").length in 3..30
            val isTitleCaseOrUpper = hasLetters && (cleanHeading.all { it.isUpperCase() || !it.isLetter() } || cleanHeading.split(" ").all { it.isEmpty() || it.first().isUpperCase() || it.all { c -> !c.isLetter() } })

            if (isMarkdown || isColonHeader || isTitleCaseOrUpper) {
                val topic = if (cleanHeading.contains(":")) cleanHeading.substringBefore(":").trim() else cleanHeading
                val concept = if (cleanHeading.contains(":")) cleanHeading.substringAfter(":").trim() else (extractConcept(lines.drop(1)) ?: topic)
                return TaggedTopicConcept(
                    topic = cleanTopic(topic),
                    concept = cleanConcept(concept)
                )
            }
        }

        // 3. Document-derived or general fallback
        val docFallback = documentName.substringBeforeLast(".").replace("_", " ").replace("-", " ").trim()
        val defaultTopic = if (docFallback.isNotBlank() && docFallback.length <= 40) docFallback else "General"
        val concept = extractConcept(lines) ?: "Core Concepts"

        return TaggedTopicConcept(
            topic = defaultTopic,
            concept = concept
        )
    }

    private fun extractConcept(subLines: List<String>): String? {
        for (line in subLines.take(4)) {
            val clean = line.trim()
            if (clean.length in 4..50 && !clean.endsWith(".")) {
                return clean
            }
            if (clean.contains(" - ") || clean.contains(": ")) {
                val part = clean.split(Regex(" - |: ")).firstOrNull()?.trim()
                if (!part.isNullOrBlank() && part.length in 4..40) {
                    return part
                }
            }
        }
        return null
    }

    private fun cleanTopic(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9\\s\\-_:]"), "")
            .trim()
            .ifBlank { "General" }
    }

    private fun cleanConcept(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9\\s\\-_:]"), "")
            .trim()
            .ifBlank { "General" }
    }
}
