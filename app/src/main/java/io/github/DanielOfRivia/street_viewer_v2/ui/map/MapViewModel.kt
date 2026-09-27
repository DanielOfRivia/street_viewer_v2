package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedPlacesRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    private val visitedStreetCoverageRepository: VisitedStreetCoverageRepository,
    private val locationHistoryRepository: LocationHistoryRepository,
    private val visitedPlacesRepository: VisitedPlacesRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val dayLoadState = MutableStateFlow(DayLoadState())

    val uiState: StateFlow<MapUiState> = combine(
        selectedDate,
        locationPointRepository.observeAllPoints(),
        dayLoadState,
        // All-time, independent of the selected day -- a street stays colored once visited,
        // no matter which day's track happens to be showing right now.
        visitedStreetCoverageRepository.observeVisitedStreetRuns(),
    ) { date, livePoints, loadState, streetRuns ->
        // observeAllPoints() returns everything still in local storage (up to the 30-day
        // retention window), not just today -- filter down to the selected day's own range.
        val (startMillis, endMillis) = dayRangeMillis(date)
        val localDayPoints = livePoints.filter { it.timestampMillis in startMillis..endMillis }
        val points = if (date == LocalDate.now()) {
            localDayPoints
        } else {
            mergeWithLocal(loadState.historicalPoints.orEmpty(), localDayPoints)
        }
        MapUiState(
            selectedDate = date,
            points = points,
            visitedStreetRuns = streetRuns,
            visitedPlaces = loadState.visitedPlaces,
            isLoading = loadState.isLoading,
            errorMessage = loadState.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    init {
        loadDay(LocalDate.now())
    }

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
        // Reset immediately rather than waiting for the fetch to complete, so the previous
        // day's track/pins/error never flash while the new day is loading.
        dayLoadState.value = DayLoadState(isLoading = true)
        loadDay(date)
    }

    // The server only has what's been uploaded so far -- points still waiting for the next
    // sync live only in local storage, and when the fetch fails the server contributes nothing
    // at all. Fill both gaps from local storage, matching on timestamp (preserved exactly
    // through upload) rather than the synced flag, so a point that gets synced after this
    // day's fetch completed isn't dropped from the track until the next reload.
    private fun mergeWithLocal(serverPoints: List<LocationPoint>, localPoints: List<LocationPoint>): List<LocationPoint> {
        val serverTimestamps = serverPoints.mapTo(HashSet()) { it.timestampMillis }
        val localOnly = localPoints.filter { it.timestampMillis !in serverTimestamps }
        if (localOnly.isEmpty()) return serverPoints
        return (serverPoints + localOnly).sortedBy { it.timestampMillis }
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
