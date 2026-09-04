package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {

    @Insert
    suspend fun insert(point: LocationPointEntity)

    @Query("SELECT COUNT(*) FROM location_points")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM location_points ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPage(limit: Int): List<LocationPointEntity>

    @Query("DELETE FROM location_points WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
