package io.github.DanielOfRivia.street_viewer_v2.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncResult
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val result = syncRepository.uploadPendingPoints()) {
            is SyncResult.Success -> Result.success(
                workDataOf(KEY_UPLOADED_COUNT to result.uploadedCount),
            )
            is SyncResult.Failure -> {
                val output = workDataOf(
                    KEY_UPLOADED_COUNT to result.uploadedCount,
                    KEY_ERROR_MESSAGE to result.reason,
                )
                if (runAttemptCount < MAX_RUN_ATTEMPTS) Result.retry() else Result.failure(output)
            }
        }
    }

    companion object {
        const val UNIQUE_PERIODIC_NAME = "location_sync_periodic"
        const val UNIQUE_ONE_TIME_NAME = "location_sync_now"
        const val KEY_UPLOADED_COUNT = "uploaded_count"
        const val KEY_ERROR_MESSAGE = "error_message"
        private const val MAX_RUN_ATTEMPTS = 3
        private const val PERIODIC_INTERVAL_HOURS = 1L

        private val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()

        fun oneTimeRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                .build()
    }
}
