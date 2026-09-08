package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationPointDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: LocationPointDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.locationPointDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun observeCountReflectsInsertedRows() = runBlocking {
        assertEquals(0, dao.observeCount().first())

        dao.insert(point(timestamp = 1L))
        assertEquals(1, dao.observeCount().first())

        dao.insert(point(timestamp = 2L))
        assertEquals(2, dao.observeCount().first())
    }

    @Test
    fun observeAllOrdersByTimestampAscending() = runBlocking {
        dao.insert(point(timestamp = 300L))
        dao.insert(point(timestamp = 100L))
        dao.insert(point(timestamp = 200L))

        val all = dao.observeAll().first()

        assertEquals(listOf(100L, 200L, 300L), all.map { it.timestamp })
    }

    @Test
    fun getUnsyncedPageRespectsLimitOrdersOldestFirstAndExcludesSyncedRows() = runBlocking {
        repeat(5) { i -> dao.insert(point(timestamp = i.toLong())) }
        val allIds = dao.observeAll().first().map { it.id }
        dao.markSynced(listOf(allIds[0]), syncedAtMillis = 999L)

        val page = dao.getUnsyncedPage(limit = 3)

        // timestamp 0 was synced above and is excluded, even though it's oldest
        assertEquals(listOf(1L, 2L, 3L), page.map { it.timestamp })
    }

    @Test
    fun markSyncedUpdatesOnlyTheGivenRows() = runBlocking {
        dao.insert(point(timestamp = 1L))
        dao.insert(point(timestamp = 2L))
        dao.insert(point(timestamp = 3L))

        val idToMark = dao.observeAll().first().first { it.timestamp == 2L }.id
        dao.markSynced(listOf(idToMark), syncedAtMillis = 500L)

        val all = dao.observeAll().first()
        assertEquals(500L, all.first { it.timestamp == 2L }.syncedAtMillis)
        assertNull(all.first { it.timestamp == 1L }.syncedAtMillis)
        assertNull(all.first { it.timestamp == 3L }.syncedAtMillis)
    }

    @Test
    fun deleteSyncedOlderThanRemovesOnlySyncedRowsPastTheCutoff() = runBlocking {
        dao.insert(point(timestamp = 100L)) // old, stays unsynced
        dao.insert(point(timestamp = 200L)) // old, will be synced
        dao.insert(point(timestamp = 900L)) // recent, will be synced

        val all = dao.observeAll().first()
        dao.markSynced(listOf(all.first { it.timestamp == 200L }.id), syncedAtMillis = 1L)
        dao.markSynced(listOf(all.first { it.timestamp == 900L }.id), syncedAtMillis = 1L)

        dao.deleteSyncedOlderThan(cutoffMillis = 500L)

        val remaining = dao.observeAll().first()
        // the old unsynced row survives regardless of age; the old synced row is purged;
        // the recent synced row survives
        assertEquals(listOf(100L, 900L), remaining.map { it.timestamp }.sorted())
    }

    @Test
    fun accuracySurvivesTheRoundTripAsAnExactFloat() = runBlocking {
        dao.insert(point(timestamp = 1L, accuracy = 6.4f))

        val stored = dao.observeAll().first().single()

        assertEquals(6.4f, stored.accuracy)
    }

    private fun point(timestamp: Long, accuracy: Float = 5f) = LocationPointEntity(
        latitude = 43.6532,
        longitude = -79.3832,
        timestamp = timestamp,
        accuracy = accuracy,
    )
}
