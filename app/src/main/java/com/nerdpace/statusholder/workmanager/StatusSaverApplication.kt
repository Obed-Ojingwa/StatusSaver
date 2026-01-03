package com.nerdpace.statusholder.workmanager

class StatusSaverApplication {
}



// Application class to initialize WorkManager
package com.whatsappstatussaver

import android.app.Application
import androidx.work.Configuration
import com.whatsappstatussaver.data.local.StatusDatabase
import com.whatsappstatussaver.data.repository.StatusRepositoryImpl
import com.whatsappstatussaver.worker.CleanupScheduler
import com.whatsappstatussaver.worker.CleanupWorkerFactory

class StatusSaverApplication : Application(), Configuration.Provider {

    // In a real app, use DI framework like Hilt or Koin
    private val database by lazy { StatusDatabase.getInstance(this) }
    private val repository by lazy { StatusRepositoryImpl(this, database.statusMediaDao()) }
    private val workerFactory by lazy { CleanupWorkerFactory(repository) }

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic cleanup on app startup
        CleanupScheduler.scheduleCleanup(this)

        // Optionally trigger immediate cleanup on app startup
        // CleanupScheduler.triggerImmediateCleanup(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
}
