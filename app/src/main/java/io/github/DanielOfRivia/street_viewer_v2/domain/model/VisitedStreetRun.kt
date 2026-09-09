package io.github.DanielOfRivia.street_viewer_v2.domain.model

/** One contiguous run of a street's own geometry that passes within the visited radius. */
data class VisitedStreetRun(val points: List<LatLon>)
