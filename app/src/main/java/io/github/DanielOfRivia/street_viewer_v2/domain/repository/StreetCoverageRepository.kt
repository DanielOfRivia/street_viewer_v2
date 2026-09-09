package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun

interface StreetCoverageRepository {
    /** Street geometry near [points] that passes within the visited radius of at least one of them. */
    suspend fun getVisitedStreetRuns(points: List<LocationPoint>): List<VisitedStreetRun>
}
