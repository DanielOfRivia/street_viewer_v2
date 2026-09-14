package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedPlacesRepository

class FakeVisitedPlacesRepository : VisitedPlacesRepository {

    var places: List<VisitedPlace> = emptyList()
    var lastRequestedRange: Pair<Long, Long>? = null
        private set

    override suspend fun getVisitedPlaces(startMillis: Long, endMillis: Long): List<VisitedPlace> {
        lastRequestedRange = startMillis to endMillis
        return places
    }
}
