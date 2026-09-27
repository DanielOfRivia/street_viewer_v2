package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedStreetRun
import kotlinx.coroutines.flow.Flow

interface VisitedStreetCoverageRepository {
    /** Every street segment ever recorded as visited, independent of any particular day. */
    fun observeVisitedStreetRuns(): Flow<List<VisitedStreetRun>>

    /**
     * Gap-fills [points] and persists any newly-visited street segments found near them.
     * Safe to call with just the two endpoints of a single new tracking interval, or with a
     * whole day's (or a whole history's) worth of points at once, e.g. for a one-time backfill.
     */
    suspend fun recordVisitedSegments(points: List<LocationPoint>)

    /**
     * Recomputes coverage from scratch out of [points] and replaces everything stored with it,
     * e.g. after the matching rules change. Leaves the stored segments untouched and returns
     * false when the street geometry for the whole area couldn't be fetched.
     */
    suspend fun rebuildVisitedSegments(points: List<LocationPoint>): Boolean
}
