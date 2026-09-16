package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationPointRepository {
    fun observePointCount(): Flow<Int>
    fun observeAllPoints(): Flow<List<LocationPoint>>
    suspend fun getMostRecentPoint(): LocationPoint?
    suspend fun insert(point: LocationPoint)
    suspend fun getUnsyncedPage(limit: Int): List<LocationPoint>
    suspend fun markSynced(ids: List<Long>, syncedAtMillis: Long)
    suspend fun deleteSyncedOlderThan(cutoffMillis: Long)
}
