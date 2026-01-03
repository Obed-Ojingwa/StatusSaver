package com.nerdpace.statusholder.repository


import android.net.Uri
import com.nerdpace.statusholder.domain.model.MediaType
import com.nerdpace.statusholder.domain.model.StatusMedia
import com.nerdpace.statusholder.domain.model.WhatsAppSource
import com.whatsappstatussaver.domain.model.StatusMedia
import com.whatsappstatussaver.domain.model.WhatsAppSource
import kotlinx.coroutines.flow.Flow

interface StatusRepository {

    /**
     * Scan and discover status media from WhatsApp directories
     * This will cache media to app-private storage and insert into database
     */
    suspend fun scanAndCacheStatuses(source: WhatsAppSource): Result<Unit>

    /**
     * Get cached status media by source and type
     */
    fun getStatusMedia(source: WhatsAppSource, type: MediaType? = null): Flow<List<StatusMedia>>

    /**
     * Save a status media to user's device storage (Downloads/Pictures)
     */
    suspend fun saveToDevice(mediaId: String): Result<Uri>

    /**
     * Get single media by ID
     */
    suspend fun getMediaById(id: String): StatusMedia?

    /**
     * Clean up expired cached media (older than 7 days and not saved)
     */
    suspend fun cleanupExpiredMedia(): Int

    /**
     * Request SAF access to WhatsApp status directory
     */
    fun getWhatsAppStatusUri(source: WhatsAppSource): Uri
}