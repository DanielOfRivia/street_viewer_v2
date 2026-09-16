package io.github.DanielOfRivia.street_viewer_v2.domain.model

/** One OSM way's stable identity and node geometry, in order. */
data class OsmWay(val id: Long, val nodes: List<LatLon>)
