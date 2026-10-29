package com.example.data.repository

import com.example.domain.knowledge.KnowledgeChunker
import com.example.domain.knowledge.PageAwareKnowledgeChunker
import com.example.domain.model.KnowledgeBaseStats
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningMaterial
import com.example.domain.repository.KnowledgeBaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LocalKnowledgeBaseRepository(
    private val chunker: KnowledgeChunker = PageAwareKnowledgeChunker(),
    private val storageDir: File? = null
) : KnowledgeBaseRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Map of materialId -> List<KnowledgeChunk>
    private val _knowledgeStore = MutableStateFlow<Map<String, List<KnowledgeChunk>>>(emptyMap())

    init {
        loadFromDisk()
    }

    private fun getStorageFile(): File? {
        val dir = storageDir ?: return null
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cga_knowledge_base.json")
    }

    private fun loadFromDisk() {
        val file = getStorageFile() ?: return
        if (!file.exists()) return
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                _knowledgeStore.value = json.decodeFromString<Map<String, List<KnowledgeChunk>>>(content)
            }
        } catch (_: Exception) {
            // Corrupted file fallback
        }
    }

    private fun saveToDisk() {
        val file = getStorageFile() ?: return
        try {
            file.writeText(json.encodeToString(_knowledgeStore.value))
        } catch (_: Exception) {
            // Non-fatal IO error
        }
    }

    override fun getChunksFlow(materialId: String?): Flow<List<KnowledgeChunk>> {
        return _knowledgeStore.map { store ->
            if (materialId != null) {
                store[materialId] ?: emptyList()
            } else {
                store.values.flatten()
            }
        }
    }

    override suspend fun ingestMaterial(material: LearningMaterial): Result<List<KnowledgeChunk>> {
        try {
            val chunks = chunker.chunkMaterial(material)
            _knowledgeStore.update { currentMap ->
                currentMap + (material.id to chunks)
            }
            saveToDisk()
            return Result.success(chunks)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getChunks(materialId: String): List<KnowledgeChunk> {
        return _knowledgeStore.value[materialId] ?: emptyList()
    }

    override suspend fun getAllChunks(): List<KnowledgeChunk> {
        return _knowledgeStore.value.values.flatten()
    }

    override suspend fun getChunksByTopic(materialId: String, topic: String): List<KnowledgeChunk> {
        val chunks = _knowledgeStore.value[materialId] ?: emptyList()
        return chunks.filter { it.topic.equals(topic, ignoreCase = true) }
    }

    override suspend fun searchChunks(materialId: String?, query: String): List<KnowledgeChunk> {
        return retrieveRelevantResults(materialId, query).map { it.chunk }
    }

    override suspend fun retrieveRelevantResults(
        materialId: String?,
        query: String,
        limit: Int
    ): List<com.example.domain.model.RetrievalResult> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isBlank()) return emptyList()

        val candidateChunks = if (materialId != null) {
            _knowledgeStore.value[materialId] ?: emptyList()
        } else {
            _knowledgeStore.value.values.flatten()
        }

        if (candidateChunks.isEmpty()) return emptyList()

        val rawTokens = cleanQuery.split(Regex("[^a-z0-9]+"))
            .map { it.trim() }
            .filter { it.length > 1 && it !in STOP_WORDS }

        val expandedTokens = expandQueryTokens(rawTokens)
        val corePhrase = rawTokens.joinToString(" ")

        // Deduplicate chunks by id
        val distinctChunks = candidateChunks.distinctBy { it.id }

        val scoredResults = distinctChunks.mapNotNull { chunk ->
            val score = computeRelevanceScore(chunk, cleanQuery, corePhrase, expandedTokens)
            if (score >= 10) {
                com.example.domain.model.RetrievalResult(
                    chunk = chunk,
                    materialId = chunk.materialId,
                    documentName = chunk.documentName,
                    sourceType = chunk.sourceType,
                    sourceLocation = com.example.domain.model.SourceLocation.from(
                        chunk.sourceType,
                        chunk.pageNumber,
                        chunk.startTimeMs,
                        chunk.endTimeMs
                    ),
                    topic = chunk.topic,
                    concept = chunk.concept,
                    relevanceScore = score,
                    matchedExcerpt = chunk.previewText
                )
            } else null
        }

        // Deterministic ranking: highest relevanceScore first, then chunk id
        return scoredResults
            .sortedWith(compareByDescending<com.example.domain.model.RetrievalResult> { it.relevanceScore }.thenBy { it.chunk.id })
            .take(limit)
    }

    override suspend fun getTopics(materialId: String): List<String> {
        val chunks = _knowledgeStore.value[materialId] ?: emptyList()
        return chunks.map { it.topic }.distinct().filter { it.isNotBlank() }
    }

    override suspend fun getConcepts(materialId: String): List<String> {
        val chunks = _knowledgeStore.value[materialId] ?: emptyList()
        return chunks.map { it.concept }.distinct().filter { it.isNotBlank() }
    }

    override suspend fun getStats(materialId: String): KnowledgeBaseStats? {
        val chunks = _knowledgeStore.value[materialId] ?: return null
        val docName = chunks.firstOrNull()?.documentName ?: "Material"
        val topics = chunks.map { it.topic }.distinct().size
        val concepts = chunks.map { it.concept }.distinct().size

        return KnowledgeBaseStats(
            materialId = materialId,
            materialName = docName,
            totalChunks = chunks.size,
            totalTopics = topics,
            totalConcepts = concepts,
            isReady = chunks.isNotEmpty()
        )
    }

    override suspend fun getAllStats(): List<KnowledgeBaseStats> {
        return _knowledgeStore.value.map { (matId, chunks) ->
            val docName = chunks.firstOrNull()?.documentName ?: "Material"
            KnowledgeBaseStats(
                materialId = matId,
                materialName = docName,
                totalChunks = chunks.size,
                totalTopics = chunks.map { it.topic }.distinct().size,
                totalConcepts = chunks.map { it.concept }.distinct().size,
                isReady = chunks.isNotEmpty()
            )
        }
    }

    override suspend fun deleteMaterialKnowledge(materialId: String): Result<Unit> {
        _knowledgeStore.update { current -> current - materialId }
        saveToDisk()
        return Result.success(Unit)
    }

    private fun expandQueryTokens(tokens: List<String>): List<String> {
        val result = linkedSetOf<String>()
        for (token in tokens) {
            result.add(token)
            ACRONYM_EXPANSIONS[token]?.let { expandedWords ->
                result.addAll(expandedWords)
            }
        }
        // Reverse check: if query contains "machine" and "learning", also include "ml"
        val tokenSet = tokens.toSet()
        for ((acronym, phraseWords) in ACRONYM_EXPANSIONS) {
            if (phraseWords.size >= 2 && phraseWords.all { it in tokenSet }) {
                result.add(acronym)
            }
        }
        return result.toList()
    }

    private fun normalizeDottedAcronyms(input: String): String {
        return input
            .replace("m.l.", "ml")
            .replace("a.i.", "ai")
            .replace("d.l.", "dl")
            .replace("n.l.p.", "nlp")
    }

    private fun computeRelevanceScore(
        chunk: KnowledgeChunk,
        fullQuery: String,
        corePhrase: String,
        tokens: List<String>
    ): Int {
        var score = 0
        val lowerText = normalizeDottedAcronyms(chunk.text.lowercase())
        val lowerTopic = normalizeDottedAcronyms(chunk.topic.lowercase())
        val lowerConcept = normalizeDottedAcronyms(chunk.concept.lowercase())
        val lowerDoc = normalizeDottedAcronyms(chunk.documentName.lowercase())

        // 1. Exact phrase match on full query or stripped core phrase (when 2+ words or >= 4 chars)
        val hasFullPhraseMatch = lowerText.contains(fullQuery) ||
            (corePhrase.length >= 4 && corePhrase.contains(" ") && lowerText.contains(corePhrase))
        val hasTopicPhraseMatch = lowerTopic.contains(fullQuery) || lowerConcept.contains(fullQuery) ||
            (corePhrase.length >= 4 && corePhrase.contains(" ") && (lowerTopic.contains(corePhrase) || lowerConcept.contains(corePhrase)))

        if (hasFullPhraseMatch) {
            score += 50
        }
        if (hasTopicPhraseMatch) {
            score += 40
        }

        // Check expanded phrase match for single-token acronyms (e.g., query "ml" matching "machine learning")
        var hasExpandedPhraseMatch = false
        for (token in tokens) {
            val expandedPhrase = ACRONYM_PHRASES[token]
            if (expandedPhrase != null) {
                if (lowerText.contains(expandedPhrase)) {
                    score += 45
                    hasExpandedPhraseMatch = true
                }
                if (lowerTopic.contains(expandedPhrase) || lowerConcept.contains(expandedPhrase)) {
                    score += 35
                    hasExpandedPhraseMatch = true
                }
            }
        }

        // 2. Token matches (whole-word, plural/inflection for 4+ chars, or 6+ char shared root)
        var contentMatched = hasFullPhraseMatch || hasTopicPhraseMatch || hasExpandedPhraseMatch
        val textWords = lowerText.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        val topicWords = lowerTopic.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        val conceptWords = lowerConcept.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        val docWords = lowerDoc.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

        fun wordsMatch(word: String, queryToken: String): Boolean {
            if (word == queryToken) return true
            if (word.length >= 4 && queryToken.length >= 4) {
                if (word == "${queryToken}s" || queryToken == "${word}s" ||
                    word == "${queryToken}es" || queryToken == "${word}es" ||
                    word == "${queryToken}ed" || queryToken == "${word}ed"
                ) {
                    return true
                }
            }
            if (word.length >= 6 && queryToken.length >= 6) {
                return word.startsWith(queryToken) || queryToken.startsWith(word)
            }
            return false
        }

        for (token in tokens) {
            val cleanToken = token.replace(Regex("^[^a-z0-9]+|[^a-z0-9]+$"), "")
            if (cleanToken.length < 2) continue

            val topicMatch = topicWords.any { wordsMatch(it, cleanToken) }
            val conceptMatch = conceptWords.any { wordsMatch(it, cleanToken) }
            val docMatch = docWords.any { wordsMatch(it, cleanToken) }

            if (topicMatch) { score += 20; contentMatched = true }
            if (conceptMatch) { score += 15; contentMatched = true }
            if (docMatch) score += 10

            // Term frequency in text
            val occurrences = textWords.count { wordsMatch(it, cleanToken) }
            if (occurrences > 0) {
                score += minOf(occurrences * 5, 25)
                contentMatched = true
            }
        }

        // If neither text, topic, concept, nor phrase matched, ignore pure document name matches
        if (!contentMatched) {
            return 0
        }

        return score
    }

    companion object {
        private val STOP_WORDS = setOf(
            "the", "is", "at", "which", "on", "a", "an", "and", "or", "in", "of", "to", "for", "with", "as", "by",
            "tell", "me", "about", "what", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "can", "could", "will", "would", "should", "may", "might", "explain", "describe",
            "define", "discuss", "show", "give", "please", "how", "why", "when", "where", "who", "whom",
            "this", "that", "these", "those", "my", "your", "our", "their", "its", "from", "into", "onto"
        )

        private val ACRONYM_EXPANSIONS = mapOf(
            "ml" to listOf("machine", "learning", "aiml"),
            "ai" to listOf("artificial", "intelligence", "aiml"),
            "aiml" to listOf("artificial", "intelligence", "machine", "learning", "ml", "ai"),
            "dl" to listOf("deep", "learning"),
            "nlp" to listOf("natural", "language", "processing"),
            "cnn" to listOf("convolutional", "neural", "network"),
            "rnn" to listOf("recurrent", "neural", "network"),
            "svm" to listOf("support", "vector", "machine"),
            "knn" to listOf("nearest", "neighbor"),
            "pca" to listOf("principal", "component", "analysis"),
            "rl" to listOf("reinforcement", "learning"),
            "ann" to listOf("artificial", "neural", "network")
        )

        private val ACRONYM_PHRASES = mapOf(
            "ml" to "machine learning",
            "ai" to "artificial intelligence",
            "dl" to "deep learning",
            "nlp" to "natural language processing",
            "cnn" to "convolutional neural network",
            "rnn" to "recurrent neural network",
            "svm" to "support vector machine",
            "knn" to "nearest neighbor",
            "pca" to "principal component analysis",
            "rl" to "reinforcement learning",
            "ann" to "artificial neural network"
        )
    }
}
