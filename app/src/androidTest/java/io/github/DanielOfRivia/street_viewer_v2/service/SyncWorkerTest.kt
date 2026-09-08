package io.github.DanielOfRivia.street_viewer_v2.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncResult
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun buildWorker(syncRepository: SyncRepository): SyncWorker =
        TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(FakeWorkerFactory(syncRepository))
            .build()

    @Test
    fun successReportsUploadedCountAndSucceeds() = runTest {
        val worker = buildWorker(FakeSyncRepository(SyncResult.Success(42)))

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.success(workDataOf(SyncWorker.KEY_UPLOADED_COUNT to 42)),
            result,
        )
    }

    @Test
    fun failureRetriesBeforeTheAttemptCapIsReached() = runTest {
        val worker = buildWorker(FakeSyncRepository(SyncResult.Failure(10, "boom")))

        val result = worker.doWork()

        // runAttemptCount defaults to 0 for a freshly built test worker, below the retry
        // cap, so the first attempt retries rather than reporting a terminal failure.
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    private class FakeSyncRepository(private val result: SyncResult) : SyncRepository {
        override suspend fun uploadPendingPoints(): SyncResult = result
    }

    private class FakeWorkerFactory(
        private val syncRepository: SyncRepository,
    ) : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker = SyncWorker(appContext, workerParameters, syncRepository)
    }
}
