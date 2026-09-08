package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLocationPointRepository : LocationPointRepository {

    private val points = MutableStateFlow<List<LocationPoint>>(emptyList())
    private var nextId = 1L

    override fun observePointCount(): Flow<Int> = points.map { it.size }

    override fun observeAllPoints(): Flow<List<LocationPoint>> = points

    override suspend fun insert(point: LocationPoint) {
        points.value = points.value + point.copy(id = nextId++)
    }

    override suspend fun getUnsyncedPage(limit: Int): List<LocationPoint> =
        points.value.filter { it.syncedAtMillis == null }.sortedBy { it.timestampMillis }.take(limit)

    override suspend fun markSynced(ids: List<Long>, syncedAtMillis: Long) {
        points.value = points.value.map { if (it.id in ids) it.copy(syncedAtMillis = syncedAtMillis) else it }
    }

    override suspend fun deleteSyncedOlderThan(cutoffMillis: Long) {
        points.value = points.value.filterNot { it.syncedAtMillis != null && it.timestampMillis < cutoffMillis }
    }
}
