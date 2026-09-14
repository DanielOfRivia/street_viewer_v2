package io.github.DanielOfRivia.street_viewer_v2.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationPointRepository: LocationPointRepository,
    private val streetCoverageRepository: StreetCoverageRepository,
    private val locationHistoryRepository: LocationHistoryRepository,
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val historicalPoints = MutableStateFlow<List<LocationPoint>?>(null)
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MapUiState> = combine(
        selectedDate,
        locationPointRepository.observeAllPoints(),
        historicalPoints,
        isLoading,
        errorMessage,
    ) { date, livePoints, historical, loading, error ->
        val points = if (date == LocalDate.now()) livePoints else historical.orEmpty()
        MapUiState(
            selectedDate = date,
            points = points,
            visitedStreetRuns = streetCoverageRepository.getVisitedStreetRuns(points),
            isLoading = loading,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState(),
    )

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
        errorMessage.value = null

        if (date == LocalDate.now()) {
            historicalPoints.value = null
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            val zone = ZoneId.systemDefault()
            val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            when (val result = locationHistoryRepository.getLocationsInRange(startMillis, endMillis)) {
                is LocationHistoryResult.Success -> historicalPoints.value = result.points
                is LocationHistoryResult.Failure -> {
                    historicalPoints.value = emptyList()
                    errorMessage.value = result.reason
                }
            }
            isLoading.value = false
        }
    }
}
