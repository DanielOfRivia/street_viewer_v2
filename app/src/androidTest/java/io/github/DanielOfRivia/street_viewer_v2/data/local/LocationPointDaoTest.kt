package io.github.DanielOfRivia.street_viewer_v2.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun getPageRespectsLimitAndOrdersOldestFirst() = runBlocking {
        repeat(5) { i -> dao.insert(point(timestamp = i.toLong())) }

        val page = dao.getPage(limit = 3)

        assertEquals(listOf(0L, 1L, 2L), page.map { it.timestamp })
    }

    @Test
    fun deleteByIdsRemovesOnlyTheGivenRows() = runBlocking {
        dao.insert(point(timestamp = 1L))
        dao.insert(point(timestamp = 2L))
        dao.insert(point(timestamp = 3L))

        val idToDelete = dao.observeAll().first().first { it.timestamp == 2L }.id
        dao.deleteByIds(listOf(idToDelete))

        val remaining = dao.observeAll().first()
        assertEquals(listOf(1L, 3L), remaining.map { it.timestamp })
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
