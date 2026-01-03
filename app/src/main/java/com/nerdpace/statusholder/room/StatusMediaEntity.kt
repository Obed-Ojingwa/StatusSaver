package com.nerdpace.statusholder.room


// Room Entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_media")
data class StatusMediaEntity(
    @PrimaryKey
    val id: String,
    val originalUri: String, // Original WhatsApp status URI
    val cachedFilePath: String?, // Path to cached file in app-private storage
    val mediaType: String, // "PHOTO" or "VIDEO"
    val source: String, // "NORMAL_WHATSAPP" or "WHATSAPP_BUSINESS"
    val timestamp: Long,
    val size: Long,
    val displayName: String,
    val mimeType: String,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 7 days
)