package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitedStreetSegmentDao {

    // The (wayId, segmentIndex) primary key makes this a natural dedup point -- re-recording a
    // segment visited on an earlier day (or an earlier fix on the same day) is just a no-op.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(segments: List<VisitedStreetSegmentEntity>)

    @Query("SELECT * FROM visited_street_segments")
    fun observeAll(): Flow<List<VisitedStreetSegmentEntity>>

    @Query("DELETE FROM visited_street_segments")
    suspend fun deleteAll()

    // One transaction, so the map never observes the empty table in between.
    @Transaction
    suspend fun replaceAll(segments: List<VisitedStreetSegmentEntity>) {
        deleteAll()
        insertAll(segments)
    }
}
