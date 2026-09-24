package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncResult
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeClock
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationApi
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationPointRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncRepositoryImplTest {

    private lateinit var api: FakeLocationApi
    private lateinit var locationPointRepository: FakeLocationPointRepository
    private lateinit var clock: FakeClock
    private lateinit var syncRepository: SyncRepositoryImpl

    private val retentionWindowMillis = TimeUnit.DAYS.toMillis(30)

    @Before
    fun setUp() {
        api = FakeLocationApi()
        locationPointRepository = FakeLocationPointRepository()
        clock = FakeClock(currentMillis = NOW)
        syncRepository = SyncRepositoryImpl(
            api = api,
            locationPointRepository = locationPointRepository,
            json = Json,
            clock = clock,
            pageSize = 2,
            retentionWindowMillis = retentionWindowMillis,
        )
    }

    @Test
    fun nothingToUploadReturnsSuccessWithZero() = runTest {
        val result = syncRepository.uploadPendingPoints()

        assertEquals(SyncResult.Success(0), result)
        assertEquals(0, api.callCount)
    }

    @Test
    fun allPointsWithinOnePageAreMarkedSyncedNotDeleted() = runTest {
        locationPointRepository.insert(point(NOW - 2))
        locationPointRepository.insert(point(NOW - 1))

        val result = syncRepository.uploadPendingPoints()

        assertEquals(SyncResult.Success(2), result)
        assertEquals(1, api.callCount)
        // still present locally (recent, so the retention cleanup doesn't touch them),
        // just no longer pending
        assertTrue(locationPointRepository.getUnsyncedPage(100).isEmpty())
        assertEquals(2, locationPointRepository.observeAllPoints().first().size)
    }

    @Test
    fun morePointsThanPageSizeAreUploadedAcrossMultiplePages() = runTest {
        repeat(5) { locationPointRepository.insert(point(NOW - it)) }

        val result = syncRepository.uploadPendingPoints()

        // page size 2, 5 points -> 3 calls (2 + 2 + 1)
        assertEquals(SyncResult.Success(5), result)
        assertEquals(3, api.callCount)
        assertTrue(locationPointRepository.getUnsyncedPage(100).isEmpty())
    }

    @Test
    fun partialFailureMarksOnlyTheAcceptedPagesSyncedAndReportsWhatWasUploaded() = runTest {
        repeat(4) { locationPointRepository.insert(point(NOW - it)) }
        api.responseProvider = { call ->
            if (call == 1) {
                Response.success(null)
            } else {
                Response.error(500, "".toResponseBody("text/plain".toMediaType()))
            }
        }

        val result = syncRepository.uploadPendingPoints()

        assertEquals(SyncResult.Failure(2, "Server returned HTTP 500"), result)
        // first page (2 points) marked synced, second page (2 points) still pending --
        // not lost, not double-sent on a retry
        assertEquals(2, locationPointRepository.getUnsyncedPage(100).size)
    }

    @Test
    fun networkExceptionDuringUploadIsSurfacedAsFailureNotThrown() = runTest {
        locationPointRepository.insert(point(NOW))
        api.responseProvider = { throw IOException("no route to host") }

        val result = syncRepository.uploadPendingPoints()

        assertEquals(SyncResult.Failure(0, "no route to host"), result)
        assertEquals(1, locationPointRepository.getUnsyncedPage(100).size)
    }

    @Test
    fun pointInsertedWhileAPageIsInFlightIsPickedUpByALaterPage() = runTest {
        locationPointRepository.insert(point(NOW - 2))
        locationPointRepository.insert(point(NOW - 1))
        // simulate a new GPS fix landing in the DB while the first page's upload is in flight
        api.onUpload = { call ->
            if (call == 1) {
                locationPointRepository.insert(point(NOW))
            }
        }

        val result = syncRepository.uploadPendingPoints()

        assertEquals(SyncResult.Success(3), result)
        assertEquals(2, api.callCount)
        assertTrue(locationPointRepository.getUnsyncedPage(100).isEmpty())
    }

    @Test
    fun concurrentSyncsDoNotUploadTheSamePageTwice() = runTest {
        locationPointRepository.insert(point(NOW - 1))
        locationPointRepository.insert(point(NOW))
        val firstUploadGate = CompletableDeferred<Unit>()
        api.onUpload = { call -> if (call == 1) firstUploadGate.await() }

        val first = async { syncRepository.uploadPendingPoints() }
        val second = async { syncRepository.uploadPendingPoints() }
        runCurrent()
        firstUploadGate.complete(Unit)

        assertEquals(SyncResult.Success(2), first.await())
        assertEquals(SyncResult.Success(0), second.await())
        assertEquals(1, api.callCount)
    }

    @Test
    fun syncedPointsOlderThanTheRetentionWindowAreDeletedAfterSync() = runTest {
        val oldSynced = point(NOW - TimeUnit.DAYS.toMillis(40)).copy(syncedAtMillis = NOW - TimeUnit.DAYS.toMillis(40))
        val recentSynced = point(NOW - TimeUnit.DAYS.toMillis(5)).copy(syncedAtMillis = NOW - TimeUnit.DAYS.toMillis(5))
        locationPointRepository.insert(oldSynced)
        locationPointRepository.insert(recentSynced)

        syncRepository.uploadPendingPoints()

        val remaining = locationPointRepository.observeAllPoints().first()
        assertEquals(1, remaining.size)
        assertEquals(NOW - TimeUnit.DAYS.toMillis(5), remaining.single().timestampMillis)
    }

    @Test
    fun unsyncedPointsAreNeverDeletedByRetentionCleanupRegardlessOfAge() = runTest {
        val veryOldButNeverSynced = point(NOW - TimeUnit.DAYS.toMillis(400))
        locationPointRepository.insert(veryOldButNeverSynced)
        // the only pending point fails to upload, so it never gets marked synced
        api.responseProvider = { throw IOException("offline") }

        val result = syncRepository.uploadPendingPoints()

        assertEquals(SyncResult.Failure(0, "offline"), result)
        val remaining = locationPointRepository.observeAllPoints().first()
        assertEquals(1, remaining.size)
        assertNull(remaining.single().syncedAtMillis)
    }

    private fun point(timestampMillis: Long) = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = timestampMillis,
        accuracyMeters = 6.4f,
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
