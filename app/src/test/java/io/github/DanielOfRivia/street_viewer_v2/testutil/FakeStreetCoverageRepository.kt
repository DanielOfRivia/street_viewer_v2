package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository

class FakeStreetCoverageRepository : StreetCoverageRepository {

    var visitedStreetRuns: List<VisitedStreetRun> = emptyList()

    override suspend fun getVisitedStreetRuns(points: List<LocationPoint>): List<VisitedStreetRun> =
        visitedStreetRuns
}
