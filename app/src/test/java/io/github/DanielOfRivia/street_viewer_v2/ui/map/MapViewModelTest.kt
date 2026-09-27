package io.github.DanielOfRivia.street_viewer_v2.ui.map

import app.cash.turbine.test
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeLocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeVisitedPlacesRepository
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeVisitedStreetCoverageRepository
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
    private lateinit var visitedStreetCoverageRepository: FakeVisitedStreetCoverageRepository
    private lateinit var locationHistoryRepository: FakeLocationHistoryRepository
    private lateinit var visitedPlacesRepository: FakeVisitedPlacesRepository
    private lateinit var viewModel: MapViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locationPointRepository = FakeLocationPointRepository()
        visitedStreetCoverageRepository = FakeVisitedStreetCoverageRepository()
        locationHistoryRepository = FakeLocationHistoryRepository()
        visitedPlacesRepository = FakeVisitedPlacesRepository()
        viewModel = MapViewModel(
            locationPointRepository,
            visitedStreetCoverageRepository,
            locationHistoryRepository,
            visitedPlacesRepository,
        )
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
            assertEquals(todayStartMillis + 100L, awaitItem().newestPoint?.timestampMillis)

            locationPointRepository.insert(point(timestampMillis = 200L))
            assertEquals(todayStartMillis + 200L, awaitItem().newestPoint?.timestampMillis)
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
    fun coarsePointsAreLeftOutOfTheDrawnTrack() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            locationPointRepository.insert(point(timestampMillis = 1L))
            assertEquals(1, awaitItem().points.size)

            // Still stored (and uploaded, for stay detection) -- just not drawn as a jump, so the
            // drawn state doesn't change at all.
            locationPointRepository.insert(point(timestampMillis = 2L).copy(accuracyMeters = 120f))
            expectNoEvents()
            assertEquals(todayStartMillis + 1L, viewModel.uiState.value.newestPoint?.timestampMillis)
        }
    }

    @Test
    fun visitedStreetRunsComeFromTheVisitedStreetCoverageRepositoryIndependentOfTheSelectedDay() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            // Not gated behind inserting a point or picking a day -- this is an all-time,
            // persisted set the ViewModel just observes.
            visitedStreetCoverageRepository.visitedStreetRuns.value = listOf(
                VisitedStreetRun(points = emptyList()),
            )
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
    fun pastDayMergesNotYetUploadedLocalPointsIntoTheServerTrack() = runTest(dispatcher) {
        val pastDate = LocalDate.now().minusDays(1)
        val dayStart = pastDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val uploaded = pastDayPoint(dayStart + 100L, syncedAtMillis = 1L)
        val pendingUpload = pastDayPoint(dayStart + 200L, syncedAtMillis = null)
        locationPointRepository.insert(uploaded)
        locationPointRepository.insert(pendingUpload)
        // The server only knows about the uploaded one.
        locationHistoryRepository.result = LocationHistoryResult.Success(listOf(uploaded.copy(id = 99)))

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(pastDate)
            dispatcher.scheduler.advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertEquals(listOf(dayStart + 100L, dayStart + 200L), loaded.points.map { it.timestampMillis })
            assertNull(loaded.errorMessage)
        }
    }

    @Test
    fun failedHistoryFetchFallsBackToLocalPointsForThatDay() = runTest(dispatcher) {
        val pastDate = LocalDate.now().minusDays(1)
        val dayStart = pastDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        locationPointRepository.insert(pastDayPoint(dayStart + 100L, syncedAtMillis = 1L))
        locationPointRepository.insert(pastDayPoint(dayStart + 200L, syncedAtMillis = null))
        locationPointRepository.insert(point(timestampMillis = 1L)) // today's, must not leak in
        locationHistoryRepository.result = LocationHistoryResult.Failure("offline")

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(pastDate)
            dispatcher.scheduler.advanceUntilIdle()

            val loaded = expectMostRecentItem()
            assertEquals(listOf(dayStart + 100L, dayStart + 200L), loaded.points.map { it.timestampMillis })
            assertEquals("offline", loaded.errorMessage)
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

    @Test
    fun visitedPlacesAreFetchedOnInitialLoadForToday() = runTest(dispatcher) {
        val place = visitedPlace(id = 1)
        // The init-triggered fetch has no real suspension point in these fakes, so it runs
        // eagerly to completion as soon as the ViewModel is constructed -- the fake's data
        // must be set up first, which means the shared `viewModel` from setUp() (built before
        // this test body runs) is already too late; build a fresh instance here instead.
        visitedPlacesRepository.places = listOf(place)
        val freshViewModel = MapViewModel(
            locationPointRepository,
            visitedStreetCoverageRepository,
            locationHistoryRepository,
            visitedPlacesRepository,
        )

        freshViewModel.uiState.test {
            // First item is stateIn's seed initialValue, emitted before the combine flow
            // (started lazily on this very subscription) has produced anything real.
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(listOf(place), expectMostRecentItem().visitedPlaces)
        }
    }

    @Test
    fun selectingADateFetchesVisitedPlacesForThatSameDayRange() = runTest(dispatcher) {
        val date = LocalDate.now().minusDays(5)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onDateSelected(date)
            dispatcher.scheduler.advanceUntilIdle()
            expectMostRecentItem()
        }

        val (start, end) = requireNotNull(visitedPlacesRepository.lastRequestedRange)
        val zone = java.time.ZoneId.systemDefault()
        assertEquals(date.atStartOfDay(zone).toInstant().toEpochMilli(), start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1, end)
    }

    @Test
    fun switchingDatesReplacesVisitedPlacesRatherThanAccumulating() = runTest(dispatcher) {
        viewModel.uiState.test {
            awaitItem()

            visitedPlacesRepository.places = listOf(visitedPlace(id = 1))
            viewModel.onDateSelected(LocalDate.now().minusDays(1))
            dispatcher.scheduler.advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().visitedPlaces.size)

            visitedPlacesRepository.places = listOf(visitedPlace(id = 2), visitedPlace(id = 3))
            viewModel.onDateSelected(LocalDate.now().minusDays(2))
            dispatcher.scheduler.advanceUntilIdle()
            val places = expectMostRecentItem().visitedPlaces
            assertEquals(2, places.size)
            assertEquals(setOf(2L, 3L), places.map { it.id }.toSet())
        }
    }

    private fun visitedPlace(id: Long) = VisitedPlace(
        id = id,
        latitude = 43.6532,
        longitude = -79.3832,
        arrivalTimeMillis = 0L,
        departureTimeMillis = 1_000L,
        pointCount = 5,
        address = null,
        businesses = emptyList(),
    )

    private val todayStartMillis = LocalDate.now()
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun pastDayPoint(timestampMillis: Long, syncedAtMillis: Long?) = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = timestampMillis,
        accuracyMeters = 6.4f,
        syncedAtMillis = syncedAtMillis,
    )

    // MapViewModel now filters "today" live points down to today's own range (it used to pass
    // observeAllPoints() through unfiltered, which leaked points from the whole 30-day local
    // retention window onto the "today" track) -- offsets must land within today, not near epoch.
    private fun point(timestampMillis: Long) = LocationPoint(
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = todayStartMillis + timestampMillis,
        accuracyMeters = 6.4f,
    )
}
