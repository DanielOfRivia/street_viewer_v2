package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace

interface VisitedPlacesRepository {
    /**
     * Derived stay-places (clustered visits) in [startMillis, endMillis]. Best-effort: any
     * failure returns an empty list rather than an error, since this is a supplementary
     * overlay on the map, not the primary requested data (unlike location history).
     */
    suspend fun getVisitedPlaces(startMillis: Long, endMillis: Long): List<VisitedPlace>
}
