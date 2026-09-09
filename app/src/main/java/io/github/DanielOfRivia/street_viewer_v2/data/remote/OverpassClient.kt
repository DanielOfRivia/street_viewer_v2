package io.github.DanielOfRivia.street_viewer_v2.data.remote

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds

interface OverpassClient {
    /** Each returned list is one street's node geometry, in order. Empty on any failure. */
    suspend fun fetchHighways(bounds: MapBounds): List<List<LatLon>>
}
