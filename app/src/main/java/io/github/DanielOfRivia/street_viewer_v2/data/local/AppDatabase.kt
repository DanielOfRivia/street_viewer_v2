package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocationPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationPointDao(): LocationPointDao
}
