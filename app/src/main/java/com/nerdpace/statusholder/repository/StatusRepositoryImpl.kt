package com.nerdpace.statusholder.repository


// Repository Implementation

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import com.whatsappstatussaver.data.local.dao.StatusMediaDao
import com.whatsappstatussaver.data.local.entity.StatusMediaEntity
import com.whatsappstatussaver.domain.model.MediaType
import com.whatsappstatussaver.domain.model.StatusMedia
import com.whatsappstatussaver.domain.model.WhatsAppSource
import com.whatsappstatussaver.domain.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class StatusRepositoryImpl(
    private val context: Context,
    private val dao: StatusMediaDao
) : StatusRepository {

    private val cacheDir = File(context.cacheDir, "status_cache").apply { mkdirs() }

    override suspend fun scanAndCacheStatuses(source: WhatsAppSource): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val statusUri = getWhatsAppStatusUri(source)
            val mediaList = mutableListOf<StatusMediaEntity>()

            // Use DocumentsContract to access SAF directory
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                statusUri,
                DocumentsContract.getTreeDocumentId(statusUri)
            )

            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn)
                    val displayName = cursor.getString(nameColumn)
                    val mimeType = cursor.getString(mimeColumn) ?: continue
                    val size = cursor.getLong(sizeColumn)
                    val modified = cursor.getLong(modifiedColumn)

                    // Skip .nomedia files
                    if (displayName == ".nomedia") continue

                    // Determine media type
                    val mediaType = when {
                        mimeType.startsWith("image/") -> MediaType.PHOTO
                        mimeType.startsWith("video/") -> MediaType.VIDEO
                        else -> continue
                    }

                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(statusUri, documentId)

                    // Cache the file to app-private storage
                    val cachedFile = cacheMediaFile(documentUri, displayName, source)

                    val mediaId = UUID.nameUUIDFromBytes(
                        "${source.name}_${displayName}_$modified".toByteArray()
                    ).toString()

                    val entity = StatusMediaEntity(
                        id = mediaId,
                        originalUri = documentUri.toString(),
                        cachedFilePath = cachedFile?.absolutePath,
                        mediaType = mediaType.name,
                        source = source.name,
                        timestamp = modified,
                        size = size,
                        displayName = displayName,
                        mimeType = mimeType
                    )

                    mediaList.add(entity)
                }
            }

            // Insert all discovered media into database
            if (mediaList.isNotEmpty()) {
                dao.insertAllMedia(mediaList)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cacheMediaFile(sourceUri: Uri, displayName: String, source: WhatsAppSource): File? {
        return try {
            val sourceDir = File(cacheDir, source.name.lowercase())
            sourceDir.mkdirs()

            val targetFile = File(sourceDir, displayName)

            // Don't re-cache if file already exists and is recent
            if (targetFile.exists() && System.currentTimeMillis() - targetFile.lastModified() < 60000) {
                return targetFile
            }

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            targetFile
        } catch (e: Exception) {
            null
        }
    }

    override fun getStatusMedia(source: WhatsAppSource, type: MediaType?): Flow<List<StatusMedia>> {
        return if (type != null) {
            dao.getMediaBySourceAndType(source.name, type.name)
        } else {
            dao.getMediaBySource(source.name)
        }.map { entities ->
            entities.mapNotNull { it.toDomainModel() }
        }
    }

    override suspend fun getMediaById(id: String): StatusMedia? = withContext(Dispatchers.IO) {
        dao.getMediaById(id)?.toDomainModel()
    }

    override suspend fun saveToDevice(mediaId: String): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val entity = dao.getMediaById(mediaId) ?: return@withContext Result.failure(
                Exception("Media not found")
            )

            val cachedFile = entity.cachedFilePath?.let { File(it) }
                ?: return@withContext Result.failure(Exception("Cached file not found"))

            if (!cachedFile.exists()) {
                return@withContext Result.failure(Exception("Cached file does not exist"))
            }

            // Determine collection based on media type
            val collection = when (entity.mediaType) {
                "PHOTO" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> return@withContext Result.failure(Exception("Unknown media type"))
            }

            // Prepare content values
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, entity.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/WhatsApp Status")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            // Insert into MediaStore
            val uri = context.contentResolver.insert(collection, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

            // Write file content
            context.contentResolver.openOutputStream(uri)?.use { output ->
                cachedFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            // Mark as not pending
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)

            // Update database to mark as saved
            dao.updateSaveStatus(mediaId, true)

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cleanupExpiredMedia(): Int = withContext(Dispatchers.IO) {
        // Get expired media
        val expiredMedia = dao.getExpiredMedia()

        // Delete cached files
        expiredMedia.forEach { entity ->
            entity.cachedFilePath?.let { path ->
                File(path).delete()
            }
        }

        // Delete from database
        dao.deleteExpiredMedia()
    }

    override fun getWhatsAppStatusUri(source: WhatsAppSource): Uri {
        // This returns a tree URI that user must grant access to via SAF
        val path = when (source) {
            WhatsAppSource.NORMAL_WHATSAPP ->
                "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
            WhatsAppSource.WHATSAPP_BUSINESS ->
                "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses"
        }
        return Uri.parse(path)
    }

    private fun StatusMediaEntity.toDomainModel(): StatusMedia? {
        val cachedUri = cachedFilePath?.let { File(it).toUri() }

        return StatusMedia(
            id = id,
            uri = originalUri.toUri(),
            cachedUri = cachedUri,
            mediaType = MediaType.valueOf(mediaType),
            source = WhatsAppSource.valueOf(source),
            timestamp = timestamp,
            size = size,
            displayName = displayName,
            mimeType = mimeType,
            isSaved = isSaved,
            expiresAt = expiresAt
        )
    }
}