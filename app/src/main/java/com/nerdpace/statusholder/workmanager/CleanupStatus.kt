package com.nerdpace.statusholder.workmanager

class CleanupStatus {
}



// Extension to monitor cleanup work progress
package com.whatsappstatussaver.util

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.whatsappstatussaver.worker.CleanupWorker

data class CleanupStatus(
    val isRunning: Boolean,
    val lastDeletedCount: Int?,
    val lastRunTimestamp: Long?,
    val error: String?
)

fun Context.observeCleanupStatus(): LiveData<CleanupStatus> {
    return WorkManager.getInstance(this)
        .getWorkInfosForUniqueWorkLiveData(CleanupWorker.WORK_NAME)
        .map { workInfoList ->
            val latestWork = workInfoList.firstOrNull()

            CleanupStatus(
                isRunning = latestWork?.state == WorkInfo.State.RUNNING,
                lastDeletedCount = latestWork?.outputData?.getInt(
                    CleanupWorker.KEY_DELETED_COUNT,
                    -1
                )?.takeIf { it >= 0 },
                lastRunTimestamp = latestWork?.outputData?.getLong(
                    CleanupWorker.KEY_TIMESTAMP,
                    -1
                )?.takeIf { it > 0 },
                error = latestWork?.outputData?.getString(CleanupWorker.KEY_ERROR)
            )
        }
}