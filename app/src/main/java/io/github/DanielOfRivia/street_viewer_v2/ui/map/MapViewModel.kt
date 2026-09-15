package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.LocationGapFiller
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedPlacesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationPointRepository: LocationPointRepository,
    private val streetCoverageRepository: StreetCoverageRepository,
    private val locationHistoryRepository: LocationHistoryRepository,
    private val visitedPlacesRepository: VisitedPlacesRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val dayLoadState = MutableStateFlow(DayLoadState())
    private val visitedStreetRuns = MutableStateFlow<List<VisitedStreetRun>>(emptyList())
    private val isComputingStreetCoverage = MutableStateFlow(false)

    // Cheap and non-suspending -- safe to combine directly into uiState's own transform below.
    // Street-coverage matching is NOT folded in here because it can call out to Overpass over
    // the network; combine() runs its transform to completion for one emission before it will
    // process the next, so a slow suspend call inside it would block selectedDate/isLoading from
    // ever reaching the UI while it's in flight -- exactly the "changing date freezes on the
    // previous day" symptom this splits apart.
    private val effectivePoints: Flow<List<LocationPoint>> = combine(
        selectedDate,
        locationPointRepository.observeAllPoints(),
        dayLoadState,
    ) { date, livePoints, loadState ->
        // observeAllPoints() returns everything still in local storage (up to the 30-day
        // retention window), not just today -- filter down to the selected day's own range.
        if (date == LocalDate.now()) {
            val (startMillis, endMillis) = dayRangeMillis(date)
            livePoints.filter { it.timestampMillis in startMillis..endMillis }
        } else {
            loadState.historicalPoints.orEmpty()
        }
    }

    val uiState: StateFlow<MapUiState> = combine(
        selectedDate,
        effectivePoints,
        dayLoadState,
        visitedStreetRuns,
        isComputingStreetCoverage,
    ) { date, points, loadState, streetRuns, coverageLoading ->
        MapUiState(
            selectedDate = date,
            points = points,
            visitedStreetRuns = streetRuns,
            visitedPlaces = loadState.visitedPlaces,
            isLoading = loadState.isLoading || coverageLoading,
            errorMessage = loadState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    init {
        // collectLatest cancels an in-flight (possibly slow, network-bound) coverage lookup as
        // soon as the points it was computing for are no longer current -- e.g. the user picked
        // another day before the previous one's Overpass call returned.
        viewModelScope.launch {
            effectivePoints.collectLatest { points ->
                isComputingStreetCoverage.value = true
                visitedStreetRuns.value = streetCoverageRepository.getVisitedStreetRuns(LocationGapFiller.fillGaps(points))
                isComputingStreetCoverage.value = false
            }
        }
        loadDay(LocalDate.now())
    }

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
        // Reset immediately rather than waiting for the fetch to complete, so the previous
        // day's track/pins/error never flash while the new day is loading.
        dayLoadState.value = DayLoadState(isLoading = true)
        loadDay(date)
    }

    private fun dayRangeMillis(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return startMillis to endMillis
    }

    private fun loadDay(date: LocalDate) {
        viewModelScope.launch {
            dayLoadState.update { it.copy(isLoading = true) }

            val (startMillis, endMillis) = dayRangeMillis(date)

            val places = visitedPlacesRepository.getVisitedPlaces(startMillis, endMillis)

            if (date == LocalDate.now()) {
                // The raw track for today comes from the live local Flow above, not this
                // fetch -- only visited places (always server-derived) need it.
                dayLoadState.update { it.copy(visitedPlaces = places, isLoading = false) }
                return@launch
            }

            when (val result = locationHistoryRepository.getLocationsInRange(startMillis, endMillis)) {
                is LocationHistoryResult.Success -> dayLoadState.update {
                    it.copy(historicalPoints = result.points, visitedPlaces = places, isLoading = false)
                }
                is LocationHistoryResult.Failure -> dayLoadState.update {
                    it.copy(
                        historicalPoints = emptyList(),
                        visitedPlaces = places,
                        errorMessage = result.reason,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private data class DayLoadState(
        val historicalPoints: List<LocationPoint>? = null,
        val visitedPlaces: List<VisitedPlace> = emptyList(),
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )
}
