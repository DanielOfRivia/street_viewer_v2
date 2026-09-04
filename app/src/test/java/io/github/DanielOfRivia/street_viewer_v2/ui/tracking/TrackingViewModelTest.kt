package io.github.DanielOfRivia.street_viewer_v2.ui.tracking

import app.cash.turbine.test
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.TrackingStopReason
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationPointRepository
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
    private lateinit var viewModel: TrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locationPointRepository = FakeLocationPointRepository()
        trackingStatusRepository = FakeTrackingStatusRepository()
        viewModel = TrackingViewModel(locationPointRepository, trackingStatusRepository)
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

    private fun samplePoint() = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = 1_700_000_000_000L,
        accuracyMeters = 6.4f,
    )
}
