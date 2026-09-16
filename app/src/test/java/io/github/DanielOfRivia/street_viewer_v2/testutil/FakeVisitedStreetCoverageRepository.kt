package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVisitedStreetCoverageRepository : VisitedStreetCoverageRepository {

    val visitedStreetRuns = MutableStateFlow<List<VisitedStreetRun>>(emptyList())
    val recordedCalls = mutableListOf<List<LocationPoint>>()

    override fun observeVisitedStreetRuns(): Flow<List<VisitedStreetRun>> = visitedStreetRuns

    override suspend fun recordVisitedSegments(points: List<LocationPoint>) {
        recordedCalls.add(points)
    }
}
