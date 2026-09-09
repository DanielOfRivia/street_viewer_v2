package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import app.cash.turbine.test
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason
import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncSchedulerState
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeSyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeTrackingPreferencesRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeTrackingStatusRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var locationPointRepository: FakeLocationPointRepository
    private lateinit var trackingStatusRepository: FakeTrackingStatusRepository
    private lateinit var trackingPreferencesRepository: FakeTrackingPreferencesRepository
    private lateinit var syncScheduler: FakeSyncScheduler
    private lateinit var viewModel: TrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locationPointRepository = FakeLocationPointRepository()
        trackingStatusRepository = FakeTrackingStatusRepository()
        trackingPreferencesRepository = FakeTrackingPreferencesRepository()
        syncScheduler = FakeSyncScheduler()
        viewModel = TrackingViewModel(
            locationPointRepository,
            trackingStatusRepository,
            trackingPreferencesRepository,
            syncScheduler,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsIdleWithNoPoints() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.pointCount)
            assertFalse(state.isTracking)
            assertNull(state.stopReason)
        }
    }

    @Test
    fun pointCountTracksRepositoryInsertions() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            locationPointRepository.insert(samplePoint())
            assertEquals(1, awaitItem().pointCount)

            locationPointRepository.insert(samplePoint())
            assertEquals(2, awaitItem().pointCount)
        }
    }

    @Test
    fun trackingStatusReflectsRepositoryState() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            trackingStatusRepository.reportStarted()
            assertTrue(awaitItem().isTracking)

            trackingStatusRepository.reportStopped(TrackingStopReason.PERMISSION_MISSING)
            val stopped = awaitItem()
            assertFalse(stopped.isTracking)
            assertEquals(TrackingStopReason.PERMISSION_MISSING, stopped.stopReason)
        }
    }

    @Test
    fun permissionStateChangesAreReflectedInUiState() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            viewModel.onPermissionStateChanged(LocationPermissionState.PermanentlyDenied)
            assertEquals(LocationPermissionState.PermanentlyDenied, awaitItem().permissionState)
        }
    }

    @Test
    fun syncStateReflectsSchedulerState() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            syncScheduler.emit(SyncSchedulerState.Running)
            assertEquals(SyncSchedulerState.Running, awaitItem().syncState)

            syncScheduler.emit(SyncSchedulerState.Succeeded(uploadedCount = 0))
            assertEquals(SyncSchedulerState.Succeeded(0), awaitItem().syncState)
        }
    }

    @Test
    fun onSyncClickDelegatesToScheduler() {
        viewModel.onSyncClick()

        assertEquals(1, syncScheduler.requestSyncCallCount)
    }

    @Test
    fun shouldAutoResumeIsFalseWhenTrackingWasNeverRequested() = runTest(dispatcher) {
        assertFalse(viewModel.shouldAutoResumeTracking())
    }

    @Test
    fun shouldAutoResumeIsTrueWhenRequestedButNotCurrentlyActive() = runTest(dispatcher) {
        trackingPreferencesRepository.setTrackingRequested(true)

        assertTrue(viewModel.shouldAutoResumeTracking())
    }

    @Test
    fun shouldAutoResumeIsFalseWhenAlreadyActive() = runTest(dispatcher) {
        trackingPreferencesRepository.setTrackingRequested(true)
        trackingStatusRepository.reportStarted()

        assertFalse(viewModel.shouldAutoResumeTracking())
    }

    private fun samplePoint() = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = 1_700_000_000_000L,
        accuracyMeters = 6.4f,
    )
}
