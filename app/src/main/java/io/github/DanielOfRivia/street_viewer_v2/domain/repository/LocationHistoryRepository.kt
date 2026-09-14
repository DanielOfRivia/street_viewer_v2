package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult

interface LocationHistoryRepository {
    /** Points recorded in [startMillis, endMillis], fetched from the server (not local storage). */
    suspend fun getLocationsInRange(startMillis: Long, endMillis: Long): LocationHistoryResult
}
