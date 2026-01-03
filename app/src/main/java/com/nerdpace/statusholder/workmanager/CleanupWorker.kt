package com.nerdpace.statusholder.workmanager

class CleanupWorker {
}

// Cleanup Worker
package com.whatsappstatussaver.worker

import android.content.Context
import androidx.work.*
import com.whatsappstatussaver.domain.repository.StatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class CleanupWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: StatusRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            // Clean up expired media (older than 7 days and not saved)
            val deletedCount = repository.cleanupExpiredMedia()

            // Log results
            val outputData = workDataOf(
                KEY_DELETED_COUNT to deletedCount,
                KEY_TIMESTAMP to System.currentTimeMillis()
            )

            Result.success(outputData)
        } catch (e: Exception) {
            // Retry on failure
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(KEY_ERROR to (e.message ?: "Unknown error"))
                )
            }
        }
    }

    companion object {
        const val KEY_DELETED_COUNT = "deleted_count"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_ERROR = "error"
        const val WORK_NAME = "status_cleanup_work"
    }
}
