package com.example.data.repository

import android.net.Uri
import com.example.data.pdf.PdfExtractor
import com.example.data.pptx.PptxExtractor
import com.example.data.pptx.PptxExtractorImpl
import com.example.data.video.VideoExtractor
import com.example.data.video.VideoExtractorImpl
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.PdfContentUnit
import com.example.domain.repository.LearningMaterialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.UUID

class LocalLearningMaterialRepository(
    private val pdfExtractor: PdfExtractor,
    private val pptxExtractor: PptxExtractor = PptxExtractorImpl(),
    private val videoExtractor: VideoExtractor = VideoExtractorImpl(),
    private val storageDir: File? = null
) : LearningMaterialRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _materials = MutableStateFlow<List<LearningMaterial>>(emptyList())

    init {
        loadFromDisk()
    }

    private fun getStorageFile(): File? {
        val dir = storageDir ?: return null
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "cga_learning_materials.json")
    }

    private fun loadFromDisk() {
        val file = getStorageFile() ?: return
        if (!file.exists()) return
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val loaded = json.decodeFromString<List<LearningMaterial>>(content)
                // Only restore PROCESSED materials
                _materials.value = loaded.filter { it.status == LearningMaterialStatus.PROCESSED }
            }
        } catch (_: Exception) {
            // Corrupted or incompatible file; start empty safely
        }
    }

    private fun saveToDisk() {
        val file = getStorageFile() ?: return
        try {
            val processedOnly = _materials.value.filter { it.status == LearningMaterialStatus.PROCESSED }
            file.writeText(json.encodeToString(processedOnly))
        } catch (_: Exception) {
            // Non-fatal IO error
        }
    }

    override fun getMaterialsFlow(): Flow<List<LearningMaterial>> = _materials.asStateFlow()

    override suspend fun getMaterialById(id: String): LearningMaterial? {
        return _materials.value.firstOrNull { it.id == id }
    }

    override suspend fun getContentUnitsForMaterial(materialId: String): List<PdfContentUnit> {
        return _materials.value.firstOrNull { it.id == materialId }?.contentUnits ?: emptyList()
    }

    override suspend fun ingestPdfFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> {
        val tempId = "mat_${UUID.randomUUID().toString().take(8)}"
        val initialName = rawFileName?.substringBeforeLast(".") ?: "Textbook Document"

        // Place a temporary PROCESSING item
        val processingItem = LearningMaterial(
            id = tempId,
            name = initialName,
            status = LearningMaterialStatus.PROCESSING,
            createdAt = System.currentTimeMillis(),
            sourceFileName = rawFileName ?: "document.pdf",
            type = LearningMaterialType.TEXTBOOK_PDF,
            sourceType = MaterialSourceType.PDF
        )
        _materials.update { current -> listOf(processingItem) + current.filter { it.id != tempId } }

        val extractionResult = pdfExtractor.extractFromUri(uri, rawFileName)

        return if (extractionResult.isSuccess) {
            val processed = extractionResult.getOrThrow()
            _materials.update { current ->
                current.map { if (it.id == tempId) processed else it }
            }
            saveToDisk()
            Result.success(processed)
        } else {
            val failure = extractionResult.exceptionOrNull()
            val failedItem = processingItem.copy(
                status = LearningMaterialStatus.FAILED,
                errorMessage = failure?.message ?: "Could not process this PDF. Please try another file."
            )
            _materials.update { current ->
                current.map { if (it.id == tempId) failedItem else it }
            }
            Result.failure(failure ?: IllegalStateException("Failed to extract PDF"))
        }
    }

    override suspend fun ingestPdfFromStream(
        inputStream: InputStream,
        fileName: String
    ): Result<LearningMaterial> {
        val tempId = "mat_${UUID.randomUUID().toString().take(8)}"
        val processingItem = LearningMaterial(
            id = tempId,
            name = fileName.substringBeforeLast("."),
            status = LearningMaterialStatus.PROCESSING,
            createdAt = System.currentTimeMillis(),
            sourceFileName = fileName,
            type = LearningMaterialType.TEXTBOOK_PDF,
            sourceType = MaterialSourceType.PDF
        )
        _materials.update { current -> listOf(processingItem) + current.filter { it.id != tempId } }

        val extractionResult = pdfExtractor.extractFromStream(inputStream, fileName, tempId)

        return if (extractionResult.isSuccess) {
            val processed = extractionResult.getOrThrow()
            _materials.update { current ->
                current.map { if (it.id == tempId) processed else it }
            }
            saveToDisk()
            Result.success(processed)
        } else {
            val failure = extractionResult.exceptionOrNull()
            val failedItem = processingItem.copy(
                status = LearningMaterialStatus.FAILED,
                errorMessage = failure?.message ?: "Could not process this PDF. Please try another file."
            )
            _materials.update { current ->
                current.map { if (it.id == tempId) failedItem else it }
            }
            Result.failure(failure ?: IllegalStateException("Failed to extract PDF"))
        }
    }

    override suspend fun ingestPptxFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> {
        val tempId = "mat_${UUID.randomUUID().toString().take(8)}"
        val initialName = rawFileName?.substringBeforeLast(".") ?: "Presentation Slides"

        // Place a temporary PROCESSING item
        val processingItem = LearningMaterial(
            id = tempId,
            name = initialName,
            status = LearningMaterialStatus.PROCESSING,
            createdAt = System.currentTimeMillis(),
            sourceFileName = rawFileName ?: "presentation.pptx",
            type = LearningMaterialType.SLIDES,
            sourceType = MaterialSourceType.PPTX
        )
        _materials.update { current -> listOf(processingItem) + current.filter { it.id != tempId } }

        val extractionResult = pptxExtractor.extractFromUri(uri, rawFileName)

        return if (extractionResult.isSuccess) {
            val processed = extractionResult.getOrThrow()
            _materials.update { current ->
                current.map { if (it.id == tempId) processed else it }
            }
            saveToDisk()
            Result.success(processed)
        } else {
            val failure = extractionResult.exceptionOrNull()
            val failedItem = processingItem.copy(
                status = LearningMaterialStatus.FAILED,
                errorMessage = failure?.message ?: "Could not process this PowerPoint presentation. Please try another file."
            )
            _materials.update { current ->
                current.map { if (it.id == tempId) failedItem else it }
            }
            Result.failure(failure ?: IllegalStateException("Failed to extract PPTX"))
        }
    }

    override suspend fun ingestPptxFromStream(
        inputStream: InputStream,
        fileName: String
    ): Result<LearningMaterial> {
        val tempId = "mat_${UUID.randomUUID().toString().take(8)}"
        val processingItem = LearningMaterial(
            id = tempId,
            name = fileName.substringBeforeLast("."),
            status = LearningMaterialStatus.PROCESSING,
            createdAt = System.currentTimeMillis(),
            sourceFileName = fileName,
            type = LearningMaterialType.SLIDES,
            sourceType = MaterialSourceType.PPTX
        )
        _materials.update { current -> listOf(processingItem) + current.filter { it.id != tempId } }

        val extractionResult = pptxExtractor.extractFromStream(inputStream, fileName, tempId)

        return if (extractionResult.isSuccess) {
            val processed = extractionResult.getOrThrow()
            _materials.update { current ->
                current.map { if (it.id == tempId) processed else it }
            }
            saveToDisk()
            Result.success(processed)
        } else {
            val failure = extractionResult.exceptionOrNull()
            val failedItem = processingItem.copy(
                status = LearningMaterialStatus.FAILED,
                errorMessage = failure?.message ?: "Could not process this PowerPoint presentation. Please try another file."
            )
            _materials.update { current ->
                current.map { if (it.id == tempId) failedItem else it }
            }
            Result.failure(failure ?: IllegalStateException("Failed to extract PPTX"))
        }
    }

    override suspend fun ingestVideoFromUri(
        uri: Uri,
        rawFileName: String?,
        importedTranscriptText: String?
    ): Result<LearningMaterial> {
        val tempId = "mat_${UUID.randomUUID().toString().take(8)}"
        val initialName = rawFileName?.substringBeforeLast(".") ?: "Lecture Recording"

        val processingItem = LearningMaterial(
            id = tempId,
            name = initialName,
            status = LearningMaterialStatus.PROCESSING,
            createdAt = System.currentTimeMillis(),
            sourceFileName = rawFileName ?: "lecture.mp4",
            type = LearningMaterialType.LECTURE_VIDEO,
            sourceType = MaterialSourceType.VIDEO
        )
        _materials.update { current -> listOf(processingItem) + current.filter { it.id != tempId } }

        val extractionResult = videoExtractor.extractFromUri(uri, rawFileName, importedTranscriptText)

        return if (extractionResult.isSuccess) {
            val processed = extractionResult.getOrThrow()
            _materials.update { current ->
                current.map { if (it.id == tempId) processed else it }
            }
            saveToDisk()
            Result.success(processed)
        } else {
            val failure = extractionResult.exceptionOrNull()
            val failedItem = processingItem.copy(
                status = LearningMaterialStatus.FAILED,
                errorMessage = failure?.message ?: "Could not process this lecture video. Please try another file."
            )
            _materials.update { current ->
                current.map { if (it.id == tempId) failedItem else it }
            }
            Result.failure(failure ?: IllegalStateException("Failed to extract video"))
        }
    }

    override suspend fun ingestVideoFromStream(
        inputStream: InputStream,
        fileName: String,
        importedTranscriptText: String?
    ): Result<LearningMaterial> {
        val tempId = "mat_${UUID.randomUUID().toString().take(8)}"
        val processingItem = LearningMaterial(
            id = tempId,
            name = fileName.substringBeforeLast("."),
            status = LearningMaterialStatus.PROCESSING,
            createdAt = System.currentTimeMillis(),
            sourceFileName = fileName,
            type = LearningMaterialType.LECTURE_VIDEO,
            sourceType = MaterialSourceType.VIDEO
        )
        _materials.update { current -> listOf(processingItem) + current.filter { it.id != tempId } }

        val extractionResult = videoExtractor.extractFromStream(inputStream, fileName, tempId, importedTranscriptText)

        return if (extractionResult.isSuccess) {
            val processed = extractionResult.getOrThrow()
            _materials.update { current ->
                current.map { if (it.id == tempId) processed else it }
            }
            saveToDisk()
            Result.success(processed)
        } else {
            val failure = extractionResult.exceptionOrNull()
            val failedItem = processingItem.copy(
                status = LearningMaterialStatus.FAILED,
                errorMessage = failure?.message ?: "Could not process this lecture video. Please try another file."
            )
            _materials.update { current ->
                current.map { if (it.id == tempId) failedItem else it }
            }
            Result.failure(failure ?: IllegalStateException("Failed to extract video"))
        }
    }

    override suspend fun deleteMaterial(id: String): Result<Unit> {
        _materials.update { current -> current.filter { it.id != id } }
        saveToDisk()
        return Result.success(Unit)
    }

    // Helper for testing
    fun addMaterialDirectly(material: LearningMaterial) {
        _materials.update { current -> listOf(material) + current.filter { it.id != material.id } }
        saveToDisk()
    }

    fun clearMaterials() {
        _materials.value = emptyList()
        saveToDisk()
    }
}
