package io.github.DanielOfRivia.street_viewer_v2.data.repository

import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncSchedulerState
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.service.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    private val workManager: WorkManager,
) : SyncScheduler {

    override val state: Flow<SyncSchedulerState> =
        workManager.getWorkInfosForUniqueWorkFlow(SyncWorker.UNIQUE_ONE_TIME_NAME).map { it.toState() }

    override fun requestSync() {
        workManager.enqueueUniqueWork(
            SyncWorker.UNIQUE_ONE_TIME_NAME,
            ExistingWorkPolicy.REPLACE,
            SyncWorker.oneTimeRequest(),
        )
    }
}

private fun List<WorkInfo>.toState(): SyncSchedulerState {
    val info = firstOrNull() ?: return SyncSchedulerState.Idle
    return when (info.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> SyncSchedulerState.Running
        WorkInfo.State.SUCCEEDED -> SyncSchedulerState.Succeeded(
            info.outputData.getInt(SyncWorker.KEY_UPLOADED_COUNT, 0),
        )
        WorkInfo.State.FAILED -> SyncSchedulerState.Failed(
            uploadedCount = info.outputData.getInt(SyncWorker.KEY_UPLOADED_COUNT, 0),
            reason = info.outputData.getString(SyncWorker.KEY_ERROR_MESSAGE) ?: "Unknown error",
        )
        WorkInfo.State.CANCELLED, WorkInfo.State.BLOCKED -> SyncSchedulerState.Idle
    }
}
