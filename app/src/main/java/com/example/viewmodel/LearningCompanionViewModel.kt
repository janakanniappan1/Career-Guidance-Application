package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningCompanionFeatureType
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.TutorMessage
import com.example.domain.model.TutorMessageRole
import com.example.domain.model.TutorSource
import com.example.domain.model.adaptive.Assessment
import com.example.domain.model.adaptive.AssessmentDifficulty
import com.example.domain.model.adaptive.AssessmentQuestion
import com.example.domain.model.adaptive.AssessmentQuestionType
import com.example.domain.model.adaptive.AssessmentResult
import com.example.domain.model.adaptive.QuestionResult
import com.example.domain.model.adaptive.TopicMastery
import com.example.domain.model.adaptive.TutorLearningSignal
import com.example.domain.model.adaptive.TutorSignalType
import com.example.domain.repository.AdaptiveAssessmentRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.KnowledgeBaseRepository
import com.example.domain.repository.LearnerModelRepository
import com.example.domain.repository.LearningMaterialRepository
import com.example.domain.repository.LearningTutorRepository
import com.example.data.repository.LocalLearnerModelRepository
import com.example.data.repository.SupabaseAdaptiveAssessmentRepository
import com.example.util.CareerCanonicalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.UUID
import kotlin.math.roundToInt

/**
 * ViewModel for the AI Learning Companion module, Knowledge Base, and Source-Grounded AI Tutor.
 * Manages user context, PDF ingestion, chunking, topic inspection, grounded AI tutor, and learner mastery model.
 */
class LearningCompanionViewModel(
    private val authRepository: AuthRepository? = null,
    private val materialRepository: LearningMaterialRepository? = null,
    private val knowledgeBaseRepository: KnowledgeBaseRepository? = null,
    private val tutorRepository: LearningTutorRepository? = null,
    private val adaptiveAssessmentRepository: AdaptiveAssessmentRepository? = null,
    private val learnerModelRepository: LearnerModelRepository? = LocalLearnerModelRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningCompanionUiState())
    val uiState: StateFlow<LearningCompanionUiState> = _uiState.asStateFlow()

    init {
        loadUserContext()
        observeMaterials()
        observeLearnerModel()
    }

    private fun observeLearnerModel() {
        viewModelScope.launch {
            learnerModelRepository?.getAllMasteryFlow()?.collect { masteries ->
                val hasHistory = masteries.isNotEmpty()
                val overall = if (hasHistory) {
                    (masteries.map { it.masteryScore }.average()).roundToInt()
                } else {
                    0
                }
                val firstMatId = _uiState.value.materials.firstOrNull()?.id.orEmpty()
                val rec = if (firstMatId.isNotBlank()) {
                    learnerModelRepository.getRecommendedTopic(firstMatId)
                } else {
                    masteries.filter { it.isWeak }.minByOrNull { it.masteryScore }
                        ?: masteries.minByOrNull { it.masteryScore }
                }
                _uiState.update { state ->
                    state.copy(
                        topicMasteries = masteries,
                        hasMasteryHistory = hasHistory,
                        overallMastery = overall,
                        recommendedTopicMastery = rec
                    )
                }
            }
        }
    }

    fun loadUserContext() {
        viewModelScope.launch {
            try {
                val profile = authRepository?.getCurrentUser()?.firstOrNull()
                if (profile != null) {
                    val careerKey = profile.recommendedCareerId?.takeIf { it.isNotBlank() } ?: profile.careerInterest
                    val careerTitle = CareerCanonicalUtil.getCareerTitle(careerKey)
                    _uiState.update {
                        it.copy(
                            studentName = profile.name.ifBlank { "Student" },
                            careerGoal = careerTitle
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Non-fatal, use defaults
            }
        }
    }

    private fun observeMaterials() {
        viewModelScope.launch {
            materialRepository?.getMaterialsFlow()?.collect { materialList ->
                _uiState.update { state ->
                    state.copy(
                        materials = materialList,
                        totalMaterialsCount = materialList.size
                    )
                }
                // Refresh knowledge base stats for all materials
                refreshAllKbStats(materialList)
            }
        }
    }

    private suspend fun refreshAllKbStats(materials: List<LearningMaterial>) {
        val kbRepo = knowledgeBaseRepository ?: return
        val statsMap = mutableMapOf<String, com.example.domain.model.KnowledgeBaseStats>()
        for (material in materials) {
            var stats = kbRepo.getStats(material.id)
            if (stats == null && material.contentUnits.isNotEmpty()) {
                kbRepo.ingestMaterial(material)
                stats = kbRepo.getStats(material.id)
            }
            if (stats != null) {
                statsMap[material.id] = stats
            }
        }
        _uiState.update { it.copy(knowledgeBaseStatsMap = statsMap) }
    }

    /**
     * Ingest a selected PDF Uri from Android Document Picker.
     */
    fun ingestPdf(uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingMessage = "Processing your textbook...",
                    isAddMaterialDialogVisible = false,
                    infoMessage = null,
                    errorMessage = null
                )
            }

            try {
                val repository = materialRepository
                if (repository == null) {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this PDF. Please try another file."
                        )
                    }
                    return@launch
                }

                val result = repository.ingestPdfFromUri(uri, fileName)
                if (result.isSuccess) {
                    val material = result.getOrThrow()
                    // Auto-build knowledge base chunks for seamless user experience
                    val kbResult = knowledgeBaseRepository?.ingestMaterial(material)
                    val chunkCount = kbResult?.getOrNull()?.size ?: 0

                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            infoMessage = "Textbook processed successfully (${material.pageCount} pages, $chunkCount knowledge chunks ready)"
                        )
                    }
                    knowledgeBaseRepository?.getStats(material.id)?.let { stats ->
                        _uiState.update { state ->
                            state.copy(knowledgeBaseStatsMap = state.knowledgeBaseStatsMap + (material.id to stats))
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this PDF. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isProcessingPdf = false,
                        errorMessage = "Could not process this PDF. Please try another file."
                    )
                }
            }
        }
    }

    /**
     * Ingest a PDF from InputStream (for tests or local assets).
     */
    fun ingestPdfStream(stream: InputStream, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingMessage = "Processing your textbook...",
                    isAddMaterialDialogVisible = false,
                    infoMessage = null,
                    errorMessage = null
                )
            }

            try {
                val repository = materialRepository
                if (repository == null) {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this PDF. Please try another file."
                        )
                    }
                    return@launch
                }

                val result = repository.ingestPdfFromStream(stream, fileName)
                if (result.isSuccess) {
                    val material = result.getOrThrow()
                    val kbResult = knowledgeBaseRepository?.ingestMaterial(material)
                    val chunkCount = kbResult?.getOrNull()?.size ?: 0

                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            infoMessage = "Textbook processed successfully (${material.pageCount} pages, $chunkCount knowledge chunks ready)"
                        )
                    }
                    knowledgeBaseRepository?.getStats(material.id)?.let { stats ->
                        _uiState.update { state ->
                            state.copy(knowledgeBaseStatsMap = state.knowledgeBaseStatsMap + (material.id to stats))
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this PDF. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isProcessingPdf = false,
                        errorMessage = "Could not process this PDF. Please try another file."
                    )
                }
            }
        }
    }

    /**
     * Ingest a selected PowerPoint presentation Uri from Android Document Picker.
     */
    fun ingestPptx(uri: Uri, fileName: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingMessage = "Processing your PowerPoint slides...",
                    isAddMaterialDialogVisible = false,
                    infoMessage = null,
                    errorMessage = null
                )
            }

            try {
                val repository = materialRepository
                if (repository == null) {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this PowerPoint file. Please try another file."
                        )
                    }
                    return@launch
                }

                val result = repository.ingestPptxFromUri(uri, fileName)
                if (result.isSuccess) {
                    val material = result.getOrThrow()
                    // Auto-build knowledge base chunks for seamless user experience
                    val kbResult = knowledgeBaseRepository?.ingestMaterial(material)
                    val chunkCount = kbResult?.getOrNull()?.size ?: 0

                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            infoMessage = "PowerPoint slides processed successfully (${material.pageCount} slides, $chunkCount knowledge chunks ready)"
                        )
                    }
                    knowledgeBaseRepository?.getStats(material.id)?.let { stats ->
                        _uiState.update { state ->
                            state.copy(knowledgeBaseStatsMap = state.knowledgeBaseStatsMap + (material.id to stats))
                        }
                    }
                } else {
                    val failure = result.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = failure?.message ?: "Could not process this PowerPoint presentation. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isProcessingPdf = false,
                        errorMessage = "Could not process this PowerPoint presentation: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Ingest a PowerPoint presentation from InputStream (for tests or local streams).
     */
    fun ingestPptxStream(stream: InputStream, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingMessage = "Processing your PowerPoint slides...",
                    isAddMaterialDialogVisible = false,
                    infoMessage = null,
                    errorMessage = null
                )
            }

            try {
                val repository = materialRepository
                if (repository == null) {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this PowerPoint file. Please try another file."
                        )
                    }
                    return@launch
                }

                val result = repository.ingestPptxFromStream(stream, fileName)
                if (result.isSuccess) {
                    val material = result.getOrThrow()
                    val kbResult = knowledgeBaseRepository?.ingestMaterial(material)
                    val chunkCount = kbResult?.getOrNull()?.size ?: 0

                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            infoMessage = "PowerPoint slides processed successfully (${material.pageCount} slides, $chunkCount knowledge chunks ready)"
                        )
                    }
                    knowledgeBaseRepository?.getStats(material.id)?.let { stats ->
                        _uiState.update { state ->
                            state.copy(knowledgeBaseStatsMap = state.knowledgeBaseStatsMap + (material.id to stats))
                        }
                    }
                } else {
                    val failure = result.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = failure?.message ?: "Could not process this PowerPoint presentation. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isProcessingPdf = false,
                        errorMessage = "Could not process this PowerPoint presentation: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Ingest a selected lecture video Uri from Android Document Picker.
     */
    fun ingestVideo(uri: Uri, rawFileName: String? = null, importedTranscriptText: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingMessage = "Processing lecture video transcript...",
                    isAddMaterialDialogVisible = false,
                    infoMessage = null,
                    errorMessage = null
                )
            }

            try {
                val repository = materialRepository
                if (repository == null) {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this video file. Please try another file."
                        )
                    }
                    return@launch
                }

                val result = repository.ingestVideoFromUri(uri, rawFileName, importedTranscriptText)
                if (result.isSuccess) {
                    val material = result.getOrThrow()
                    // Auto-build knowledge base chunks for seamless user experience
                    val kbResult = knowledgeBaseRepository?.ingestMaterial(material)
                    val chunkCount = kbResult?.getOrNull()?.size ?: 0

                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            infoMessage = "Lecture video processed successfully (${material.pageCount} transcript segments, $chunkCount knowledge chunks ready)"
                        )
                    }
                    knowledgeBaseRepository?.getStats(material.id)?.let { stats ->
                        _uiState.update { state ->
                            state.copy(knowledgeBaseStatsMap = state.knowledgeBaseStatsMap + (material.id to stats))
                        }
                    }
                } else {
                    val failure = result.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = failure?.message ?: "Could not process this lecture video. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isProcessingPdf = false,
                        errorMessage = "Could not process this lecture video: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Ingest a lecture video from InputStream (for tests or local streams).
     */
    fun ingestVideoStream(stream: InputStream, fileName: String, importedTranscriptText: String? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isProcessingPdf = true,
                    processingMessage = "Processing lecture video transcript...",
                    isAddMaterialDialogVisible = false,
                    infoMessage = null,
                    errorMessage = null
                )
            }

            try {
                val repository = materialRepository
                if (repository == null) {
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = "Could not process this video file. Please try another file."
                        )
                    }
                    return@launch
                }

                val result = repository.ingestVideoFromStream(stream, fileName, importedTranscriptText)
                if (result.isSuccess) {
                    val material = result.getOrThrow()
                    val kbResult = knowledgeBaseRepository?.ingestMaterial(material)
                    val chunkCount = kbResult?.getOrNull()?.size ?: 0

                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            infoMessage = "Lecture video processed successfully (${material.pageCount} transcript segments, $chunkCount knowledge chunks ready)"
                        )
                    }
                    knowledgeBaseRepository?.getStats(material.id)?.let { stats ->
                        _uiState.update { state ->
                            state.copy(knowledgeBaseStatsMap = state.knowledgeBaseStatsMap + (material.id to stats))
                        }
                    }
                } else {
                    val failure = result.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isProcessingPdf = false,
                            errorMessage = failure?.message ?: "Could not process this lecture video. Please try another file."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isProcessingPdf = false,
                        errorMessage = "Could not process this lecture video: ${e.message}"
                    )
                }
            }
        }
    }
    fun buildKnowledgeBase(material: LearningMaterial) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuildingKnowledgeBase = true) }
            val result = knowledgeBaseRepository?.ingestMaterial(material)
            if (result != null && result.isSuccess) {
                val chunks = result.getOrThrow()
                val stats = knowledgeBaseRepository.getStats(material.id)
                _uiState.update { state ->
                    state.copy(
                        isBuildingKnowledgeBase = false,
                        infoMessage = "Knowledge Base Ready: ${stats?.totalTopics ?: 0} Topics, ${stats?.totalConcepts ?: 0} Concepts, ${chunks.size} Chunks",
                        knowledgeBaseStatsMap = if (stats != null) state.knowledgeBaseStatsMap + (material.id to stats) else state.knowledgeBaseStatsMap
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBuildingKnowledgeBase = false,
                        errorMessage = "Failed to construct knowledge base from ${material.name}"
                    )
                }
            }
        }
    }

    /**
     * Opens the Knowledge Base Viewer modal/dialog for a specific material or all materials.
     */
    fun openKnowledgeBase(material: LearningMaterial?) {
        viewModelScope.launch {
            val kbRepo = knowledgeBaseRepository
            if (kbRepo == null) {
                _uiState.update { it.copy(errorMessage = "Knowledge Base is currently initializing.") }
                return@launch
            }

            val targetMaterial = material ?: _uiState.value.materials.firstOrNull()
            if (targetMaterial == null) {
                _uiState.update { it.copy(infoMessage = "Upload a study material first to explore its Knowledge Base.") }
                return@launch
            }

            // Ensure material is chunked in KB
            var chunks = kbRepo.getChunks(targetMaterial.id)
            if (chunks.isEmpty() && targetMaterial.contentUnits.isNotEmpty()) {
                kbRepo.ingestMaterial(targetMaterial)
                chunks = kbRepo.getChunks(targetMaterial.id)
            }

            val topics = kbRepo.getTopics(targetMaterial.id)
            val concepts = kbRepo.getConcepts(targetMaterial.id)
            val stats = kbRepo.getStats(targetMaterial.id)

            _uiState.update {
                it.copy(
                    isKnowledgeBaseViewerVisible = true,
                    selectedMaterialForKb = targetMaterial,
                    kbChunks = chunks,
                    kbTopics = topics,
                    kbConcepts = concepts,
                    selectedTopic = topics.firstOrNull(),
                    searchQuery = "",
                    searchResults = emptyList(),
                    knowledgeBaseStatsMap = if (stats != null) it.knowledgeBaseStatsMap + (targetMaterial.id to stats) else it.knowledgeBaseStatsMap
                )
            }
        }
    }

    fun closeKnowledgeBase() {
        _uiState.update {
            it.copy(
                isKnowledgeBaseViewerVisible = false,
                selectedMaterialForKb = null,
                selectedTopic = null,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun selectTopic(topic: String?) {
        _uiState.update { it.copy(selectedTopic = topic) }
    }

    fun searchKnowledgeBase(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query) }
            if (query.isBlank()) {
                _uiState.update { it.copy(searchResults = emptyList(), retrievalResults = emptyList()) }
                return@launch
            }

            val materialId = _uiState.value.selectedMaterialForKb?.id
            val retrieved = knowledgeBaseRepository?.retrieveRelevantResults(materialId, query) ?: emptyList()
            _uiState.update {
                it.copy(
                    searchResults = retrieved.map { r -> r.chunk },
                    retrievalResults = retrieved
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), retrievalResults = emptyList()) }
    }

    fun openSourcePreview(chunk: KnowledgeChunk) {
        _uiState.update {
            it.copy(
                selectedSourcePreviewChunk = chunk,
                isSourcePreviewVisible = true
            )
        }
    }

    fun dismissSourcePreview() {
        _uiState.update {
            it.copy(
                selectedSourcePreviewChunk = null,
                isSourcePreviewVisible = false
            )
        }
    }

    fun onMaterialSelected(material: LearningMaterial) {
        _uiState.update {
            it.copy(
                selectedMaterialForDetails = material,
                isDocumentDetailsVisible = true
            )
        }
    }

    fun dismissDocumentDetails() {
        _uiState.update {
            it.copy(
                selectedMaterialForDetails = null,
                isDocumentDetailsVisible = false
            )
        }
    }

    /**
     * Delete Learning Material Methods (Safe Deletion)
     */
    fun requestDeleteMaterial(material: LearningMaterial) {
        _uiState.update {
            it.copy(
                materialToDelete = material,
                isDeleteConfirmationVisible = true,
                errorMessage = null
            )
        }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update {
            it.copy(
                materialToDelete = null,
                isDeleteConfirmationVisible = false,
                isDeletingMaterial = false
            )
        }
    }

    fun confirmDeleteMaterial() {
        val material = _uiState.value.materialToDelete ?: return
        if (_uiState.value.isDeletingMaterial) return

        _uiState.update { it.copy(isDeletingMaterial = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Delete material from LearningMaterialRepository
                val deleteMatResult = materialRepository?.deleteMaterial(material.id)
                if (deleteMatResult != null && deleteMatResult.isFailure) {
                    val error = deleteMatResult.exceptionOrNull()?.message ?: "Failed to delete learning material."
                    _uiState.update {
                        it.copy(
                            isDeletingMaterial = false,
                            isDeleteConfirmationVisible = false,
                            materialToDelete = null,
                            errorMessage = error
                        )
                    }
                    return@launch
                }

                // 2. Delete associated knowledge chunks and stats from KnowledgeBaseRepository
                knowledgeBaseRepository?.deleteMaterialKnowledge(material.id)

                // 3. Delete associated learner mastery records from LearnerModelRepository
                learnerModelRepository?.deleteMaterialMastery(material.id)

                // 4. Update UI state cleanly
                _uiState.update { current ->
                    val remainingMaterials = current.materials.filter { it.id != material.id }
                    val updatedStatsMap = current.knowledgeBaseStatsMap - material.id

                    val isSelectedForDetails = current.selectedMaterialForDetails?.id == material.id
                    val isSelectedForKb = current.selectedMaterialForKb?.id == material.id
                    val isSelectedForAssessment = current.selectedAssessmentMaterial?.id == material.id

                    current.copy(
                        materials = remainingMaterials,
                        totalMaterialsCount = remainingMaterials.size,
                        knowledgeBaseStatsMap = updatedStatsMap,
                        isDeleteConfirmationVisible = false,
                        isDeletingMaterial = false,
                        materialToDelete = null,
                        isDocumentDetailsVisible = if (isSelectedForDetails) false else current.isDocumentDetailsVisible,
                        selectedMaterialForDetails = if (isSelectedForDetails) null else current.selectedMaterialForDetails,
                        isKnowledgeBaseViewerVisible = if (isSelectedForKb) false else current.isKnowledgeBaseViewerVisible,
                        selectedMaterialForKb = if (isSelectedForKb) null else current.selectedMaterialForKb,
                        selectedAssessmentMaterial = if (isSelectedForAssessment) remainingMaterials.firstOrNull() else current.selectedAssessmentMaterial,
                        infoMessage = "Material \"${material.name}\" deleted successfully."
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isDeletingMaterial = false,
                        isDeleteConfirmationVisible = false,
                        materialToDelete = null,
                        errorMessage = "Failed to delete material: ${e.message}"
                    )
                }
            }
        }
    }

    fun openAddMaterialDialog(preselectedType: LearningMaterialType? = null) {
        _uiState.update {
            it.copy(
                isAddMaterialDialogVisible = true,
                selectedMaterialType = preselectedType ?: LearningMaterialType.TEXTBOOK_PDF,
                infoMessage = null,
                errorMessage = null
            )
        }
    }

    fun dismissAddMaterialDialog() {
        _uiState.update {
            it.copy(
                isAddMaterialDialogVisible = false
            )
        }
    }

    fun selectMaterialType(type: LearningMaterialType) {
        _uiState.update {
            it.copy(selectedMaterialType = type)
        }
    }

    fun onFeatureCardClicked(feature: LearningCompanionFeatureType) {
        when (feature) {
            LearningCompanionFeatureType.KNOWLEDGE_BASE -> {
                val firstMaterial = _uiState.value.materials.firstOrNull()
                if (firstMaterial != null) {
                    openKnowledgeBase(firstMaterial)
                } else {
                    _uiState.update {
                        it.copy(infoMessage = "Upload your first study material to build and browse the Knowledge Base.")
                    }
                }
            }
            LearningCompanionFeatureType.SOURCE_GROUNDED_TUTOR -> {
                openTutorScreen()
            }
            LearningCompanionFeatureType.ADAPTIVE_ASSESSMENT -> {
                openAssessmentConfig()
            }
            LearningCompanionFeatureType.MY_MASTERY -> {
                openMasteryScreen()
            }
        }
    }

    /**
     * Source-Grounded AI Tutor navigation and interactions.
     */
    fun openTutorScreen() {
        _uiState.update {
            it.copy(
                isTutorScreenVisible = true,
                tutorInputQuery = "",
                tutorStatusMessage = null
            )
        }
    }

    fun closeTutorScreen() {
        _uiState.update {
            it.copy(
                isTutorScreenVisible = false,
                tutorStatusMessage = null,
                isTutorSearching = false,
                isTutorGenerating = false
            )
        }
    }

    fun updateTutorInput(query: String) {
        _uiState.update { it.copy(tutorInputQuery = query) }
    }

    fun askTutor(questionOverride: String? = null) {
        val query = (questionOverride ?: _uiState.value.tutorInputQuery).trim()
        if (query.isBlank()) return

        val userMsgId = UUID.randomUUID().toString()
        val userMsg = TutorMessage(
            id = userMsgId,
            role = TutorMessageRole.USER,
            text = query,
            timestamp = System.currentTimeMillis()
        )

        _uiState.update { state ->
            state.copy(
                tutorInputQuery = "",
                tutorMessages = state.tutorMessages + userMsg,
                isTutorSearching = true,
                isTutorGenerating = false,
                tutorStatusMessage = "Searching your learning material..."
            )
        }

        viewModelScope.launch {
            try {
                val kbRepo = knowledgeBaseRepository
                val materials = _uiState.value.materials

                // Check 1: Empty materials / Knowledge Base
                if (materials.isEmpty() || kbRepo == null) {
                    val refusalMsg = TutorMessage(
                        id = UUID.randomUUID().toString(),
                        role = TutorMessageRole.ASSISTANT,
                        text = "I couldn't find this topic in your uploaded learning material. Please upload study materials to start source-grounded tutoring.",
                        timestamp = System.currentTimeMillis(),
                        isGrounded = false,
                        unsupportedReason = "⚠️ No study materials uploaded yet"
                    )
                    _uiState.update {
                        it.copy(
                            isTutorSearching = false,
                            isTutorGenerating = false,
                            tutorStatusMessage = null,
                            tutorMessages = it.tutorMessages + refusalMsg
                        )
                    }
                    return@launch
                }

                // Ensure all active materials have their knowledge chunks indexed
                for (mat in materials) {
                    if (kbRepo.getChunks(mat.id).isEmpty() && mat.contentUnits.isNotEmpty()) {
                        kbRepo.ingestMaterial(mat)
                    }
                }

                // Step 2: Retrieval across KnowledgeBase
                val relevantChunks = kbRepo.searchChunks(materialId = null, query = query)

                // Step 3: Relevance Check / Refusal
                if (relevantChunks.isEmpty()) {
                    val refusalMsg = TutorMessage(
                        id = UUID.randomUUID().toString(),
                        role = TutorMessageRole.ASSISTANT,
                        text = "I couldn't find this topic in your uploaded learning material.",
                        timestamp = System.currentTimeMillis(),
                        isGrounded = false,
                        unsupportedReason = "⚠️ Not covered in uploaded material"
                    )
                    _uiState.update {
                        it.copy(
                            isTutorSearching = false,
                            isTutorGenerating = false,
                            tutorStatusMessage = null,
                            tutorMessages = it.tutorMessages + refusalMsg
                        )
                    }
                    return@launch
                }

                // Limit context chunks to top 4 strongest relevant chunks
                val topContextChunks = relevantChunks.take(4)

                // Track conversation learning signal for learner model (Step 6)
                val primaryChunk = topContextChunks.firstOrNull()
                val primaryTopic = primaryChunk?.topic ?: "General"
                val primaryMatId = primaryChunk?.materialId ?: ""
                val lowerQuery = query.lowercase()
                val signalType = when {
                    lowerQuery.contains("clarif") || lowerQuery.contains("confus") || lowerQuery.contains("mean") || lowerQuery.contains("again") ->
                        TutorSignalType.ASKED_FOR_CLARIFICATION
                    lowerQuery.contains("example") || lowerQuery.contains("sample") || lowerQuery.contains("instance") ->
                        TutorSignalType.ASKED_FOR_EXAMPLE
                    else ->
                        TutorSignalType.ASKED_FOR_EXPLANATION
                }
                learnerModelRepository?.recordTutorSignal(
                    primaryMatId,
                    TutorLearningSignal(
                        topic = primaryTopic,
                        timestamp = System.currentTimeMillis(),
                        signalType = signalType
                    )
                )

                // Step 4: Contact Secure Server-Side Gemini Backend
                _uiState.update {
                    it.copy(
                        isTutorSearching = false,
                        isTutorGenerating = true,
                        tutorStatusMessage = "Generating explanation..."
                    )
                }

                val tutorRepo = tutorRepository
                if (tutorRepo == null) {
                    val assistantMsg = TutorMessage(
                        id = UUID.randomUUID().toString(),
                        role = TutorMessageRole.ASSISTANT,
                        text = "AI Tutor service is not connected. Please verify configuration.",
                        timestamp = System.currentTimeMillis(),
                        isGrounded = false,
                        unsupportedReason = "Backend not initialized"
                    )
                    _uiState.update {
                        it.copy(
                            isTutorSearching = false,
                            isTutorGenerating = false,
                            tutorStatusMessage = null,
                            tutorMessages = it.tutorMessages + assistantMsg
                        )
                    }
                    return@launch
                }

                val tutorResult = tutorRepo.askTutor(query, topContextChunks)

                if (tutorResult.isSuccess) {
                    val response = tutorResult.getOrThrow()
                    val assistantMsg = TutorMessage(
                        id = UUID.randomUUID().toString(),
                        role = TutorMessageRole.ASSISTANT,
                        text = response.answer,
                        timestamp = System.currentTimeMillis(),
                        sources = response.sources,
                        isGrounded = response.grounded && response.sources.isNotEmpty(),
                        unsupportedReason = if (response.grounded) null else (response.unsupportedReason ?: "⚠️ Not covered in uploaded material")
                    )
                    _uiState.update {
                        it.copy(
                            isTutorSearching = false,
                            isTutorGenerating = false,
                            tutorStatusMessage = null,
                            tutorMessages = it.tutorMessages + assistantMsg
                        )
                    }
                } else {
                    val errMsg = tutorResult.exceptionOrNull()?.message ?: "Failed to generate tutor explanation."
                    val fallbackMsg = TutorMessage(
                        id = UUID.randomUUID().toString(),
                        role = TutorMessageRole.ASSISTANT,
                        text = "I encountered a problem contacting the AI tutor backend: $errMsg",
                        timestamp = System.currentTimeMillis(),
                        isGrounded = false,
                        unsupportedReason = "Network / Backend Error"
                    )
                    _uiState.update {
                        it.copy(
                            isTutorSearching = false,
                            isTutorGenerating = false,
                            tutorStatusMessage = null,
                            tutorMessages = it.tutorMessages + fallbackMsg
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val errorMsg = TutorMessage(
                    id = UUID.randomUUID().toString(),
                    role = TutorMessageRole.ASSISTANT,
                    text = "Could not process your question. Please try again.",
                    timestamp = System.currentTimeMillis(),
                    isGrounded = false,
                    unsupportedReason = "Unexpected error"
                )
                _uiState.update {
                    it.copy(
                        isTutorSearching = false,
                        isTutorGenerating = false,
                        tutorStatusMessage = null,
                        tutorMessages = it.tutorMessages + errorMsg
                    )
                }
            }
        }
    }

    fun openTutorSourcePreview(source: TutorSource) {
        val previewChunk = KnowledgeChunk(
            id = source.chunkId,
            materialId = source.materialId,
            documentName = source.documentName,
            pageNumber = source.pageNumber,
            text = source.previewText.ifBlank { "Source reference: ${source.documentName} page ${source.pageNumber}." },
            topic = source.documentName,
            concept = "Page ${source.pageNumber}"
        )
        _uiState.update {
            it.copy(
                selectedSourcePreviewChunk = previewChunk,
                isSourcePreviewVisible = true
            )
        }
    }

    /**
     * Adaptive Assessment Methods (Step 5)
     */
    fun openAssessmentConfig(material: LearningMaterial? = null) {
        val targetMaterial = material ?: _uiState.value.selectedAssessmentMaterial ?: _uiState.value.materials.firstOrNull()
        if (targetMaterial == null) {
            _uiState.update {
                it.copy(errorMessage = "Build a Knowledge Base before creating an assessment.")
            }
            return
        }

        viewModelScope.launch {
            var chunks = knowledgeBaseRepository?.getChunks(targetMaterial.id) ?: emptyList()
            if (chunks.isEmpty() && targetMaterial.contentUnits.isNotEmpty()) {
                knowledgeBaseRepository?.ingestMaterial(targetMaterial)
                chunks = knowledgeBaseRepository?.getChunks(targetMaterial.id) ?: emptyList()
            }
            val stats = knowledgeBaseRepository?.getStats(targetMaterial.id)
            if (stats == null || stats.totalChunks == 0 || chunks.isEmpty()) {
                _uiState.update {
                    it.copy(errorMessage = "Build a Knowledge Base before creating an assessment.")
                }
                return@launch
            }

            val topics = knowledgeBaseRepository.getTopics(targetMaterial.id)
            val topicOptions = listOf("All Topics") + topics

            _uiState.update {
                it.copy(
                    isAssessmentConfigVisible = true,
                    selectedAssessmentMaterial = targetMaterial,
                    availableAssessmentTopics = topicOptions,
                    selectedAssessmentTopic = "All Topics",
                    selectedAssessmentDifficulty = AssessmentDifficulty.MIXED,
                    selectedQuestionCount = 5,
                    errorMessage = null
                )
            }
        }
    }

    fun closeAssessmentConfig() {
        _uiState.update {
            it.copy(
                isAssessmentConfigVisible = false,
                isGeneratingAssessment = false
            )
        }
    }

    fun selectAssessmentMaterial(material: LearningMaterial) {
        viewModelScope.launch {
            val topics = knowledgeBaseRepository?.getTopics(material.id) ?: emptyList()
            _uiState.update {
                it.copy(
                    selectedAssessmentMaterial = material,
                    availableAssessmentTopics = listOf("All Topics") + topics,
                    selectedAssessmentTopic = "All Topics"
                )
            }
        }
    }

    fun selectAssessmentTopic(topic: String) {
        _uiState.update { it.copy(selectedAssessmentTopic = topic) }
    }

    fun selectAssessmentDifficulty(difficulty: AssessmentDifficulty) {
        _uiState.update { it.copy(selectedAssessmentDifficulty = difficulty) }
    }

    fun selectAssessmentQuestionCount(count: Int) {
        _uiState.update { it.copy(selectedQuestionCount = count) }
    }

    fun generateAssessment() {
        val material = _uiState.value.selectedAssessmentMaterial ?: _uiState.value.materials.firstOrNull()
        if (material == null) {
            _uiState.update { it.copy(errorMessage = "Build a Knowledge Base before creating an assessment.") }
            return
        }

        val scope = _uiState.value.selectedAssessmentTopic
        val difficulty = _uiState.value.selectedAssessmentDifficulty
        val count = _uiState.value.selectedQuestionCount

        _uiState.update {
            it.copy(
                isGeneratingAssessment = true,
                assessmentGenerationMessage = "Generating $count questions grounded in ${material.name}...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val kbRepo = knowledgeBaseRepository
                val assessmentRepo = adaptiveAssessmentRepository

                if (kbRepo == null) {
                    _uiState.update {
                        it.copy(
                            isGeneratingAssessment = false,
                            errorMessage = "Knowledge Base is not available."
                        )
                    }
                    return@launch
                }

                if (kbRepo.getChunks(material.id).isEmpty() && material.contentUnits.isNotEmpty()) {
                    kbRepo.ingestMaterial(material)
                }

                // Retrieve context chunks according to scope
                val allChunks = if (scope.equals("All Topics", ignoreCase = true)) {
                    kbRepo.getChunks(material.id)
                } else {
                    kbRepo.getChunksByTopic(material.id, scope).ifEmpty {
                        kbRepo.getChunks(material.id)
                    }
                }

                if (allChunks.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isGeneratingAssessment = false,
                            errorMessage = "Build a Knowledge Base before creating an assessment."
                        )
                    }
                    return@launch
                }

                // Supply substantive chunks sampled evenly across the material
                val substantiveChunks = allChunks.filter { it.text.trim().length >= 25 }.ifEmpty { allChunks }
                val maxChunks = maxOf(10, count)
                val targetChunks = if (substantiveChunks.size > maxChunks) {
                    val step = substantiveChunks.size.toDouble() / maxChunks
                    (0 until maxChunks).map { idx ->
                        substantiveChunks[(idx * step).toInt().coerceIn(0, substantiveChunks.lastIndex)]
                    }.distinctBy { it.id }
                } else {
                    substantiveChunks
                }

                val repo = assessmentRepo ?: SupabaseAdaptiveAssessmentRepository()
                val result = repo.generateAssessment(
                    materialId = material.id,
                    documentName = material.name,
                    scope = scope,
                    difficulty = difficulty,
                    questionCount = count,
                    contextChunks = targetChunks
                )

                if (result.isSuccess) {
                    val assessment = result.getOrThrow()
                    _uiState.update {
                        it.copy(
                            isGeneratingAssessment = false,
                            isAssessmentConfigVisible = false,
                            isAssessmentQuizVisible = true,
                            isAssessmentResultVisible = false,
                            activeAssessment = assessment,
                            currentQuestionIndex = 0,
                            studentAnswers = emptyMap(),
                            currentShortAnswerInput = "",
                            assessmentResult = null,
                            infoMessage = "Assessment ready: ${assessment.questions.size} questions"
                        )
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to generate assessment questions."
                    _uiState.update {
                        it.copy(
                            isGeneratingAssessment = false,
                            errorMessage = err
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isGeneratingAssessment = false,
                        errorMessage = "An unexpected error occurred while generating assessment."
                    )
                }
            }
        }
    }

    fun selectAnswer(questionId: String, answer: String) {
        _uiState.update {
            it.copy(studentAnswers = it.studentAnswers + (questionId to answer))
        }
    }

    fun updateShortAnswerInput(text: String) {
        _uiState.update { it.copy(currentShortAnswerInput = text) }
    }

    fun saveCurrentShortAnswer(questionId: String) {
        val input = _uiState.value.currentShortAnswerInput.trim()
        if (input.isNotBlank()) {
            selectAnswer(questionId, input)
        }
    }

    fun nextQuestion() {
        val active = _uiState.value.activeAssessment ?: return
        val currentIdx = _uiState.value.currentQuestionIndex
        val currentQ = active.questions.getOrNull(currentIdx)
        if (currentQ != null && currentQ.type == AssessmentQuestionType.SHORT_ANSWER) {
            saveCurrentShortAnswer(currentQ.id)
        }
        if (currentIdx < active.questions.size - 1) {
            val nextIdx = currentIdx + 1
            val nextQ = active.questions.getOrNull(nextIdx)
            val existingAnswer = if (nextQ != null) _uiState.value.studentAnswers[nextQ.id].orEmpty() else ""
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIdx,
                    currentShortAnswerInput = existingAnswer
                )
            }
        }
    }

    fun previousQuestion() {
        val active = _uiState.value.activeAssessment ?: return
        val currentIdx = _uiState.value.currentQuestionIndex
        val currentQ = active.questions.getOrNull(currentIdx)
        if (currentQ != null && currentQ.type == AssessmentQuestionType.SHORT_ANSWER) {
            saveCurrentShortAnswer(currentQ.id)
        }
        if (currentIdx > 0) {
            val prevIdx = currentIdx - 1
            val prevQ = active.questions.getOrNull(prevIdx)
            val existingAnswer = if (prevQ != null) _uiState.value.studentAnswers[prevQ.id].orEmpty() else ""
            _uiState.update {
                it.copy(
                    currentQuestionIndex = prevIdx,
                    currentShortAnswerInput = existingAnswer
                )
            }
        }
    }

    fun goToQuestion(index: Int) {
        val active = _uiState.value.activeAssessment ?: return
        if (index in 0 until active.questions.size) {
            val currentQ = active.questions.getOrNull(_uiState.value.currentQuestionIndex)
            if (currentQ != null && currentQ.type == AssessmentQuestionType.SHORT_ANSWER) {
                saveCurrentShortAnswer(currentQ.id)
            }
            val targetQ = active.questions[index]
            val existingAnswer = _uiState.value.studentAnswers[targetQ.id].orEmpty()
            _uiState.update {
                it.copy(
                    currentQuestionIndex = index,
                    currentShortAnswerInput = existingAnswer
                )
            }
        }
    }

    fun submitAssessment() {
        val active = _uiState.value.activeAssessment ?: return
        val currentIdx = _uiState.value.currentQuestionIndex
        val currentQ = active.questions.getOrNull(currentIdx)
        if (currentQ != null && currentQ.type == AssessmentQuestionType.SHORT_ANSWER) {
            saveCurrentShortAnswer(currentQ.id)
        }

        val answers = _uiState.value.studentAnswers
        val repo = adaptiveAssessmentRepository ?: SupabaseAdaptiveAssessmentRepository()
        val result = repo.evaluateAssessment(active, answers)

        viewModelScope.launch {
            val updatedMasteries = learnerModelRepository?.updateFromAssessment(active.materialId, result) ?: emptyList()
            val recommended = learnerModelRepository?.getRecommendedTopic(active.materialId)
            val allMasteries = learnerModelRepository?.getAllMastery(active.materialId) ?: updatedMasteries
            val avgMastery = if (allMasteries.isNotEmpty()) {
                (allMasteries.map { it.masteryScore }.average()).roundToInt()
            } else {
                result.scorePercentage
            }

            _uiState.update { state ->
                state.copy(
                    isAssessmentQuizVisible = false,
                    isAssessmentResultVisible = true,
                    assessmentResult = result,
                    overallMastery = avgMastery,
                    topicMasteries = allMasteries,
                    recommendedTopicMastery = recommended,
                    hasMasteryHistory = allMasteries.isNotEmpty()
                )
            }
        }
    }

    fun closeAssessmentQuiz() {
        _uiState.update {
            it.copy(
                isAssessmentQuizVisible = false,
                activeAssessment = null,
                studentAnswers = emptyMap()
            )
        }
    }

    fun closeAssessmentResult() {
        _uiState.update {
            it.copy(
                isAssessmentResultVisible = false,
                assessmentResult = null,
                activeAssessment = null,
                studentAnswers = emptyMap()
            )
        }
    }

    fun practiceWeakTopic(topic: String) {
        val material = _uiState.value.selectedAssessmentMaterial ?: _uiState.value.materials.firstOrNull()
        _uiState.update {
            it.copy(
                isAssessmentResultVisible = false,
                selectedAssessmentTopic = topic,
                selectedAssessmentMaterial = material,
                isAssessmentConfigVisible = true
            )
        }
    }

    fun askTutorAboutWeakTopic(topic: String) {
        val query = "Explain $topic from my uploaded material with key concepts and examples."
        _uiState.update {
            it.copy(
                isAssessmentResultVisible = false,
                isTutorScreenVisible = true,
                tutorInputQuery = query
            )
        }
        askTutor(questionOverride = query)
    }

    fun openQuestionResultSource(result: QuestionResult) {
        val previewChunk = KnowledgeChunk(
            id = result.sourceChunkId,
            materialId = _uiState.value.selectedAssessmentMaterial?.id ?: "",
            documentName = result.documentName,
            pageNumber = result.pageNumber,
            text = result.explanation.ifBlank { "Source reference: ${result.documentName} • Page ${result.pageNumber}" },
            topic = result.topic,
            concept = result.concept
        )
        _uiState.update {
            it.copy(
                selectedSourcePreviewChunk = previewChunk,
                isSourcePreviewVisible = true
            )
        }
    }

    /**
     * Personalized Learner Model / Mastery Navigation (Step 6)
     */
    fun openMasteryScreen() {
        viewModelScope.launch {
            val matId = _uiState.value.materials.firstOrNull()?.id.orEmpty()
            val masteries = if (matId.isNotBlank()) {
                learnerModelRepository?.getAllMastery(matId) ?: emptyList()
            } else {
                learnerModelRepository?.getAllMasteryFlow()?.firstOrNull() ?: emptyList()
            }
            val rec = if (matId.isNotBlank()) {
                learnerModelRepository?.getRecommendedTopic(matId)
            } else null

            _uiState.update {
                it.copy(
                    isMasteryScreenVisible = true,
                    topicMasteries = masteries,
                    hasMasteryHistory = masteries.isNotEmpty(),
                    recommendedTopicMastery = rec
                )
            }
        }
    }

    fun closeMasteryScreen() {
        _uiState.update { it.copy(isMasteryScreenVisible = false) }
    }

    fun practiceTopicFromMastery(topic: String) {
        val material = _uiState.value.materials.firstOrNull()
        _uiState.update {
            it.copy(
                isMasteryScreenVisible = false,
                selectedAssessmentTopic = topic,
                selectedAssessmentMaterial = material,
                isAssessmentConfigVisible = true
            )
        }
    }

    fun askTutorFromMastery(topic: String) {
        val query = "Explain $topic from my uploaded material with key concepts and examples."
        _uiState.update {
            it.copy(
                isMasteryScreenVisible = false,
                isTutorScreenVisible = true,
                tutorInputQuery = query
            )
        }
        askTutor(questionOverride = query)
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
