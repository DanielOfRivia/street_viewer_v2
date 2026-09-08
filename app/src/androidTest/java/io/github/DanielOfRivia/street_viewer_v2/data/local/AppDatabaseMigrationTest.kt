package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppDatabaseMigrationTest {

    @get:Rule
    val migrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun schemaCreatesSuccessfullyAtVersion1() {
        migrationTestHelper.createDatabase(TEST_DB_NAME, 1).close()
    }

    @Test
    fun migrate1To2AddsNullableSyncedColumnAndPreservesExistingRows() {
        migrationTestHelper.createDatabase(TEST_DB_NAME, 1).apply {
            execSQL(
                "INSERT INTO location_points (latitude, longitude, timestamp, accuracy) " +
                    "VALUES (43.6532, -79.3832, 1000, 6.4)",
            )
            close()
        }

        val migrated = migrationTestHelper.runMigrationsAndValidate(TEST_DB_NAME, 2, true)

        val cursor = migrated.query("SELECT latitude, timestamp, syncedAtMillis FROM location_points")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(43.6532, it.getDouble(0), 0.0001)
            assertEquals(1000L, it.getLong(1))
            assertTrue(it.isNull(2))
        }
        migrated.close()
    }

    companion object {
        private const val TEST_DB_NAME = "migration-test"
    }
}
