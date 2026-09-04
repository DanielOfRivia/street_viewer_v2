package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import kotlinx.coroutines.flow.Flow

interface LocationPointRepository {
    fun observePointCount(): Flow<Int>
    fun observeAllPoints(): Flow<List<LocationPoint>>
    suspend fun insert(point: LocationPoint)
    suspend fun getPage(limit: Int): List<LocationPoint>
    suspend fun deleteByIds(ids: List<Long>)
}
