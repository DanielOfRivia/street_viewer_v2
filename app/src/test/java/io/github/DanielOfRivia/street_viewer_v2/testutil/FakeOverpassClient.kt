package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClient
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds

class FakeOverpassClient : OverpassClient {

    var ways: List<List<LatLon>> = emptyList()
    var fetchCallCount = 0
        private set
    val requestedBounds = mutableListOf<MapBounds>()

    override suspend fun fetchHighways(bounds: MapBounds): List<List<LatLon>> {
        fetchCallCount++
        requestedBounds.add(bounds)
        return ways
    }
}
