package io.github.DanielOfRivia.street_viewer_v2.ui.map

import app.cash.turbine.test
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationPointRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var locationPointRepository: FakeLocationPointRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locationPointRepository = FakeLocationPointRepository()
        viewModel = MapViewModel(locationPointRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateHasNoPointsAndNoNewestPoint() = runTest(dispatcher) {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.points.isEmpty())
            assertNull(state.newestPoint)
        }
    }

    @Test
    fun newestPointIsTheLastInsertedPoint() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            locationPointRepository.insert(point(timestampMillis = 100L))
            assertEquals(100L, awaitItem().newestPoint?.timestampMillis)

            locationPointRepository.insert(point(timestampMillis = 200L))
            assertEquals(200L, awaitItem().newestPoint?.timestampMillis)
        }
    }

    @Test
    fun pointsListGrowsAsPointsAreInserted() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            locationPointRepository.insert(point(timestampMillis = 1L))
            assertEquals(1, awaitItem().points.size)

            locationPointRepository.insert(point(timestampMillis = 2L))
            assertEquals(2, awaitItem().points.size)
        }
    }

    private fun point(timestampMillis: Long) = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = timestampMillis,
        accuracyMeters = 6.4f,
    )
}
