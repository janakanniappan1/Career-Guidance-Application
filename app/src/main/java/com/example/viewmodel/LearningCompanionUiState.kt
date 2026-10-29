package com.example.viewmodel

import com.example.domain.model.KnowledgeBaseStats
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningCompanionFeatureType
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialType

/**
 * UI State for the AI Learning Companion screen and Knowledge Base Viewer.
 */
data class LearningCompanionUiState(
    val isLoading: Boolean = false,
    val isProcessingPdf: Boolean = false,
    val isBuildingKnowledgeBase: Boolean = false,
    val processingMessage: String = "Processing your textbook...",
    val studentName: String = "Student",
    val careerGoal: String = "Technology & Engineering",
    val overallMastery: Int = 0, // Initially 0% as requested
    val totalMaterialsCount: Int = 0,
    val materials: List<LearningMaterial> = emptyList(),
    val availableMaterialTypes: List<LearningMaterialType> = LearningMaterialType.entries,
    val availableFeatures: List<LearningCompanionFeatureType> = LearningCompanionFeatureType.entries,
    val selectedMaterialType: LearningMaterialType? = null,
    val isAddMaterialDialogVisible: Boolean = false,
    val selectedMaterialForDetails: LearningMaterial? = null,
    val isDocumentDetailsVisible: Boolean = false,
    // Delete Confirmation State
    val materialToDelete: LearningMaterial? = null,
    val isDeleteConfirmationVisible: Boolean = false,
    val isDeletingMaterial: Boolean = false,
    // Knowledge Base State
    val knowledgeBaseStatsMap: Map<String, KnowledgeBaseStats> = emptyMap(),
    val selectedMaterialForKb: LearningMaterial? = null,
    val isKnowledgeBaseViewerVisible: Boolean = false,
    val kbTopics: List<String> = emptyList(),
    val kbConcepts: List<String> = emptyList(),
    val kbChunks: List<KnowledgeChunk> = emptyList(),
    val selectedTopic: String? = null,
    val searchQuery: String = "",
    val searchResults: List<KnowledgeChunk> = emptyList(),
    val retrievalResults: List<com.example.domain.model.RetrievalResult> = emptyList(),
    val selectedSourcePreviewChunk: KnowledgeChunk? = null,
    val isSourcePreviewVisible: Boolean = false,
    // Source-Grounded AI Tutor State
    val isTutorScreenVisible: Boolean = false,
    val isTutorSearching: Boolean = false,
    val isTutorGenerating: Boolean = false,
    val tutorStatusMessage: String? = null,
    val tutorMessages: List<com.example.domain.model.TutorMessage> = emptyList(),
    val tutorInputQuery: String = "",
    // Adaptive Assessment State
    val isAssessmentConfigVisible: Boolean = false,
    val isAssessmentQuizVisible: Boolean = false,
    val isAssessmentResultVisible: Boolean = false,
    val isGeneratingAssessment: Boolean = false,
    val assessmentGenerationMessage: String = "Generating source-grounded questions...",
    val selectedAssessmentMaterial: LearningMaterial? = null,
    val availableAssessmentTopics: List<String> = emptyList(),
    val selectedAssessmentTopic: String = "All Topics",
    val selectedAssessmentDifficulty: com.example.domain.model.adaptive.AssessmentDifficulty = com.example.domain.model.adaptive.AssessmentDifficulty.MIXED,
    val selectedQuestionCount: Int = 5,
    val activeAssessment: com.example.domain.model.adaptive.Assessment? = null,
    val currentQuestionIndex: Int = 0,
    val studentAnswers: Map<String, String> = emptyMap(),
    val currentShortAnswerInput: String = "",
    val assessmentResult: com.example.domain.model.adaptive.AssessmentResult? = null,
    // Personalized Learner Model / Mastery State (Step 6)
    val isMasteryScreenVisible: Boolean = false,
    val topicMasteries: List<com.example.domain.model.adaptive.TopicMastery> = emptyList(),
    val recommendedTopicMastery: com.example.domain.model.adaptive.TopicMastery? = null,
    val recommendedFocusMessage: String? = null,
    val hasMasteryHistory: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)
