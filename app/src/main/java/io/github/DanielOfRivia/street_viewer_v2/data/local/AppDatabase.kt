package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationPointEntity::class, VisitedStreetSegmentEntity::class],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationPointDao(): LocationPointDao
    abstract fun visitedStreetSegmentDao(): VisitedStreetSegmentDao
}
