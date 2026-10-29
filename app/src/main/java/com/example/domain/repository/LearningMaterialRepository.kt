package com.example.domain.repository

import android.net.Uri
import com.example.domain.model.LearningMaterial
import com.example.domain.model.PdfContentUnit
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

/**
 * Repository interface for managing local and remote learning materials
 * and page-aware extracted units.
 */
interface LearningMaterialRepository {
    fun getMaterialsFlow(): Flow<List<LearningMaterial>>
    suspend fun getMaterialById(id: String): LearningMaterial?
    suspend fun getContentUnitsForMaterial(materialId: String): List<PdfContentUnit>
    suspend fun ingestPdfFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial>
    suspend fun ingestPdfFromStream(inputStream: InputStream, fileName: String): Result<LearningMaterial>
    suspend fun ingestPptxFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> =
        Result.failure(UnsupportedOperationException("PPTX ingestion not supported by this repository"))
    suspend fun ingestPptxFromStream(inputStream: InputStream, fileName: String): Result<LearningMaterial> =
        Result.failure(UnsupportedOperationException("PPTX ingestion not supported by this repository"))
    suspend fun ingestVideoFromUri(
        uri: Uri,
        rawFileName: String?,
        importedTranscriptText: String? = null
    ): Result<LearningMaterial> =
        Result.failure(UnsupportedOperationException("Video ingestion not supported by this repository"))
    suspend fun ingestVideoFromStream(
        inputStream: InputStream,
        fileName: String,
        importedTranscriptText: String? = null
    ): Result<LearningMaterial> =
        Result.failure(UnsupportedOperationException("Video ingestion not supported by this repository"))
    suspend fun deleteMaterial(id: String): Result<Unit>
}
