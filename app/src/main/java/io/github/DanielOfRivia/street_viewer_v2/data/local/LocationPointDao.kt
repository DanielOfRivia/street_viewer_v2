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

    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecent(): LocationPointEntity?

    @Query("SELECT * FROM location_points WHERE syncedAtMillis IS NULL ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedPage(limit: Int): List<LocationPointEntity>

    @Query("UPDATE location_points SET syncedAtMillis = :syncedAtMillis WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>, syncedAtMillis: Long)

    @Query("DELETE FROM location_points WHERE syncedAtMillis IS NOT NULL AND timestamp < :cutoffMillis")
    suspend fun deleteSyncedOlderThan(cutoffMillis: Long)
}
