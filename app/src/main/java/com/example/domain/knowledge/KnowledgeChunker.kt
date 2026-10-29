package com.example.domain.knowledge

import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningMaterial
import com.example.domain.model.PdfContentUnit

interface KnowledgeChunker {
    fun chunkMaterial(material: LearningMaterial): List<KnowledgeChunk>
    fun chunkUnits(units: List<PdfContentUnit>): List<KnowledgeChunk>
    fun chunkSingleUnit(unit: PdfContentUnit, tagger: KnowledgeTagger = DeterministicKnowledgeTagger()): List<KnowledgeChunk>
}

/**
 * Deterministic, page-aware chunker.
 * Preserves document isolation and source page references.
 */
class PageAwareKnowledgeChunker(
    private val tagger: KnowledgeTagger = DeterministicKnowledgeTagger(),
    private val maxChunkCharSize: Int = 800,
    private val minChunkCharSize: Int = 50
) : KnowledgeChunker {

    override fun chunkMaterial(material: LearningMaterial): List<KnowledgeChunk> {
        val units = material.contentUnits
        if (units.isEmpty()) return emptyList()
        return chunkUnits(units)
    }

    override fun chunkUnits(units: List<PdfContentUnit>): List<KnowledgeChunk> {
        val allChunks = mutableListOf<KnowledgeChunk>()
        var globalIndex = 0

        // Group strictly by materialId to ensure document isolation
        val unitsByMaterial = units.groupBy { it.materialId }

        for ((_, materialUnits) in unitsByMaterial) {
            for (unit in materialUnits) {
                val pageChunks = chunkSingleUnitInternal(unit, globalIndex)
                allChunks.addAll(pageChunks)
                globalIndex += pageChunks.size
            }
        }

        return allChunks
    }

    override fun chunkSingleUnit(
        unit: PdfContentUnit,
        tagger: KnowledgeTagger
    ): List<KnowledgeChunk> {
        return chunkSingleUnitInternal(unit, 0, tagger)
    }

    private fun chunkSingleUnitInternal(
        unit: PdfContentUnit,
        startIndex: Int,
        customTagger: KnowledgeTagger = tagger
    ): List<KnowledgeChunk> {
        val rawText = unit.text.trim()
        if (rawText.isBlank()) {
            return emptyList() // Do not create chunks from completely empty pages
        }

        // If the entire page/slide/segment is smaller than or equal to maxChunkCharSize, create a single chunk
        if (rawText.length <= maxChunkCharSize) {
            val tags = customTagger.tag(unit.documentName, unit.pageNumber, unit.slideTitle?.let { "$it\n$rawText" } ?: rawText)
            return listOf(
                KnowledgeChunk(
                    id = "${unit.materialId}_p${unit.pageNumber}_c0",
                    materialId = unit.materialId,
                    documentName = unit.documentName,
                    pageNumber = unit.pageNumber,
                    text = rawText,
                    topic = tags.topic,
                    concept = tags.concept,
                    chunkIndex = startIndex,
                    sourceType = unit.sourceType,
                    startTimeMs = unit.startTimeMs,
                    endTimeMs = unit.endTimeMs
                )
            )
        }

        // Split large page/slide/segment text by paragraphs or sentences
        val paragraphs = rawText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        val chunks = mutableListOf<KnowledgeChunk>()
        var currentBuffer = StringBuilder()
        var subIndex = 0

        for (para in paragraphs) {
            if (currentBuffer.length + para.length > maxChunkCharSize && currentBuffer.isNotBlank()) {
                val chunkText = currentBuffer.toString().trim()
                val tags = customTagger.tag(unit.documentName, unit.pageNumber, unit.slideTitle?.let { "$it\n$chunkText" } ?: chunkText)
                chunks.add(
                    KnowledgeChunk(
                        id = "${unit.materialId}_p${unit.pageNumber}_c$subIndex",
                        materialId = unit.materialId,
                        documentName = unit.documentName,
                        pageNumber = unit.pageNumber,
                        text = chunkText,
                        topic = tags.topic,
                        concept = tags.concept,
                        chunkIndex = startIndex + subIndex,
                        sourceType = unit.sourceType,
                        startTimeMs = unit.startTimeMs,
                        endTimeMs = unit.endTimeMs
                    )
                )
                subIndex++
                currentBuffer = StringBuilder()
            }

            // If a single paragraph is longer than maxChunkCharSize, split by sentence boundaries
            if (para.length > maxChunkCharSize) {
                val sentences = para.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
                for (sentence in sentences) {
                    if (currentBuffer.length + sentence.length > maxChunkCharSize && currentBuffer.isNotBlank()) {
                        val chunkText = currentBuffer.toString().trim()
                        val tags = customTagger.tag(unit.documentName, unit.pageNumber, unit.slideTitle?.let { "$it\n$chunkText" } ?: chunkText)
                        chunks.add(
                            KnowledgeChunk(
                                id = "${unit.materialId}_p${unit.pageNumber}_c$subIndex",
                                materialId = unit.materialId,
                                documentName = unit.documentName,
                                pageNumber = unit.pageNumber,
                                text = chunkText,
                                topic = tags.topic,
                                concept = tags.concept,
                                chunkIndex = startIndex + subIndex,
                                sourceType = unit.sourceType,
                                startTimeMs = unit.startTimeMs,
                                endTimeMs = unit.endTimeMs
                            )
                        )
                        subIndex++
                        currentBuffer = StringBuilder()
                    }
                    if (currentBuffer.isNotEmpty()) currentBuffer.append(" ")
                    currentBuffer.append(sentence)
                }
            } else {
                if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                currentBuffer.append(para)
            }
        }

        if (currentBuffer.isNotBlank()) {
            val chunkText = currentBuffer.toString().trim()
            val tags = customTagger.tag(unit.documentName, unit.pageNumber, unit.slideTitle?.let { "$it\n$chunkText" } ?: chunkText)
            chunks.add(
                KnowledgeChunk(
                    id = "${unit.materialId}_p${unit.pageNumber}_c$subIndex",
                    materialId = unit.materialId,
                    documentName = unit.documentName,
                    pageNumber = unit.pageNumber,
                    text = chunkText,
                    topic = tags.topic,
                    concept = tags.concept,
                    chunkIndex = startIndex + subIndex,
                    sourceType = unit.sourceType,
                    startTimeMs = unit.startTimeMs,
                    endTimeMs = unit.endTimeMs
                )
            )
        }

        return chunks
    }
}
