package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
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

    companion object {
        private const val TEST_DB_NAME = "migration-test"
    }
}
