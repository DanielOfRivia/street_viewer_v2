package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClient
import io.github.DanielOfRivia.street_viewer_v2.domain.GeoMath
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreetCoverageRepositoryImpl @Inject constructor(
    private val overpassClient: OverpassClient,
) : StreetCoverageRepository {

    private val cacheMutex = Mutex()
    private var cachedBounds: MapBounds? = null
    private var cachedWays: List<List<LatLon>> = emptyList()

    override suspend fun getVisitedStreetRuns(points: List<LocationPoint>): List<VisitedStreetRun> {
        if (points.isEmpty()) return emptyList()

        val visitedPoints = points.map { LatLon(it.latitude, it.longitude) }
        val requiredBounds = MapBounds.ofPoints(visitedPoints)

        val ways = cacheMutex.withLock {
            val cached = cachedBounds
            // Refetches only when the visited area grows past what's already cached, not on
            // every single new point -- Overpass is a shared public resource and street
            // geometry itself doesn't change, only which points we've recorded.
            if (cached == null || !cached.contains(requiredBounds)) {
                val paddedBounds = requiredBounds.padded(CACHE_PADDING_DEGREES)
                cachedWays = overpassClient.fetchHighways(paddedBounds)
                cachedBounds = paddedBounds
            }
            cachedWays
        }

        return ways.flatMap { way -> extractVisitedRuns(way, visitedPoints) }
    }

    private fun extractVisitedRuns(way: List<LatLon>, visitedPoints: List<LatLon>): List<VisitedStreetRun> {
        if (way.size < 2) return emptyList()

        val segmentIsVisited = BooleanArray(way.size - 1) { i ->
            isSegmentVisited(way[i], way[i + 1], visitedPoints)
        }

        val runs = mutableListOf<VisitedStreetRun>()
        var index = 0
        while (index < segmentIsVisited.size) {
            if (!segmentIsVisited[index]) {
                index++
                continue
            }
            val runPoints = mutableListOf(way[index])
            while (index < segmentIsVisited.size && segmentIsVisited[index]) {
                runPoints.add(way[index + 1])
                index++
            }
            runs.add(VisitedStreetRun(runPoints))
        }
        return runs
    }

    private fun isSegmentVisited(a: LatLon, b: LatLon, visitedPoints: List<LatLon>): Boolean =
        visitedPoints.any { point ->
            GeoMath.distanceToSegmentMeters(
                pointLat = point.latitude,
                pointLon = point.longitude,
                aLat = a.latitude,
                aLon = a.longitude,
                bLat = b.latitude,
                bLon = b.longitude,
            ) <= VISITED_RADIUS_METERS
        }

    companion object {
        private const val VISITED_RADIUS_METERS = 30.0

        // ~1.1km of latitude margin around the visited-points bounding box, so a bit more
        // walking doesn't immediately force a re-fetch.
        private const val CACHE_PADDING_DEGREES = 0.01
    }
}
