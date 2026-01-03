package com.nerdpace.statusholder.workmanager

class scheduleCleanup {
}



// Cleanup Scheduler
package com.whatsappstatussaver.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object CleanupScheduler {

    /**
     * Schedule periodic cleanup work that runs every 24 hours
     * This ensures expired cached media is deleted automatically
     */
    fun scheduleCleanup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Only run when battery is not low
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2, // Allow 2 hour flex window
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag("cleanup")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
            cleanupRequest
        )
    }

    /**
     * Trigger immediate one-time cleanup
     * Useful for manual cleanup or app startup
     */
    fun triggerImmediateCleanup(context: Context) {
        val cleanupRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
            .addTag("cleanup_immediate")
            .build()

        WorkManager.getInstance(context).enqueue(cleanupRequest)
    }

    /**
     * Cancel all scheduled cleanup work
     */
    fun cancelCleanup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CleanupWorker.WORK_NAME)
    }

    /**
     * Get status of cleanup work
     */
    fun getCleanupWorkInfo(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData(CleanupWorker.WORK_NAME)
    }
}
