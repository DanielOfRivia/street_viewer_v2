package io.github.DanielOfRivia.street_viewer_v2.domain.model

data class MapBounds(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    fun contains(other: MapBounds): Boolean =
        south <= other.south && west <= other.west && north >= other.north && east >= other.east

    fun padded(degrees: Double): MapBounds = MapBounds(
        south = south - degrees,
        west = west - degrees,
        north = north + degrees,
        east = east + degrees,
    )

    companion object {
        fun ofPoints(points: List<LatLon>): MapBounds {
            require(points.isNotEmpty()) { "Cannot compute bounds of an empty point list" }
            return MapBounds(
                south = points.minOf { it.latitude },
                west = points.minOf { it.longitude },
                north = points.maxOf { it.latitude },
                east = points.maxOf { it.longitude },
            )
        }
    }
}
