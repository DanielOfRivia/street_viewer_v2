package io.github.DanielOfRivia.street_viewer_v2.data.repository

import app.cash.turbine.test
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.OsmWay
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeOverpassClient
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeVisitedStreetSegmentDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VisitedStreetCoverageRepositoryImplTest {

    private lateinit var overpassClient: FakeOverpassClient
    private lateinit var dao: FakeVisitedStreetSegmentDao
    private lateinit var repository: VisitedStreetCoverageRepositoryImpl

    @Before
    fun setUp() {
        overpassClient = FakeOverpassClient()
        dao = FakeVisitedStreetSegmentDao()
        repository = VisitedStreetCoverageRepositoryImpl(overpassClient, dao)
    }

    @Test
    fun noPointsPersistsNothingAndDoesNotFetch() = runTest {
        repository.recordVisitedSegments(emptyList())

        assertEquals(0, overpassClient.fetchCallCount)
        repository.observeVisitedStreetRuns().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun waySegmentFarFromEveryPointIsExcluded() = runTest {
        // 5 equally-spaced nodes, ~111m apart at the equator
        overpassClient.ways = listOf(
            way(
                1L,
                LatLon(0.0, 0.000), LatLon(0.0, 0.001), LatLon(0.0, 0.002),
                LatLon(0.0, 0.003), LatLon(0.0, 0.004),
            ),
        )

        repository.recordVisitedSegments(listOf(point(0.0, 0.000)))

        // only the segment touching (0,0) is within 30m; the rest of the way is ~111m+ away
        repository.observeVisitedStreetRuns().test {
            val runs = awaitItem()
            assertEquals(1, runs.size)
            assertEquals(listOf(LatLon(0.0, 0.000), LatLon(0.0, 0.001)), runs.single().points)
        }
    }

    @Test
    fun eachMatchingSegmentIsStoredAsItsOwnRunRatherThanMergedWithNeighbours() = runTest {
        overpassClient.ways = listOf(
            way(
                1L,
                LatLon(0.0, 0.000), LatLon(0.0, 0.001), LatLon(0.0, 0.002),
                LatLon(0.0, 0.003), LatLon(0.0, 0.004),
            ),
        )

        // (0.000) only touches segment 0-1; (0.003) sits exactly on the shared vertex between
        // segments 2-3 and 3-4, so it matches both of those too -- three segments in total,
        // each persisted as its own 2-point run rather than merged into a longer polyline.
        // The two points are >90s apart so LocationGapFiller (applied inside
        // recordVisitedSegments) doesn't bridge the ~330m between them with interpolated
        // points, which would otherwise also light up the segment in between.
        repository.recordVisitedSegments(
            listOf(point(0.0, 0.000, timestampMillis = 0L), point(0.0, 0.003, timestampMillis = 200_000L)),
        )

        repository.observeVisitedStreetRuns().test {
            val runs = awaitItem()
            assertEquals(3, runs.size)
            val pointSets = runs.map { it.points }.toSet()
            assertTrue(listOf(LatLon(0.0, 0.000), LatLon(0.0, 0.001)) in pointSets)
            assertTrue(listOf(LatLon(0.0, 0.002), LatLon(0.0, 0.003)) in pointSets)
            assertTrue(listOf(LatLon(0.0, 0.003), LatLon(0.0, 0.004)) in pointSets)
        }
    }

    @Test
    fun aWayShorterThanTwoNodesProducesNoRuns() = runTest {
        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0)))

        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))

        repository.observeVisitedStreetRuns().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun repeatedCallsWithinTheCachedAreaDoNotRefetch() = runTest {
        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))

        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))
        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))
        repository.recordVisitedSegments(listOf(point(0.00001, 0.00001)))

        assertEquals(1, overpassClient.fetchCallCount)
    }

    @Test
    fun aPointOutsideTheCachedPaddedAreaTriggersARefetch() = runTest {
        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))

        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))
        // well beyond the ~0.01 degree cache padding around the first call's bounds
        repository.recordVisitedSegments(listOf(point(5.0, 5.0)))

        assertEquals(2, overpassClient.fetchCallCount)
    }

    @Test
    fun aFailedFetchIsNotCachedAndRetriesOnTheNextCall() = runTest {
        overpassClient.ways = emptyList() // simulates a failed/empty Overpass response

        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))
        assertEquals(1, overpassClient.fetchCallCount)

        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))
        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))

        // The first call's empty result must not have been cached as "this area has no roads" --
        // it retries and this time finds (and persists) the real way.
        assertEquals(2, overpassClient.fetchCallCount)
        repository.observeVisitedStreetRuns().test {
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun recordingTheSameSegmentTwiceDoesNotDuplicateIt() = runTest {
        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))

        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))
        repository.recordVisitedSegments(listOf(point(0.0, 0.0))) // e.g. visited again another day

        repository.observeVisitedStreetRuns().test {
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun aBatchThatIsEntirelyTooFastRecordsNothingAndDoesNotFetch() = runTest {
        // ~500m in 30s -> ~60 km/h, e.g. driving
        repository.recordVisitedSegments(
            listOf(point(0.0, 0.0, timestampMillis = 0L), point(0.0, 0.0045, timestampMillis = 30_000L)),
        )

        assertEquals(0, overpassClient.fetchCallCount)
        repository.observeVisitedStreetRuns().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun rebuildReplacesPreviouslyStoredSegmentsWithOnlyWhatThePointsStillCover() = runTest {
        overpassClient.ways = listOf(
            way(1L, LatLon(0.0, 0.000), LatLon(0.0, 0.001), LatLon(0.0, 0.002)),
        )
        repository.recordVisitedSegments(listOf(point(0.0, 0.002)))

        val rebuilt = repository.rebuildVisitedSegments(listOf(point(0.0, 0.000)))

        assertTrue(rebuilt)
        repository.observeVisitedStreetRuns().test {
            assertEquals(listOf(LatLon(0.0, 0.000), LatLon(0.0, 0.001)), awaitItem().single().points)
        }
    }

    @Test
    fun rebuildKeepsExistingSegmentsWhenStreetsCannotBeFetched() = runTest {
        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))
        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))

        overpassClient.ways = emptyList() // simulates a failed/empty Overpass response
        val rebuilt = repository.rebuildVisitedSegments(listOf(point(1.0, 1.0)))

        assertFalse(rebuilt)
        repository.observeVisitedStreetRuns().test {
            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun rebuildWithOnlyTooFastPointsClearsEverything() = runTest {
        overpassClient.ways = listOf(way(1L, LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))
        repository.recordVisitedSegments(listOf(point(0.0, 0.0)))

        val rebuilt = repository.rebuildVisitedSegments(
            listOf(point(0.0, 0.0, timestampMillis = 0L), point(0.0, 0.0045, timestampMillis = 30_000L)),
        )

        assertTrue(rebuilt)
        repository.observeVisitedStreetRuns().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    private fun way(id: Long, vararg nodes: LatLon) = OsmWay(id = id, nodes = nodes.toList())

    private fun point(latitude: Double, longitude: Double, timestampMillis: Long = 0L) = LocationPoint(
        latitude = latitude,
        longitude = longitude,
        timestampMillis = timestampMillis,
        accuracyMeters = 5f,
    )
}
