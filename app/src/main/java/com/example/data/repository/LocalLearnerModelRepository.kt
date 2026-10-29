package com.example.data.repository

import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.model.adaptive.ConfidenceLevel
import com.example.domain.model.adaptive.TopicMastery
import com.example.domain.model.adaptive.TutorLearningSignal
import com.example.domain.model.adaptive.TutorSignalType
import com.example.domain.repository.LearnerModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.roundToInt

@Serializable
private data class LearnerModelDiskPayload(
    val masteryStore: Map<String, Map<String, TopicMastery>> = emptyMap(),
    val tutorSignals: Map<String, List<TutorLearningSignal>> = emptyMap()
)

/**
 * Persistent & in-memory implementation of [LearnerModelRepository].
 *
 * ALGORITHM: Weighted Evidence Mastery Model
 * -------------------------------------------
 * - Initial baseline mastery when a topic is first assessed: 50%
 * - Update formula after assessment:
 *     newMastery = (oldMastery * 0.6) + (assessmentAccuracy * 100 * 0.4)
 * - Result is rounded to nearest integer and clamped to [0, 100].
 * - Updates only topics assessed; unassessed topics remain strictly unchanged.
 * - Tutor interaction signals adjust topic confidence level without artificially
 *   deflating baseline mastery.
 */
class LocalLearnerModelRepository(
    private val storageDir: File? = null
) : LearnerModelRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Map of materialId -> Map of topic -> TopicMastery
    private val _masteryStore = MutableStateFlow<Map<String, Map<String, TopicMastery>>>(emptyMap())

    // Map of materialId -> List of TutorLearningSignal
    private val _tutorSignals = mutableMapOf<String, MutableList<TutorLearningSignal>>()

    init {
        loadFromDisk()
    }

    private fun getStorageFile(): File? {
        val dir = storageDir ?: return null
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cga_learner_mastery.json")
    }

    private fun loadFromDisk() {
        val file = getStorageFile() ?: return
        if (!file.exists()) return
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val payload = json.decodeFromString<LearnerModelDiskPayload>(content)
                _masteryStore.value = payload.masteryStore
                _tutorSignals.clear()
                payload.tutorSignals.forEach { (matId, list) ->
                    _tutorSignals[matId] = list.toMutableList()
                }
            }
        } catch (_: Exception) {
            // Corrupted file fallback
        }
    }

    private fun saveToDisk() {
        val file = getStorageFile() ?: return
        try {
            val payload = LearnerModelDiskPayload(
                masteryStore = _masteryStore.value,
                tutorSignals = _tutorSignals.mapValues { it.value.toList() }
            )
            file.writeText(json.encodeToString(payload))
        } catch (_: Exception) {
            // Non-fatal IO error
        }
    }

    override fun getAllMasteryFlow(materialId: String?): Flow<List<TopicMastery>> {
        return _masteryStore.map { store ->
            if (materialId != null) {
                store[materialId]?.values?.toList() ?: emptyList()
            } else {
                store.values.flatMap { it.values }
            }
        }
    }

    override suspend fun getMastery(materialId: String, topic: String): TopicMastery? {
        return _masteryStore.value[materialId]?.get(topic)
    }

    override suspend fun getAllMastery(materialId: String): List<TopicMastery> {
        return _masteryStore.value[materialId]?.values?.toList() ?: emptyList()
    }

    /**
     * Executes the Weighted Evidence Mastery Model update.
     */
    override suspend fun updateFromAssessment(
        materialId: String,
        result: AssessmentResult
    ): List<TopicMastery> {
        val updatedTopics = mutableListOf<TopicMastery>()

        _masteryStore.update { currentStore ->
            val materialMap = (currentStore[materialId] ?: emptyMap()).toMutableMap()

            // Group evaluated question results by topic
            val questionsByTopic = result.questionResults.groupBy { it.topic }

            for ((topic, qResults) in questionsByTopic) {
                if (topic.isBlank()) continue

                val attempted = qResults.size
                val correct = qResults.count { it.correct }
                val accuracyPct = if (attempted > 0) ((correct * 100.0) / attempted).roundToInt() else 0

                val existing = materialMap[topic]
                val oldMastery = existing?.masteryScore ?: 50 // Baseline 50% for first assessment

                // Weighted Evidence Mastery Model calculation:
                // newMastery = (oldMastery * 0.6) + (assessmentAccuracy * 100 * 0.4)
                val rawNewMastery = (oldMastery * 0.6) + (accuracyPct * 0.4)
                val clampedMastery = when {
                    accuracyPct == 100 && rawNewMastery >= 99.0 -> 100
                    accuracyPct == 0 && rawNewMastery <= 1.0 -> 0
                    else -> rawNewMastery.roundToInt().coerceIn(0, 100)
                }

                val totalAttempted = (existing?.questionsAttempted ?: 0) + attempted
                val totalCorrect = (existing?.questionsCorrect ?: 0) + correct

                // Check tutor signals to factor into confidence
                val signalsForTopic = _tutorSignals[materialId]?.filter { it.topic.equals(topic, ignoreCase = true) } ?: emptyList()
                val clarificationsCount = signalsForTopic.count { it.signalType == TutorSignalType.ASKED_FOR_CLARIFICATION }
                val confidence = ConfidenceLevel.fromAccuracyAndSignals(accuracyPct, clarificationsCount)

                val newTopicMastery = TopicMastery(
                    materialId = materialId,
                    topic = topic,
                    masteryScore = clampedMastery,
                    questionsAttempted = totalAttempted,
                    questionsCorrect = totalCorrect,
                    lastAttemptAt = System.currentTimeMillis(),
                    recentAccuracy = accuracyPct,
                    confidenceLevel = confidence
                )

                materialMap[topic] = newTopicMastery
                updatedTopics.add(newTopicMastery)
            }

            currentStore + (materialId to materialMap)
        }

        saveToDisk()
        return updatedTopics
    }

    override suspend fun getWeakTopics(materialId: String): List<TopicMastery> {
        val all = getAllMastery(materialId)
        return all.filter { it.isWeak }.sortedBy { it.masteryScore }
    }

    override suspend fun getStrongTopics(materialId: String): List<TopicMastery> {
        val all = getAllMastery(materialId)
        return all.filter { it.isStrong }.sortedByDescending { it.masteryScore }
    }

    /**
     * Deterministic recommendation logic:
     * 1. If any topic < 60%: recommend lowest-mastery topic among them.
     * 2. Else if any topic < 80%: recommend lowest-mastery topic among them.
     * 3. Else all topics >= 80%: return null (all strong).
     */
    override suspend fun getRecommendedTopic(materialId: String): TopicMastery? {
        val all = getAllMastery(materialId)
        if (all.isEmpty()) return null

        val below60 = all.filter { it.masteryScore < 60 }
        if (below60.isNotEmpty()) {
            return below60.minByOrNull { it.masteryScore }
        }

        val below80 = all.filter { it.masteryScore < 80 }
        if (below80.isNotEmpty()) {
            return below80.minByOrNull { it.masteryScore }
        }

        return null
    }

    override suspend fun recordTutorSignal(materialId: String, signal: TutorLearningSignal) {
        val list = _tutorSignals.getOrPut(materialId) { mutableListOf() }
        list.add(signal)

        // If student repeatedly asks for clarification about an existing topic, adjust confidence
        val topic = signal.topic
        val existingMastery = _masteryStore.value[materialId]?.get(topic)
        if (existingMastery != null) {
            val clarifications = list.filter {
                it.topic.equals(topic, ignoreCase = true) &&
                        it.signalType == TutorSignalType.ASKED_FOR_CLARIFICATION
            }.size

            if (clarifications >= 2 && existingMastery.confidenceLevel != ConfidenceLevel.LOW) {
                _masteryStore.update { currentStore ->
                    val materialMap = (currentStore[materialId] ?: emptyMap()).toMutableMap()
                    materialMap[topic] = existingMastery.copy(confidenceLevel = ConfidenceLevel.LOW)
                    currentStore + (materialId to materialMap)
                }
            }
        }
        saveToDisk()
    }

    override suspend fun deleteMaterialMastery(materialId: String): Result<Unit> {
        _masteryStore.update { current -> current - materialId }
        _tutorSignals.remove(materialId)
        saveToDisk()
        return Result.success(Unit)
    }

    override suspend fun clearMastery() {
        _masteryStore.value = emptyMap()
        _tutorSignals.clear()
        saveToDisk()
    }
}
