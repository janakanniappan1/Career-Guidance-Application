package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.KnowledgeBaseStats
import com.example.domain.model.KnowledgeChunk
import com.example.domain.model.LearningCompanionFeatureType
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.PdfContentUnit
import com.example.domain.model.TutorMessage
import com.example.domain.model.TutorMessageRole
import com.example.domain.model.TutorSource
import com.example.ui.components.CgaBottomNavigation
import com.example.ui.components.CgaButtonVariant
import com.example.ui.components.CgaPrimaryButton
import com.example.ui.components.CgaProgressBar
import com.example.ui.components.CgaSecondaryButton
import com.example.ui.components.CgaTopBar
import com.example.ui.components.TagPill
import com.example.ui.theme.CGAColors
import com.example.ui.theme.CGADesignTokens
import com.example.ui.theme.LocalCGATokens
import com.example.viewmodel.LearningCompanionViewModel

/**
 * AI Learning Companion Screen
 * Multimodal AI Hackathon 2026 - Track D: Personalized Tutoring & Adaptive Learning.
 */
@Composable
fun LearningCompanionScreen(
    viewModel: LearningCompanionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToRoadmap: (String) -> Unit = {},
    onNavigateToPractice: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tokens = LocalCGATokens.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Android PDF Document Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.ingestPdf(it)
        }
    }

    // Android PowerPoint (.pptx) Document Picker Launcher
    val pptxPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.ingestPptx(it)
        }
    }

    // Android Lecture Video (.mp4) Document Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.ingestVideo(it)
        }
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    if (state.isTutorScreenVisible) {
        BackHandler {
            viewModel.closeTutorScreen()
        }
        SourceGroundedTutorView(
            state = state,
            onBackClick = { viewModel.closeTutorScreen() },
            onSendMessage = { viewModel.askTutor() },
            onQueryChanged = { viewModel.updateTutorInput(it) },
            onSourceClick = { source -> viewModel.openTutorSourcePreview(source) },
            tokens = tokens,
            modifier = modifier
        )
    } else if (state.isAssessmentQuizVisible && state.activeAssessment != null) {
        AdaptiveAssessmentQuizView(
            state = state,
            viewModel = viewModel,
            tokens = tokens,
            modifier = modifier
        )
    } else if (state.isAssessmentResultVisible && state.assessmentResult != null) {
        AdaptiveAssessmentResultView(
            state = state,
            viewModel = viewModel,
            tokens = tokens,
            modifier = modifier
        )
    } else if (state.isMasteryScreenVisible) {
        MasteryDashboardView(
            state = state,
            viewModel = viewModel,
            tokens = tokens,
            modifier = modifier
        )
    } else {
        Scaffold(
            topBar = {
                CgaTopBar(
                    title = "AI Learning Companion",
                    onBackClick = onNavigateBack,
                    testTag = "learning_companion_top_bar"
                )
            },
            bottomBar = {
                CgaBottomNavigation(
                    currentRoute = "dashboard",
                    onNavigate = { route ->
                        when (route) {
                            "dashboard" -> onNavigateToDashboard()
                            "roadmap" -> onNavigateToRoadmap("")
                            "practice" -> onNavigateToPractice()
                            "explore" -> onNavigateToExplore()
                            "profile" -> onNavigateToProfile()
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = tokens.contentBackground,
            modifier = modifier.testTag("learning_companion_screen")
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 1. HEADER SECTION
                LearningCompanionHeader(
                    careerGoal = state.careerGoal,
                    tokens = tokens
                )

                Spacer(modifier = Modifier.height(18.dp))

                // PROCESSING STATE BANNER
                if (state.isProcessingPdf || state.isBuildingKnowledgeBase) {
                    ProcessingPdfBanner(
                        message = if (state.isBuildingKnowledgeBase) "Structuring knowledge chunks and concepts..." else state.processingMessage,
                        tokens = tokens
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 6. OVERALL MASTERY PROGRESS AREA (Initially 0%)
                OverallMasterySection(
                    masteryPercentage = state.overallMastery,
                    materialsCount = state.totalMaterialsCount,
                    tokens = tokens
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. MAIN ACTION: Add Learning Material Button
                CgaPrimaryButton(
                    text = "Add Learning Material",
                    onClick = { viewModel.openAddMaterialDialog() },
                    leadingIcon = Icons.Default.Add,
                    variant = CgaButtonVariant.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_learning_material_button")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. MATERIAL TYPES SECTION (Textbook / PDF, Slides, Lecture Video)
                MaterialTypesSection(
                    materialTypes = state.availableMaterialTypes,
                    onSelectType = { type ->
                        if (type == LearningMaterialType.TEXTBOOK_PDF) {
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        } else {
                            viewModel.openAddMaterialDialog(type)
                        }
                    },
                    tokens = tokens
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 4. MY LEARNING MATERIALS SECTION (or Empty State)
                if (state.materials.isNotEmpty()) {
                    MyLearningMaterialsSection(
                        materials = state.materials,
                        statsMap = state.knowledgeBaseStatsMap,
                        onMaterialClick = { material -> viewModel.onMaterialSelected(material) },
                        onOpenKbClick = { material -> viewModel.openKnowledgeBase(material) },
                        onDeleteClick = { material -> viewModel.requestDeleteMaterial(material) },
                        onAddMoreClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        tokens = tokens
                    )
                } else {
                    ContinueLearningSection(
                        onUploadClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        tokens = tokens
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 5. FEATURE CARDS GRID (Knowledge Base, Source-Grounded Tutor, Adaptive Assessment, My Mastery)
                FeatureCardsSection(
                    features = state.availableFeatures,
                    onFeatureClick = { feature -> viewModel.onFeatureCardClicked(feature) },
                    tokens = tokens
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Material Modal Dialog
    if (state.isAddMaterialDialogVisible) {
        AddMaterialDialog(
            selectedType = state.selectedMaterialType ?: LearningMaterialType.TEXTBOOK_PDF,
            availableTypes = state.availableMaterialTypes,
            onSelectType = { type ->
                viewModel.selectMaterialType(type)
                if (type == LearningMaterialType.TEXTBOOK_PDF) {
                    viewModel.dismissAddMaterialDialog()
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                } else if (type == LearningMaterialType.SLIDES) {
                    viewModel.dismissAddMaterialDialog()
                    pptxPickerLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/octet-stream"
                        )
                    )
                } else if (type == LearningMaterialType.LECTURE_VIDEO) {
                    viewModel.dismissAddMaterialDialog()
                    videoPickerLauncher.launch(arrayOf("video/mp4", "video/*"))
                }
            },
            onConfirmAction = { type ->
                viewModel.dismissAddMaterialDialog()
                if (type == LearningMaterialType.TEXTBOOK_PDF) {
                    pdfPickerLauncher.launch(arrayOf("application/pdf"))
                } else if (type == LearningMaterialType.SLIDES) {
                    pptxPickerLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/octet-stream"
                        )
                    )
                } else if (type == LearningMaterialType.LECTURE_VIDEO) {
                    videoPickerLauncher.launch(arrayOf("video/mp4", "video/*"))
                }
            },
            onDismiss = { viewModel.dismissAddMaterialDialog() },
            tokens = tokens
        )
    }

    // Delete Learning Material Confirmation Dialog
    if (state.isDeleteConfirmationVisible && state.materialToDelete != null) {
        DeleteMaterialConfirmationDialog(
            materialName = state.materialToDelete!!.name,
            isDeleting = state.isDeletingMaterial,
            onConfirm = { viewModel.confirmDeleteMaterial() },
            onDismiss = { viewModel.dismissDeleteConfirmation() },
            tokens = tokens
        )
    }

    // Document Details Modal
    if (state.isDocumentDetailsVisible && state.selectedMaterialForDetails != null) {
        val selectedMat = state.selectedMaterialForDetails!!
        val kbStats = state.knowledgeBaseStatsMap[selectedMat.id]
        DocumentDetailsDialog(
            material = selectedMat,
            kbStats = kbStats,
            onBuildKnowledgeBase = { viewModel.buildKnowledgeBase(selectedMat) },
            onOpenKnowledgeBase = {
                viewModel.dismissDocumentDetails()
                viewModel.openKnowledgeBase(selectedMat)
            },
            onDismiss = { viewModel.dismissDocumentDetails() },
            tokens = tokens
        )
    }

    // Knowledge Base Viewer Modal
    if (state.isKnowledgeBaseViewerVisible && state.selectedMaterialForKb != null) {
        val material = state.selectedMaterialForKb!!
        val stats = state.knowledgeBaseStatsMap[material.id]
        KnowledgeBaseViewerDialog(
            material = material,
            stats = stats,
            topics = state.kbTopics,
            concepts = state.kbConcepts,
            chunks = state.kbChunks,
            selectedTopic = state.selectedTopic,
            searchQuery = state.searchQuery,
            searchResults = state.searchResults,
            retrievalResults = state.retrievalResults,
            onSelectTopic = { viewModel.selectTopic(it) },
            onSearchQueryChanged = { viewModel.searchKnowledgeBase(it) },
            onClearSearch = { viewModel.clearSearch() },
            onChunkClick = { chunk -> viewModel.openSourcePreview(chunk) },
            onDismiss = { viewModel.closeKnowledgeBase() },
            tokens = tokens
        )
    }

    // Source Reference Preview Modal
    if (state.isSourcePreviewVisible && state.selectedSourcePreviewChunk != null) {
        SourcePreviewDialog(
            chunk = state.selectedSourcePreviewChunk!!,
            onDismiss = { viewModel.dismissSourcePreview() },
            tokens = tokens
        )
    }

    // Adaptive Assessment Configuration Dialog
    if (state.isAssessmentConfigVisible) {
        AssessmentConfigDialog(
            state = state,
            viewModel = viewModel,
            tokens = tokens
        )
    }
}

/**
 * SOURCE-GROUNDED AI TUTOR SCREEN VIEW
 */
@Composable
private fun SourceGroundedTutorView(
    state: com.example.viewmodel.LearningCompanionUiState,
    onBackClick: () -> Unit,
    onSendMessage: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSourceClick: (TutorSource) -> Unit,
    tokens: CGADesignTokens,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.tutorMessages.size, state.tutorStatusMessage) {
        if (state.tutorMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.tutorMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CgaTopBar(
                title = "AI Tutor",
                onBackClick = onBackClick,
                testTag = "ai_tutor_top_bar"
            )
        },
        containerColor = tokens.contentBackground,
        modifier = modifier.testTag("ai_tutor_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Header Info Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .border(width = 1.dp, color = tokens.borderMain)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CGAColors.LightPurpleIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = CGAColors.ElectricPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Source-Grounded AI Tutor",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = tokens.textHeading
                        )
                        Text(
                            text = "Ask questions about your uploaded learning material.",
                            fontSize = 11.sp,
                            color = tokens.textMuted
                        )
                    }
                }
            }

            // Chat Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.tutorMessages.isEmpty()) {
                    // Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(tokens.surface)
                                .border(1.5.dp, tokens.borderMain, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CGAColors.ElectricPurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ask questions from your materials",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = tokens.textHeading,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Every answer is strictly grounded with page-level citations from your textbooks and PDFs.",
                            fontSize = 12.sp,
                            color = tokens.textMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Quick suggestion prompt pills
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TutorSuggestionPrompt(
                                prompt = "What are the core concepts covered in my material?",
                                onClick = {
                                    onQueryChanged("What are the core concepts covered in my material?")
                                    onSendMessage()
                                },
                                tokens = tokens
                            )
                            TutorSuggestionPrompt(
                                prompt = "Explain the key definitions from page 1.",
                                onClick = {
                                    onQueryChanged("Explain the key definitions from page 1.")
                                    onSendMessage()
                                },
                                tokens = tokens
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.tutorMessages) { msg ->
                            TutorMessageBubble(
                                message = msg,
                                onSourceClick = onSourceClick,
                                tokens = tokens
                            )
                        }

                        // In-flight status bubble
                        if (state.isTutorSearching || state.isTutorGenerating) {
                            item {
                                InFlightTutorStatusBubble(
                                    statusText = state.tutorStatusMessage ?: "Thinking...",
                                    tokens = tokens
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tokens.surface)
                    .border(width = 1.dp, color = tokens.borderMain)
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = state.tutorInputQuery,
                        onValueChange = onQueryChanged,
                        placeholder = {
                            Text(
                                text = "Ask about concepts, definitions, formulas...",
                                fontSize = 12.sp,
                                color = tokens.textMuted
                            )
                        },
                        singleLine = true,
                        enabled = !state.isTutorSearching && !state.isTutorGenerating,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CGAColors.Primary,
                            unfocusedBorderColor = tokens.borderMain,
                            focusedContainerColor = tokens.contentBackground,
                            unfocusedContainerColor = tokens.contentBackground
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tutor_input_field")
                    )

                    IconButton(
                        onClick = onSendMessage,
                        enabled = state.tutorInputQuery.isNotBlank() && !state.isTutorSearching && !state.isTutorGenerating,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.tutorInputQuery.isNotBlank() && !state.isTutorSearching && !state.isTutorGenerating)
                                    CGAColors.Primary
                                else
                                    tokens.borderMain
                            )
                            .testTag("tutor_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorSuggestionPrompt(
    prompt: String,
    onClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.surface)
            .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = prompt,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = tokens.textHeading
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = tokens.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun InFlightTutorStatusBubble(
    statusText: String,
    tokens: CGADesignTokens
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surface)
            .border(1.dp, tokens.borderMain, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("tutor_inflight_status")
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = CGAColors.ElectricPurple
        )
        Text(
            text = statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = tokens.textHeading
        )
    }
}

@Composable
private fun TutorMessageBubble(
    message: TutorMessage,
    onSourceClick: (TutorSource) -> Unit,
    tokens: CGADesignTokens
) {
    val isUser = message.role == TutorMessageRole.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(CGAColors.Primary)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("user_message_${message.id}")
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(tokens.surface)
                    .border(1.5.dp, tokens.borderMain, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                    .padding(14.dp)
                    .testTag("assistant_message_${message.id}")
            ) {
                Column {
                    // Grounding Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isGrounded) {
                            TagPill(
                                text = "✅ Grounded in your material",
                                backgroundColor = tokens.successContainer,
                                textColor = tokens.onSuccessContainer
                            )
                        } else {
                            TagPill(
                                text = message.unsupportedReason ?: "⚠️ Not covered in uploaded material",
                                backgroundColor = tokens.warningContainer,
                                textColor = tokens.onWarningContainer
                            )
                        }

                        Text(
                            text = "AI TUTOR",
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            color = tokens.textMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Explanation Answer
                    Text(
                        text = message.text,
                        color = tokens.textHeading,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    // Grounded Sources Citations Section
                    if (message.sources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.contentBackground)
                                .border(1.dp, tokens.borderMain, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .testTag("citations_section_${message.id}")
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = CGAColors.ElectricPurple,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "📚 Sources (${message.sources.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = tokens.textHeading
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    message.sources.forEach { source ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(tokens.surface)
                                                .border(1.dp, tokens.borderMain, RoundedCornerShape(8.dp))
                                                .clickable { onSourceClick(source) }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .testTag("citation_source_${source.chunkId}"),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = source.documentName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tokens.textHeading,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                TagPill(
                                                    text = source.unitLabel,
                                                    backgroundColor = CGAColors.LightPurpleIconBg,
                                                    textColor = CGAColors.ElectricPurple
                                                )
                                            }

                                            Text(
                                                text = "View >",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CGAColors.Primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. Header Component
 */
@Composable
private fun LearningCompanionHeader(
    careerGoal: String,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("learning_companion_header_card")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CGAColors.LightPurpleIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CGAColors.ElectricPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "AI Learning Companion",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }

                TagPill(
                    text = "TRACK D",
                    backgroundColor = CGAColors.LightPurpleIconBg,
                    textColor = CGAColors.ElectricPurple
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Learn from your own study materials with personalized AI guidance.",
                color = tokens.textBody,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )

            if (careerGoal.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Target Career Goal:",
                        color = tokens.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TagPill(
                        text = careerGoal,
                        backgroundColor = tokens.infoContainer,
                        textColor = tokens.onInfoContainer
                    )
                }
            }
        }
    }
}

/**
 * Processing Banner
 */
@Composable
private fun ProcessingPdfBanner(
    message: String,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.infoContainer)
            .border(1.5.dp, CGAColors.Primary, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("pdf_processing_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = CGAColors.Primary,
                strokeWidth = 2.5.dp
            )
            Column {
                Text(
                    text = message,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Extracting page-level text units & indexing contents locally",
                    color = tokens.textMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Overall Mastery Area Component (Initially 0%)
 */
@Composable
private fun OverallMasterySection(
    masteryPercentage: Int,
    materialsCount: Int,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("overall_mastery_card")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Mastery",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (materialsCount == 0) "No study materials analyzed yet" else "$materialsCount active materials",
                        color = tokens.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (masteryPercentage > 0) tokens.successContainer else tokens.infoContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$masteryPercentage%",
                        color = if (masteryPercentage > 0) tokens.onSuccessContainer else tokens.onInfoContainer,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CgaProgressBar(
                progress = masteryPercentage / 100f,
                label = "Concept Comprehension",
                showPercentage = false,
                progressColor = CGAColors.Primary
            )
        }
    }
}

/**
 * Material Types Section Component
 */
@Composable
private fun MaterialTypesSection(
    materialTypes: List<LearningMaterialType>,
    onSelectType: (LearningMaterialType) -> Unit,
    tokens: CGADesignTokens
) {
    Column {
        Text(
            text = "Supported Study Materials",
            color = tokens.textHeading,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Connect any material to generate source-grounded tutoring & assessments",
            color = tokens.textMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            materialTypes.forEach { type ->
                MaterialTypeCard(
                    type = type,
                    onClick = { onSelectType(type) },
                    tokens = tokens,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MaterialTypeCard(
    type: LearningMaterialType,
    onClick: () -> Unit,
    tokens: CGADesignTokens,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("material_type_${type.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = type.emoji,
                fontSize = 26.sp
            )

            Column {
                Text(
                    text = type.displayName,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = type.supportedFormats,
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * My Learning Materials Section
 */
@Composable
private fun MyLearningMaterialsSection(
    materials: List<LearningMaterial>,
    statsMap: Map<String, KnowledgeBaseStats>,
    onMaterialClick: (LearningMaterial) -> Unit,
    onOpenKbClick: (LearningMaterial) -> Unit,
    onDeleteClick: (LearningMaterial) -> Unit,
    onAddMoreClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Learning Materials",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )

            Text(
                text = "+ Upload PDF",
                color = CGAColors.Primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onAddMoreClick() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            materials.forEach { material ->
                val stats = statsMap[material.id]
                LearningMaterialCard(
                    material = material,
                    stats = stats,
                    onClick = { onMaterialClick(material) },
                    onKbClick = { onOpenKbClick(material) },
                    onDeleteClick = { onDeleteClick(material) },
                    tokens = tokens
                )
            }
        }
    }
}

@Composable
private fun LearningMaterialCard(
    material: LearningMaterial,
    stats: KnowledgeBaseStats?,
    onClick: () -> Unit,
    onKbClick: () -> Unit,
    onDeleteClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag("material_card_${material.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.contentBackground)
                        .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = material.type.emoji,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.name,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val unitCountLabel = when (material.type) {
                        LearningMaterialType.SLIDES -> "${material.pageCount} slides"
                        LearningMaterialType.LECTURE_VIDEO -> "${material.pageCount} transcript segments"
                        else -> "${material.pageCount} pages"
                    }
                    Text(
                        text = "${material.type.displayName.substringBefore("/")} • $unitCountLabel",
                        color = tokens.textMuted,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                when (material.status) {
                    LearningMaterialStatus.PROCESSED -> {
                        TagPill(
                            text = "Processed",
                            backgroundColor = tokens.successContainer,
                            textColor = tokens.onSuccessContainer
                        )
                    }
                    LearningMaterialStatus.PROCESSING -> {
                        TagPill(
                            text = "Processing...",
                            backgroundColor = tokens.warningContainer,
                            textColor = tokens.onWarningContainer
                        )
                    }
                    LearningMaterialStatus.FAILED -> {
                        TagPill(
                            text = "Failed",
                            backgroundColor = Color(0xFFFEE2E2),
                            textColor = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_material_button_${material.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete learning material",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Knowledge Base stats bar
            if (stats != null && stats.isReady) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.contentBackground)
                        .clickable { onKbClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CGAColors.ElectricPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Knowledge Base: ${stats.totalTopics} Topics • ${stats.totalConcepts} Concepts • ${stats.totalChunks} Chunks",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Knowledge Base",
                        tint = tokens.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Feature Cards Grid Component
 */
@Composable
private fun FeatureCardsSection(
    features: List<LearningCompanionFeatureType>,
    onFeatureClick: (LearningCompanionFeatureType) -> Unit,
    tokens: CGADesignTokens
) {
    Column {
        Text(
            text = "Companion Capabilities",
            color = tokens.textHeading,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            features.forEach { feature ->
                LearningFeatureCard(
                    feature = feature,
                    onClick = { onFeatureClick(feature) },
                    tokens = tokens
                )
            }
        }
    }
}

@Composable
private fun LearningFeatureCard(
    feature: LearningCompanionFeatureType,
    onClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surface)
            .border(2.dp, tokens.borderMain, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("feature_card_${feature.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.contentBackground)
                    .border(1.dp, tokens.borderMain, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = feature.emoji,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feature.title,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )

                    TagPill(
                        text = feature.badge,
                        backgroundColor = tokens.infoContainer,
                        textColor = tokens.onInfoContainer
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = feature.description,
                    color = tokens.textBody,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Continue Learning Section (Empty State)
 */
@Composable
private fun ContinueLearningSection(
    onUploadClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Column {
        Text(
            text = "Continue Learning",
            color = tokens.textHeading,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.surface)
                .border(2.dp, tokens.borderMain, RoundedCornerShape(20.dp))
                .padding(24.dp)
                .testTag("continue_learning_empty_card"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(tokens.contentBackground)
                        .border(1.dp, tokens.borderMain, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = tokens.textMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Upload your first learning material to start.",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Connect lecture slides, textbook PDFs, or video recordings to generate concept notes, smart flashcards, and adaptive mastery questions.",
                    color = tokens.textMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                CgaSecondaryButton(
                    text = "Browse PDF Textbook",
                    onClick = onUploadClick,
                    leadingIcon = Icons.Default.UploadFile,
                    modifier = Modifier.fillMaxWidth(0.75f),
                    testTag = "browse_study_files_button"
                )
            }
        }
    }
}

/**
 * Add Material Dialog Modal
 */
@Composable
private fun AddMaterialDialog(
    selectedType: LearningMaterialType,
    availableTypes: List<LearningMaterialType>,
    onSelectType: (LearningMaterialType) -> Unit,
    onConfirmAction: (LearningMaterialType) -> Unit,
    onDismiss: () -> Unit,
    tokens: CGADesignTokens
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Add Learning Material",
                    color = tokens.textHeading,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tokens.textMuted
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select the study material format you want to connect:",
                    color = tokens.textBody,
                    fontSize = 13.sp
                )

                availableTypes.forEach { type ->
                    val isSelected = type == selectedType
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) CGAColors.Primary.copy(alpha = 0.08f) else tokens.contentBackground)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) CGAColors.Primary else tokens.borderMain,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectType(type) }
                            .padding(14.dp)
                            .testTag("dialog_material_type_${type.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = type.emoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.displayName,
                                    color = tokens.textHeading,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = type.description,
                                    color = tokens.textMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val buttonText = when (selectedType) {
                LearningMaterialType.TEXTBOOK_PDF -> "Select PDF Document"
                LearningMaterialType.SLIDES -> "Select PowerPoint (.pptx)"
                LearningMaterialType.LECTURE_VIDEO -> "Select Lecture Video (.mp4)"
                else -> "Choose File"
            }
            CgaPrimaryButton(
                text = buttonText,
                onClick = { onConfirmAction(selectedType) },
                leadingIcon = Icons.Default.UploadFile,
                modifier = Modifier.fillMaxWidth(),
                testTag = "dialog_confirm_upload_button"
            )
        },
        dismissButton = null
    )
}

/**
 * Document Details Modal
 */
@Composable
private fun DocumentDetailsDialog(
    material: LearningMaterial,
    kbStats: KnowledgeBaseStats?,
    onBuildKnowledgeBase: () -> Unit,
    onOpenKnowledgeBase: () -> Unit,
    onDismiss: () -> Unit,
    tokens: CGADesignTokens
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = material.name,
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${material.pageCount} pages • Status: ${material.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = tokens.textMuted,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tokens.textMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // Knowledge Base Action / Status Area
                if (kbStats != null && kbStats.isReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.contentBackground)
                            .border(1.5.dp, CGAColors.ElectricPurple, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = CGAColors.ElectricPurple,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Knowledge Base Ready",
                                        color = tokens.textHeading,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                TagPill(
                                    text = "${kbStats.totalChunks} Chunks",
                                    backgroundColor = tokens.infoContainer,
                                    textColor = tokens.onInfoContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${kbStats.totalTopics} Topics • ${kbStats.totalConcepts} Concepts indexed",
                                color = tokens.textMuted,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            CgaPrimaryButton(
                                text = "Open Knowledge Base",
                                onClick = onOpenKnowledgeBase,
                                leadingIcon = Icons.Default.MenuBook,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "open_kb_from_details_button"
                            )
                        }
                    }
                } else {
                    CgaPrimaryButton(
                        text = "Build Knowledge Base",
                        onClick = onBuildKnowledgeBase,
                        leadingIcon = Icons.Default.Psychology,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "build_kb_button"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val unitHeaderLabel = when (material.type) {
                    LearningMaterialType.SLIDES -> "Slide-Aware Content Extracted (${material.contentUnits.size} slides):"
                    LearningMaterialType.LECTURE_VIDEO -> "Timestamped Transcript Segments (${material.contentUnits.size} segments):"
                    else -> "Page-Aware Content Extracted (${material.contentUnits.size} pages):"
                }
                Text(
                    text = unitHeaderLabel,
                    color = tokens.textHeading,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (material.contentUnits.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No extracted text pages available.",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(material.contentUnits) { unit ->
                            PageContentUnitPreviewCard(unit = unit, tokens = tokens)
                        }
                    }
                }
            }
        },
        confirmButton = {
            CgaSecondaryButton(
                text = "Close",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "dialog_close_details_button"
            )
        },
        dismissButton = null
    )
}

@Composable
private fun PageContentUnitPreviewCard(
    unit: PdfContentUnit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.contentBackground)
            .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("page_preview_${unit.pageNumber}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unit.unitLabel + (unit.slideTitle?.let { ": $it" } ?: ""),
                    color = CGAColors.Primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${unit.text.length} chars",
                    color = tokens.textMuted,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = unit.previewText,
                color = tokens.textBody,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Knowledge Base Viewer Dialog
 */
@Composable
private fun KnowledgeBaseViewerDialog(
    material: LearningMaterial,
    stats: KnowledgeBaseStats?,
    topics: List<String>,
    concepts: List<String>,
    chunks: List<KnowledgeChunk>,
    selectedTopic: String?,
    searchQuery: String,
    searchResults: List<KnowledgeChunk>,
    retrievalResults: List<com.example.domain.model.RetrievalResult> = emptyList(),
    onSelectTopic: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onChunkClick: (KnowledgeChunk) -> Unit,
    onDismiss: () -> Unit,
    tokens: CGADesignTokens
) {
    val focusManager = LocalFocusManager.current
    val displayedChunks = when {
        searchQuery.isNotBlank() -> searchResults
        selectedTopic != null -> chunks.filter { it.topic.equals(selectedTopic, ignoreCase = true) }
        else -> chunks
    }
    val scoreMap = retrievalResults.associate { it.chunk.id to it.relevanceScore }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CGAColors.LightPurpleIconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CGAColors.ElectricPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Knowledge Base",
                            color = tokens.textHeading,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = material.name,
                            color = tokens.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tokens.textMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Summary Stats Strip
                if (stats != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(tokens.contentBackground)
                            .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stats.totalTopics} Topics",
                            color = CGAColors.ElectricPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "•",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${stats.totalConcepts} Concepts",
                            color = CGAColors.Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "•",
                            color = tokens.textMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "${stats.totalChunks} Chunks",
                            color = CGAColors.GamificationOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Deterministic Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = {
                        Text(
                            text = "Search concepts or keywords (e.g. 'variable')...",
                            fontSize = 12.sp,
                            color = tokens.textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = tokens.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = tokens.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CGAColors.Primary,
                        unfocusedBorderColor = tokens.borderMain,
                        focusedContainerColor = tokens.contentBackground,
                        unfocusedContainerColor = tokens.contentBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kb_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Topic Filter Pills (only when not searching)
                if (searchQuery.isBlank() && topics.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isAllSelected = selectedTopic == null
                        TopicFilterPill(
                            text = "All (${chunks.size})",
                            isSelected = isAllSelected,
                            onClick = { onSelectTopic(null) },
                            tokens = tokens
                        )

                        topics.forEach { topic ->
                            val isSelected = topic.equals(selectedTopic, ignoreCase = true)
                            val topicCount = chunks.count { it.topic.equals(topic, ignoreCase = true) }
                            TopicFilterPill(
                                text = "$topic ($topicCount)",
                                isSelected = isSelected,
                                onClick = { onSelectTopic(topic) },
                                tokens = tokens
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Chunks List Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Search Results (${displayedChunks.size})" else "Knowledge Chunks (${displayedChunks.size})",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Tap for Source Preview",
                        color = tokens.textMuted,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Chunks List
                if (displayedChunks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching chunks found for '$searchQuery'" else "No chunks available in this topic.",
                            color = tokens.textMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(displayedChunks) { chunk ->
                            KnowledgeChunkCard(
                                chunk = chunk,
                                onClick = { onChunkClick(chunk) },
                                tokens = tokens,
                                relevanceScore = scoreMap[chunk.id]
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            CgaSecondaryButton(
                text = "Close Knowledge Base",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "close_kb_viewer_button"
            )
        },
        dismissButton = null
    )
}

@Composable
private fun TopicFilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tokens: CGADesignTokens
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CGAColors.Primary else tokens.contentBackground)
            .border(
                width = 1.dp,
                color = if (isSelected) CGAColors.Primary else tokens.borderMain,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else tokens.textHeading,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun KnowledgeChunkCard(
    chunk: KnowledgeChunk,
    onClick: () -> Unit,
    tokens: CGADesignTokens,
    relevanceScore: Int? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.contentBackground)
            .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("chunk_card_${chunk.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagPill(
                        text = chunk.topic,
                        backgroundColor = CGAColors.LightPurpleIconBg,
                        textColor = CGAColors.ElectricPurple
                    )
                    TagPill(
                        text = chunk.concept,
                        backgroundColor = tokens.infoContainer,
                        textColor = tokens.onInfoContainer
                    )
                    if (relevanceScore != null && relevanceScore > 0) {
                        TagPill(
                            text = "Score: $relevanceScore",
                            backgroundColor = CGAColors.SuccessGreen.copy(alpha = 0.12f),
                            textColor = CGAColors.SuccessGreen
                        )
                    }
                }

                Text(
                    text = chunk.unitLabel,
                    color = CGAColors.Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = chunk.previewText,
                color = tokens.textBody,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = tokens.textMuted,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "Source: ${chunk.sourceCitation}",
                    color = tokens.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 7. Source Reference Preview Dialog
 */
@Composable
private fun SourcePreviewDialog(
    chunk: KnowledgeChunk,
    onDismiss: () -> Unit,
    tokens: CGADesignTokens
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Source Citation Preview",
                        color = tokens.textHeading,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${chunk.documentName} • ${chunk.unitLabel}",
                        color = CGAColors.Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tokens.textMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TagPill(
                        text = "Topic: ${chunk.topic}",
                        backgroundColor = CGAColors.LightPurpleIconBg,
                        textColor = CGAColors.ElectricPurple
                    )
                    TagPill(
                        text = "Concept: ${chunk.concept}",
                        backgroundColor = tokens.infoContainer,
                        textColor = tokens.onInfoContainer
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.contentBackground)
                        .border(1.dp, tokens.borderMain, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = chunk.text,
                        color = tokens.textHeading,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            CgaPrimaryButton(
                text = "Close Source View",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "close_source_preview_button"
            )
        },
        dismissButton = null
    )
}

/**
 * Safe Delete Confirmation Dialog for Learning Materials
 */
@Composable
private fun DeleteMaterialConfirmationDialog(
    materialName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    tokens: CGADesignTokens
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = tokens.surface,
        title = {
            Text(
                text = "Delete learning material?",
                color = tokens.textHeading,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                modifier = Modifier.testTag("delete_material_dialog_title")
            )
        },
        text = {
            Column {
                Text(
                    text = "This will remove this learning material and its associated knowledge chunks from your Learning Companion. This action cannot be undone.",
                    color = tokens.textBody,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.testTag("delete_material_dialog_message")
                )
                if (materialName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Material: $materialName",
                        color = tokens.textMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("delete_material_dialog_confirm_button")
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DELETING...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("DELETE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier.testTag("delete_material_dialog_cancel_button")
            ) {
                Text(
                    text = "CANCEL",
                    color = tokens.textMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        modifier = Modifier.testTag("delete_material_confirmation_dialog")
    )
}

