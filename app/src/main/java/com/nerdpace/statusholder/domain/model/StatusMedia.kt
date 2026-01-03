package com.nerdpace.statusholder.domain.model

import android.net.Uri

data class StatusMedia(
    val id: String,
    val uri: Uri,
    val cachedUri: Uri?, // Local cached copy in app-private storage
    val mediaType: MediaType,
    val source: WhatsAppSource,
    val timestamp: Long,
    val size: Long,
    val displayName: String,
    val mimeType: String,
    val isSaved: Boolean = false,
    val expiresAt: Long // Timestamp when this will be auto-deleted
)