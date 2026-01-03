package com.nerdpace.statusholder.workmanager

class CleanupWorkerFactory {




// Worker Factory for Dependency Injection
    package com.whatsappstatussaver.worker

    import android.content.Context
    import androidx.work.ListenableWorker
    import androidx.work.WorkerFactory
    import androidx.work.WorkerParameters
    import com.whatsappstatussaver.domain.repository.StatusRepository

    class CleanupWorkerFactory(
        private val repository: StatusRepository
    ) : WorkerFactory() {

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return when (workerClassName) {
                CleanupWorker::class.java.name -> {
                    CleanupWorker(appContext, workerParameters, repository)
                }
                else -> null
            }
        }
    }}
