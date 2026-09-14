package io.github.DanielOfRivia.street_viewer_v2.ui.map

import app.cash.turbine.test
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeStreetCoverageRepository
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var locationPointRepository: FakeLocationPointRepository
    private lateinit var streetCoverageRepository: FakeStreetCoverageRepository
    private lateinit var locationHistoryRepository: FakeLocationHistoryRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locationPointRepository = FakeLocationPointRepository()
        streetCoverageRepository = FakeStreetCoverageRepository()
        locationHistoryRepository = FakeLocationHistoryRepository()
        viewModel = MapViewModel(locationPointRepository, streetCoverageRepository, locationHistoryRepository)
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

    @Test
    fun visitedStreetRunsComeFromTheStreetCoverageRepository() = runTest(dispatcher) {
        streetCoverageRepository.visitedStreetRuns = listOf(
            VisitedStreetRun(points = emptyList()),
        )

        viewModel.uiState.test {
            awaitItem()

            locationPointRepository.insert(point(timestampMillis = 1L))
            assertEquals(1, awaitItem().visitedStreetRuns.size)
        }
    }

    @Test
    fun initialSelectedDateIsToday() = runTest(dispatcher) {
        viewModel.uiState.test {
            assertEquals(LocalDate.now(), awaitItem().selectedDate)
        }
    }

    @Test
    fun selectingAPastDateShowsFetchedHistoricalPointsNotLivePoints() = runTest(dispatcher) {
        locationPointRepository.insert(point(timestampMillis = 1L)) // a "live" point, should be ignored
        val historicalPoint = LocationPoint(
            id = 99,
            latitude = 1.0,
            longitude = 2.0,
            timestampMillis = 500L,
            accuracyMeters = 5f,
        )
        locationHistoryRepository.result = LocationHistoryResult.Success(listOf(historicalPoint))
        val pastDate = LocalDate.now().minusDays(3)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(pastDate)
            dispatcher.scheduler.advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertEquals(pastDate, loaded.selectedDate)
            assertEquals(listOf(historicalPoint), loaded.points)
            assertFalse(loaded.isLoading)
        }
    }

    @Test
    fun failedHistoryFetchSetsErrorMessageAndEmptyPoints() = runTest(dispatcher) {
        locationHistoryRepository.result = LocationHistoryResult.Failure("Server returned HTTP 500")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(LocalDate.now().minusDays(1))
            dispatcher.scheduler.advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertTrue(loaded.points.isEmpty())
            assertEquals("Server returned HTTP 500", loaded.errorMessage)
        }
    }

    @Test
    fun switchingBackToTodayClearsErrorAndShowsLivePoints() = runTest(dispatcher) {
        locationPointRepository.insert(point(timestampMillis = 1L))
        locationHistoryRepository.result = LocationHistoryResult.Failure("offline")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(LocalDate.now().minusDays(1))
            dispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()

            viewModel.onDateSelected(LocalDate.now())
            dispatcher.scheduler.advanceUntilIdle()
            val backToToday = expectMostRecentItem()
            assertEquals(LocalDate.now(), backToToday.selectedDate)
            assertNull(backToToday.errorMessage)
            assertEquals(1, backToToday.points.size)
        }
    }

    @Test
    fun requestedRangeCoversTheWholeSelectedDayInLocalTime() = runTest(dispatcher) {
        val date = LocalDate.now().minusDays(2)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(date)
            dispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
        }

        val (start, end) = requireNotNull(locationHistoryRepository.lastRequestedRange)
        val zone = java.time.ZoneId.systemDefault()
        assertEquals(date.atStartOfDay(zone).toInstant().toEpochMilli(), start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1, end)
    }

    private fun point(timestampMillis: Long) = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = timestampMillis,
        accuracyMeters = 6.4f,
    )
}
