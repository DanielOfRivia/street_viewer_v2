package io.github.DanielOfRivia.street_viewer_v2.data.remote

import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds
import io.github.DanielOfRivia.street_viewer_v2.domain.model.OsmWay

interface OverpassClient {
    /** Each returned way is one street's own identity and node geometry, in order. Empty on any failure. */
    suspend fun fetchHighways(bounds: MapBounds): List<OsmWay>
}
