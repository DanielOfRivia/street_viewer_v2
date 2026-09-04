package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.local.LocationPointDao
import io.github.DanielOfRivia.street_viewer_v2.data.local.toDomain
import io.github.DanielOfRivia.street_viewer_v2.data.local.toEntity
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationPointRepositoryImpl @Inject constructor(
    private val dao: LocationPointDao,
) : LocationPointRepository {

    override fun observePointCount(): Flow<Int> = dao.observeCount()

    override fun observeAllPoints(): Flow<List<LocationPoint>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insert(point: LocationPoint) {
        dao.insert(point.toEntity())
    }

    override suspend fun getPage(limit: Int): List<LocationPoint> =
        dao.getPage(limit).map { it.toDomain() }

    override suspend fun deleteByIds(ids: List<Long>) {
        dao.deleteByIds(ids)
    }
}
