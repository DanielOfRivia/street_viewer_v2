package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository

class FakeLocationHistoryRepository : LocationHistoryRepository {

    var result: LocationHistoryResult = LocationHistoryResult.Success(emptyList())
    var lastRequestedRange: Pair<Long, Long>? = null
        private set

    override suspend fun getLocationsInRange(startMillis: Long, endMillis: Long): LocationHistoryResult {
        lastRequestedRange = startMillis to endMillis
        return result
    }
}
