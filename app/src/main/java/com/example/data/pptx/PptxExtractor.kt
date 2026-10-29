package com.example.data.pptx

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.domain.model.LearningMaterial
import com.example.domain.model.LearningMaterialStatus
import com.example.domain.model.LearningMaterialType
import com.example.domain.model.MaterialSourceType
import com.example.domain.model.PdfContentUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.StringReader
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Interface for extracting structured slide text units from PowerPoint (.pptx) presentations.
 * Multimodal AI Hackathon 2026 — Track D: Personalized Tutoring & Adaptive Learning.
 */
interface PptxExtractor {
    suspend fun extractFromUri(uri: Uri, rawFileName: String? = null): Result<LearningMaterial>
    suspend fun extractFromStream(inputStream: InputStream, fileName: String, customId: String? = null): Result<LearningMaterial>
}

/**
 * Zero-overhead, pure Kotlin / Android-native OOXML presentation parser.
 *
 * Why native XML & Zip parsing over Apache POI:
 * 1. Zero bloat: Apache POI adds ~35MB+ of jar dependencies and multi-second dex/compilation overhead.
 * 2. High stability: Avoids Android classloader incompatibilities, Dalvik bytecode issues, and AWT missing classes.
 * 3. Exact OOXML compliance: Reads ppt/slides/slide{N}.xml zip streams directly, extracting slide titles,
 *    shapes (<p:sp>), text bodies (<p:txBody>), paragraphs (<a:p>), and text runs (<a:r>/<a:t>) while strictly
 *    preserving slide numbers starting at 1.
 */
class PptxExtractorImpl(
    private val context: Context? = null
) : PptxExtractor {

    companion object {
        private const val TAG = "PptxExtractor"
        private const val MAX_PPTX_BYTE_SIZE = 50 * 1024 * 1024 // 50MB safety limit
    }

    override suspend fun extractFromUri(uri: Uri, rawFileName: String?): Result<LearningMaterial> = withContext(Dispatchers.IO) {
        val appContext = context ?: return@withContext Result.failure(
            IllegalStateException("Context is required to resolve PPTX URI")
        )

        val resolvedFileName = rawFileName ?: resolveFileNameFromUri(appContext, uri) ?: "Presentation_${System.currentTimeMillis()}.pptx"
        val cleanTitle = sanitizePresentationTitle(resolvedFileName)

        try {
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for selected PPTX file"))

            inputStream.use { stream ->
                extractFromStreamInternal(stream, resolvedFileName, cleanTitle)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PPTX from URI: $uri", e)
            Result.failure(e)
        }
    }

    override suspend fun extractFromStream(
        inputStream: InputStream,
        fileName: String,
        customId: String?
    ): Result<LearningMaterial> = withContext(Dispatchers.IO) {
        val cleanTitle = sanitizePresentationTitle(fileName)
        try {
            extractFromStreamInternal(inputStream, fileName, cleanTitle, customId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract PPTX from stream: $fileName", e)
            Result.failure(e)
        }
    }

    private fun extractFromStreamInternal(
        inputStream: InputStream,
        sourceFileName: String,
        presentationTitle: String,
        customId: String? = null
    ): Result<LearningMaterial> {
        val materialId = customId ?: "mat_${UUID.randomUUID().toString().take(8)}"

        // Map of Slide Number (1-indexed) -> ExtractedSlideData
        val slideEntries = mutableMapOf<Int, String>()
        var foundSlideFiles = false

        try {
            ZipInputStream(inputStream).use { zipStream ->
                var entry: ZipEntry? = zipStream.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    // PPTX slide XMLs are named: ppt/slides/slide{N}.xml
                    if (entryName.startsWith("ppt/slides/slide") && entryName.endsWith(".xml")) {
                        val slideNumberStr = entryName
                            .removePrefix("ppt/slides/slide")
                            .removeSuffix(".xml")

                        val slideNumber = slideNumberStr.toIntOrNull()
                        if (slideNumber != null && slideNumber > 0) {
                            foundSlideFiles = true
                            val slideXmlBytes = zipStream.readBytes()
                            val slideXmlString = String(slideXmlBytes, Charsets.UTF_8)
                            slideEntries[slideNumber] = slideXmlString
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Corrupted PPTX file or extraction failure: ${e.message}", e)
            return Result.failure(IllegalStateException("Invalid or corrupted PowerPoint file. Please ensure it is a valid .pptx presentation."))
        }

        if (!foundSlideFiles || slideEntries.isEmpty()) {
            return Result.failure(IllegalStateException("No slides found in the presentation. The .pptx file may be corrupted or empty."))
        }

        val sortedSlideNumbers = slideEntries.keys.sorted()
        val totalSlides = sortedSlideNumbers.maxOrNull() ?: slideEntries.size

        val contentUnits = mutableListOf<PdfContentUnit>()
        val xmlFactory = XmlPullParserFactory.newInstance()
        xmlFactory.isNamespaceAware = true

        for (slideNum in sortedSlideNumbers) {
            val xmlContent = slideEntries[slideNum] ?: continue
            val parsedSlide = parseSlideXml(xmlContent, xmlFactory)

            val slideText = parsedSlide.fullText.trim()
            val slideTitle = parsedSlide.title?.trim()

            contentUnits.add(
                PdfContentUnit(
                    id = "${materialId}_slide_$slideNum",
                    materialId = materialId,
                    documentName = presentationTitle,
                    pageNumber = slideNum,
                    text = slideText,
                    sourceType = MaterialSourceType.PPTX,
                    slideTitle = slideTitle
                )
            )
        }

        val material = LearningMaterial(
            id = materialId,
            name = presentationTitle,
            type = LearningMaterialType.SLIDES,
            pageCount = totalSlides,
            status = LearningMaterialStatus.PROCESSED,
            createdAt = System.currentTimeMillis(),
            sourceFileName = sourceFileName,
            contentUnits = contentUnits,
            sourceType = MaterialSourceType.PPTX
        )

        Log.d(TAG, "Successfully extracted ${contentUnits.size} slides for presentation '$presentationTitle'")
        return Result.success(material)
    }

    private data class ParsedSlide(
        val title: String?,
        val fullText: String
    )

    /**
     * Parses an individual ppt/slides/slide{N}.xml using Android XmlPullParser.
     * Extracts text from <a:t> nodes within <a:p> paragraph boundaries and shapes.
     */
    private fun parseSlideXml(xmlContent: String, factory: XmlPullParserFactory): ParsedSlide {
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        val paragraphs = mutableListOf<String>()
        val currentParagraphRuns = StringBuilder()
        var currentShapeText = StringBuilder()
        var slideTitle: String? = null
        var isFirstParagraph = true

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val localName = parser.name
                    if (localName.equals("p", ignoreCase = true)) {
                        // Start of paragraph
                        currentParagraphRuns.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> {
                    // XmlPullParser text event (inside <a:t>)
                    val text = parser.text
                    if (!text.isNullOrBlank()) {
                        currentParagraphRuns.append(text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val localName = parser.name
                    if (localName.equals("p", ignoreCase = true)) {
                        // End of paragraph
                        val pText = currentParagraphRuns.toString().trim()
                        if (pText.isNotBlank()) {
                            paragraphs.add(pText)
                            if (slideTitle == null && isFirstParagraph) {
                                slideTitle = pText.take(80)
                            }
                            isFirstParagraph = false
                        }
                        currentParagraphRuns.setLength(0)
                    }
                }
            }
            eventType = parser.next()
        }

        val fullText = paragraphs.joinToString("\n\n")
        return ParsedSlide(
            title = slideTitle,
            fullText = fullText
        )
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

    private fun sanitizePresentationTitle(fileName: String): String {
        return fileName.substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifBlank { "Lecture Presentation" }
    }
}
