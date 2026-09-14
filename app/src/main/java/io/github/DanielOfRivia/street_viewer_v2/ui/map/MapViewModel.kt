package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.LocationGapFiller
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedPlacesRepository
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
    private val streetCoverageRepository: StreetCoverageRepository,
    private val locationHistoryRepository: LocationHistoryRepository,
    private val visitedPlacesRepository: VisitedPlacesRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val dayLoadState = MutableStateFlow(DayLoadState())

    val uiState: StateFlow<MapUiState> = combine(
        selectedDate,
        locationPointRepository.observeAllPoints(),
        dayLoadState,
    ) { date, livePoints, loadState ->
        // observeAllPoints() returns everything still in local storage (up to the 30-day
        // retention window), not just today -- filter down to the selected day's own range.
        val points = if (date == LocalDate.now()) {
            val (startMillis, endMillis) = dayRangeMillis(date)
            livePoints.filter { it.timestampMillis in startMillis..endMillis }
        } else {
            loadState.historicalPoints.orEmpty()
        }
        MapUiState(
            selectedDate = date,
            points = points,
            visitedStreetRuns = streetCoverageRepository.getVisitedStreetRuns(LocationGapFiller.fillGaps(points)),
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
