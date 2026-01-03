package com.nerdpace.statusholder.room

// Room DAO

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusMediaDao {

    @Query("SELECT * FROM status_media WHERE source = :source ORDER BY timestamp DESC")
    fun getMediaBySource(source: String): Flow<List<StatusMediaEntity>>

    @Query("SELECT * FROM status_media WHERE source = :source AND mediaType = :type ORDER BY timestamp DESC")
    fun getMediaBySourceAndType(source: String, type: String): Flow<List<StatusMediaEntity>>

    @Query("SELECT * FROM status_media WHERE id = :id")
    suspend fun getMediaById(id: String): StatusMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: StatusMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMedia(mediaList: List<StatusMediaEntity>)

    @Update
    suspend fun updateMedia(media: StatusMediaEntity)

    @Query("UPDATE status_media SET isSaved = :isSaved WHERE id = :id")
    suspend fun updateSaveStatus(id: String, isSaved: Boolean)

    @Query("DELETE FROM status_media WHERE id = :id")
    suspend fun deleteMedia(id: String)

    @Query("DELETE FROM status_media WHERE expiresAt < :currentTime AND isSaved = 0")
    suspend fun deleteExpiredMedia(currentTime: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM status_media WHERE expiresAt < :currentTime AND isSaved = 0")
    suspend fun getExpiredMedia(currentTime: Long = System.currentTimeMillis()): List<StatusMediaEntity>

    @Query("DELETE FROM status_media")
    suspend fun clearAll()
}