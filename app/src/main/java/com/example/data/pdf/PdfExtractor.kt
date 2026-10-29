package com.example.data.pdf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.PdfContentUnit
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

interface PdfExtractor {
    suspend fun extractFromUri(uri: Uri, rawFileName: String? = null): Result<LearningMaterial>
    suspend fun extractFromStream(inputStream: InputStream, fileName: String, customId: String? = null): Result<LearningMaterial>
}

class PdfExtractorImpl(
    private val context: Context? = null
) : PdfExtractor {

    companion object {
        private const val TAG = "PdfExtractor"
        private var isLoaderInitialized = false

        fun ensureInitialized(context: Context) {
            if (!isLoaderInitialized) {
                try {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    isLoaderInitialized = true
                    Log.d(TAG, "PDFBoxResourceLoader successfully initialized")
                } catch (e: Exception) {
                    Log.w(TAG, "PDFBoxResourceLoader initialization warning: ${e.message}")
                }
            }
        }
    }

    init {
        context?.let { ensureInitialized(it) }
    }

    override suspend fun extractFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> = withContext(Dispatchers.IO) {
        val appContext = context ?: return@withContext Result.failure(
            IllegalStateException("Context is required to resolve PDF URI")
        )
        ensureInitialized(appContext)

        val resolvedFileName = rawFileName ?: resolveFileNameFromUri(appContext, uri) ?: "Textbook_${System.currentTimeMillis()}.pdf"
        val cleanTitle = sanitizeDocumentTitle(resolvedFileName)

        try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for selected PDF"))

            inputStream.use { stream ->
                extractFromStreamInternal(stream, resolvedFileName, cleanTitle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PDF from URI: $uri", e)
            Result.failure(e)
        }
    }

    override suspend fun extractFromStream(
        inputStream: InputStream,
        fileName: String,
        customId: String?
    ): Result<LearningMaterial> = withContext(Dispatchers.IO) {
        context?.let { ensureInitialized(it) }
        val cleanTitle = sanitizeDocumentTitle(fileName)
        try {
            extractFromStreamInternal(inputStream, fileName, cleanTitle, customId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PDF from stream: $fileName", e)
            Result.failure(e)
        }
    }

    private fun extractFromStreamInternal(
        inputStream: InputStream,
        sourceFileName: String,
        documentTitle: String,
        customId: String? = null
    ): Result<LearningMaterial> {
        var pdDocument: PDDocument? = null
        val materialId = customId ?: "mat_${UUID.randomUUID().toString().take(8)}"

        return try {
            pdDocument = PDDocument.load(inputStream)
            val totalPages = pdDocument.numberOfPages

            if (totalPages <= 0) {
                return Result.failure(IllegalStateException("The PDF contains no pages."))
            }

            val contentUnits = mutableListOf<PdfContentUnit>()
            val stripper = PDFTextStripper()

            for (page in 1..totalPages) {
                stripper.startPage = page
                stripper.endPage = page
                val extractedText = stripper.getText(pdDocument)?.trim().orEmpty()

                contentUnits.add(
                    PdfContentUnit(
                        id = "${materialId}_page_$page",
                        materialId = materialId,
                        documentName = documentTitle,
                        pageNumber = page,
                        text = extractedText
                    )
                )
            }

            val material = LearningMaterial(
                id = materialId,
                name = documentTitle,
                type = LearningMaterialType.TEXTBOOK_PDF,
                pageCount = totalPages,
                status = LearningMaterialStatus.PROCESSED,
                createdAt = System.currentTimeMillis(),
                sourceFileName = sourceFileName,
                contentUnits = contentUnits
            )

            Log.d(TAG, "Successfully extracted $totalPages pages for '$documentTitle'")
            Result.success(material)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PDF pages: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                pdDocument?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing PDDocument: ${e.message}")
            }
        }
    }

    private fun resolveFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not resolve file name from cursor: ${e.message}")
            }
        }
        return uri.lastPathSegment?.substringAfterLast("/")
    }

    private fun sanitizeDocumentTitle(fileName: String): String {
        return fileName.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifBlank { "Study Material" }
    }
}
