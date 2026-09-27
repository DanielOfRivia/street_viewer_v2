package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.local.VisitedStreetSegmentDao
import io.github.DanielOfRivia.street_viewer_v2.data.local.VisitedStreetSegmentEntity
import io.github.DanielOfRivia.street_viewer_v2.data.local.toDomain
import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClient
import io.github.DanielOfRivia.street_viewer_v2.domain.GeoMath
import io.github.DanielOfRivia.street_viewer_v2.domain.LocationGapFiller
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds
import io.github.DanielOfRivia.street_viewer_v2.domain.model.OsmWay
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitedStreetCoverageRepositoryImpl @Inject constructor(
    private val overpassClient: OverpassClient,
    private val dao: VisitedStreetSegmentDao,
) : VisitedStreetCoverageRepository {

    private val cacheMutex = Mutex()
    private var cachedBounds: MapBounds? = null
    private var cachedWays: List<OsmWay> = emptyList()

    override fun observeVisitedStreetRuns(): Flow<List<VisitedStreetRun>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun recordVisitedSegments(points: List<LocationPoint>) {
        if (points.isEmpty()) return

        val visitedPoints = visitedPointsOf(points)
        // Every point may have been dropped as too fast (e.g. both ends of a pair during a drive).
        if (visitedPoints.isEmpty()) return

        val (ways, _) = waysCovering(MapBounds.ofPoints(visitedPoints))

        val newSegments = ways.flatMap { way -> visitedSegmentsOf(way, visitedPoints) }
        if (newSegments.isNotEmpty()) {
            dao.insertAll(newSegments)
        }
    }

    override suspend fun rebuildVisitedSegments(points: List<LocationPoint>): Boolean {
        val visitedPoints = visitedPointsOf(points)
        if (visitedPoints.isEmpty()) {
            dao.replaceAll(emptyList())
            return true
        }

        // Unlike an incremental record, a partial result here would wipe streets that are only
        // missing because Overpass was unreachable -- all or nothing.
        val (ways, covered) = waysCovering(MapBounds.ofPoints(visitedPoints))
        if (!covered) return false

        dao.replaceAll(ways.flatMap { way -> visitedSegmentsOf(way, visitedPoints) })
        return true
    }

    private fun visitedPointsOf(points: List<LocationPoint>): List<LatLon> =
        LocationGapFiller.fillGaps(points).map { LatLon(it.latitude, it.longitude) }

    /** The cached ways, and whether they're known to cover all of [requiredBounds]. */
    private suspend fun waysCovering(requiredBounds: MapBounds): Pair<List<OsmWay>, Boolean> =
        cacheMutex.withLock {
            val cached = cachedBounds
            // Refetches only when the visited area grows past what's already cached, not on
            // every single new point -- Overpass is a shared public resource and street
            // geometry itself doesn't change, only which points we've recorded.
            if (cached == null || !cached.contains(requiredBounds)) {
                val paddedBounds = requiredBounds.padded(CACHE_PADDING_DEGREES)
                val fetched = overpassClient.fetchHighways(paddedBounds)
                // fetchHighways silently swallows failures into an empty list -- only record
                // this bounds as cached once something real came back, so a transient failure
                // gets retried on the next call instead of permanently treating that whole area
                // as road-free for the rest of the app's process lifetime.
                if (fetched.isNotEmpty()) {
                    cachedWays = fetched
                    cachedBounds = paddedBounds
                }
            }
            cachedWays to (cachedBounds?.contains(requiredBounds) == true)
        }

    private fun visitedSegmentsOf(way: OsmWay, visitedPoints: List<LatLon>): List<VisitedStreetSegmentEntity> {
        if (way.nodes.size < 2) return emptyList()

        return (0 until way.nodes.size - 1).mapNotNull { index ->
            val a = way.nodes[index]
            val b = way.nodes[index + 1]
            val isVisited = visitedPoints.any { point ->
                GeoMath.distanceToSegmentMeters(
                    pointLat = point.latitude,
                    pointLon = point.longitude,
                    aLat = a.latitude,
                    aLon = a.longitude,
                    bLat = b.latitude,
                    bLon = b.longitude,
                ) <= VISITED_RADIUS_METERS
            }
            if (!isVisited) {
                null
            } else {
                VisitedStreetSegmentEntity(
                    wayId = way.id,
                    segmentIndex = index,
                    startLat = a.latitude,
                    startLon = a.longitude,
                    endLat = b.latitude,
                    endLon = b.longitude,
                )
            }
        }
    }

    companion object {
        private const val VISITED_RADIUS_METERS = 30.0

        // ~1.1km of latitude margin around the visited-points bounding box, so a bit more
        // walking doesn't immediately force a re-fetch.
        private const val CACHE_PADDING_DEGREES = 0.01
    }
}
