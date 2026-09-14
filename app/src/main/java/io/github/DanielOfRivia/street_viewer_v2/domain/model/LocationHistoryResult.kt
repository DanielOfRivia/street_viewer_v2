package io.github.DanielOfRivia.street_viewer_v2.domain.model

sealed interface LocationHistoryResult {
    data class Success(val points: List<LocationPoint>) : LocationHistoryResult
    data class Failure(val reason: String) : LocationHistoryResult
}
