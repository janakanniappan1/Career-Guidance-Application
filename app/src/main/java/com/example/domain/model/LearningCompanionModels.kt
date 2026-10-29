package com.example.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain models for the AI Learning Companion module.
 * Hackathon Track D: Personalized Tutoring & Adaptive Learning.
 */
@Serializable
enum class LearningMaterialType(
    val id: String,
    val displayName: String,
    val emoji: String,
    val description: String,
    val supportedFormats: String
) {
    TEXTBOOK_PDF(
        id = "textbook_pdf",
        displayName = "Textbook / PDF",
        emoji = "📚",
        description = "Textbooks, research papers, syllabus PDFs & study notes",
        supportedFormats = "PDF, EPUB, DOCX"
    ),
    SLIDES(
        id = "slides",
        displayName = "Slides",
        emoji = "📊",
        description = "Lecture slide decks, presentations & course handouts",
        supportedFormats = "PPTX, PDF, Keynote"
    ),
    LECTURE_VIDEO(
        id = "lecture_video",
        displayName = "Lecture Video",
        emoji = "🎥",
        description = "Recorded lectures, class recordings & tutorial videos",
        supportedFormats = "MP4, MKV, YouTube URL"
    );

    companion object {
        fun fromId(id: String?): LearningMaterialType =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TEXTBOOK_PDF
    }
}

@Serializable
enum class LearningMaterialStatus {
    PROCESSING,
    PROCESSED,
    FAILED
}

@Serializable
enum class MaterialSourceType {
    PDF,
    PPTX,
    VIDEO
}

/**
 * Core domain representation of an ingested learning material.
 */
@Serializable
data class LearningMaterial(
    val id: String,
    val name: String,
    val type: LearningMaterialType = LearningMaterialType.TEXTBOOK_PDF,
    val pageCount: Int = 0,
    val status: LearningMaterialStatus = LearningMaterialStatus.PROCESSING,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceFileName: String = "",
    val errorMessage: String? = null,
    val contentUnits: List<PdfContentUnit> = emptyList(),
    val sourceType: MaterialSourceType = when (type) {
        LearningMaterialType.SLIDES -> MaterialSourceType.PPTX
        LearningMaterialType.LECTURE_VIDEO -> MaterialSourceType.VIDEO
        else -> MaterialSourceType.PDF
    }
)

/**
 * Represents a single extracted page-level, slide-level, or timestamped video transcript unit.
 */
@Serializable
data class PdfContentUnit(
    val id: String,
    val materialId: String,
    val documentName: String,
    val pageNumber: Int,
    val text: String,
    val sourceType: MaterialSourceType = MaterialSourceType.PDF,
    val slideTitle: String? = null,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
) {
    val unitLabel: String
        get() = when (sourceType) {
            MaterialSourceType.PPTX -> "Slide $pageNumber"
            MaterialSourceType.VIDEO -> formatTimestampRange(startTimeMs, endTimeMs)
            else -> "Page $pageNumber"
        }

    val previewText: String
        get() = if (text.length > 250) text.take(250) + "..." else text.ifBlank {
            when (sourceType) {
                MaterialSourceType.PPTX -> "[No extractable text on this slide]"
                MaterialSourceType.VIDEO -> "[No speech transcript segment]"
                else -> "[No extractable text on this page]"
            }
        }
}

/**
 * Atomic knowledge chunk containing source citation (page/slide/timestamp) and concept metadata.
 */
@Serializable
data class KnowledgeChunk(
    val id: String,
    val materialId: String,
    val documentName: String,
    val pageNumber: Int,
    val text: String,
    val topic: String = "General",
    val concept: String = "General",
    val chunkIndex: Int = 0,
    val sourceType: MaterialSourceType = MaterialSourceType.PDF,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
) {
    val unitLabel: String
        get() = when (sourceType) {
            MaterialSourceType.PPTX -> "Slide $pageNumber"
            MaterialSourceType.VIDEO -> formatTimestampRange(startTimeMs, endTimeMs)
            else -> "Page $pageNumber"
        }

    val sourceCitation: String
        get() = "$documentName • $unitLabel"

    val previewText: String
        get() = if (text.length > 200) text.take(200) + "..." else text
}

private fun formatTimestampRange(startMs: Long, endMs: Long): String {
    val startMin = startMs / 60000
    val startSec = (startMs % 60000) / 1000
    val endMin = endMs / 60000
    val endSec = (endMs % 60000) / 1000
    return String.format("%02d:%02d–%02d:%02d", startMin, startSec, endMin, endSec)
}

/**
 * Common source location representation supporting PDF pages, PPTX slides, and video timestamp intervals.
 */
sealed class SourceLocation {
    abstract val label: String

    data class PdfPage(val pageNumber: Int) : SourceLocation() {
        override val label: String = "Page $pageNumber"
    }

    data class PptxSlide(val slideNumber: Int) : SourceLocation() {
        override val label: String = "Slide $slideNumber"
    }

    data class VideoTimestamp(val startTimeMs: Long, val endTimeMs: Long) : SourceLocation() {
        override val label: String = formatTimestampRange(startTimeMs, endTimeMs)
    }

    companion object {
        fun from(sourceType: MaterialSourceType, pageNumber: Int, startMs: Long = 0L, endMs: Long = 0L): SourceLocation {
            return when (sourceType) {
                MaterialSourceType.PPTX -> PptxSlide(pageNumber)
                MaterialSourceType.VIDEO -> VideoTimestamp(startMs, endMs)
                else -> PdfPage(pageNumber)
            }
        }
    }
}

/**
 * Unified retrieval result containing source chunk, relevance score, and source location.
 */
data class RetrievalResult(
    val chunk: KnowledgeChunk,
    val materialId: String = chunk.materialId,
    val documentName: String = chunk.documentName,
    val sourceType: MaterialSourceType = chunk.sourceType,
    val sourceLocation: SourceLocation = SourceLocation.from(chunk.sourceType, chunk.pageNumber, chunk.startTimeMs, chunk.endTimeMs),
    val topic: String = chunk.topic,
    val concept: String = chunk.concept,
    val relevanceScore: Int = 0,
    val matchedExcerpt: String = chunk.previewText
) {
    val sourceCitation: String
        get() = "$documentName • ${sourceLocation.label}"
}

/**
 * Summary statistics for a material's Knowledge Base.
 */
data class KnowledgeBaseStats(
    val materialId: String,
    val materialName: String,
    val totalChunks: Int,
    val totalTopics: Int,
    val totalConcepts: Int,
    val isReady: Boolean = true
)

/**
 * Metadata representation of an uploaded learning material (backward compatible alias).
 */
typealias LearningMaterialItem = LearningMaterial

/**
 * Pillar features available in the AI Learning Companion.
 */
enum class LearningCompanionFeatureType(
    val title: String,
    val emoji: String,
    val description: String,
    val badge: String
) {
    KNOWLEDGE_BASE(
        title = "Knowledge Base",
        emoji = "🧠",
        description = "Structured concepts, summaries, and key takeaways extracted from your materials",
        badge = "GROUNDED"
    ),
    SOURCE_GROUNDED_TUTOR(
        title = "Source-Grounded AI Tutor",
        emoji = "💬",
        description = "Context-aware conversational tutoring with direct citations to your uploaded pages",
        badge = "PERSONALIZED"
    ),
    ADAPTIVE_ASSESSMENT(
        title = "Adaptive Assessment",
        emoji = "📝",
        description = "Dynamic quizzes and self-tests that adapt in real time to your knowledge gaps",
        badge = "ADAPTIVE"
    ),
    MY_MASTERY(
        title = "My Mastery",
        emoji = "📈",
        description = "Track topic-level competency, confidence scores, and retention decay over time",
        badge = "0% INITIAL"
    )
}

enum class TutorMessageRole {
    USER,
    ASSISTANT
}

data class TutorSource(
    val materialId: String,
    val documentName: String,
    val pageNumber: Int,
    val chunkId: String,
    val previewText: String = "",
    val sourceType: MaterialSourceType = MaterialSourceType.PDF,
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L
) {
    val unitLabel: String
        get() = when (sourceType) {
            MaterialSourceType.PPTX -> "Slide $pageNumber"
            MaterialSourceType.VIDEO -> {
                val startMin = startTimeMs / 60000
                val startSec = (startTimeMs % 60000) / 1000
                val endMin = endTimeMs / 60000
                val endSec = (endTimeMs % 60000) / 1000
                String.format("%02d:%02d–%02d:%02d", startMin, startSec, endMin, endSec)
            }
            else -> "Page $pageNumber"
        }

    val displayCitation: String
        get() = "$documentName • $unitLabel"
}

data class TutorMessage(
    val id: String,
    val role: TutorMessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sources: List<TutorSource> = emptyList(),
    val isGrounded: Boolean = false,
    val unsupportedReason: String? = null
)

data class TutorResponse(
    val answer: String,
    val sources: List<TutorSource> = emptyList(),
    val grounded: Boolean = false,
    val unsupportedReason: String? = null
)
