package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.testutil.FakeOverpassClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StreetCoverageRepositoryImplTest {

    private lateinit var overpassClient: FakeOverpassClient
    private lateinit var repository: StreetCoverageRepositoryImpl

    @Before
    fun setUp() {
        overpassClient = FakeOverpassClient()
        repository = StreetCoverageRepositoryImpl(overpassClient)
    }

    @Test
    fun noPointsReturnsEmptyAndDoesNotFetch() = runTest {
        val runs = repository.getVisitedStreetRuns(emptyList())

        assertTrue(runs.isEmpty())
        assertEquals(0, overpassClient.fetchCallCount)
    }

    @Test
    fun waySegmentFarFromEveryPointIsExcluded() = runTest {
        // 5 equally-spaced nodes, ~111m apart at the equator
        overpassClient.ways = listOf(
            listOf(
                LatLon(0.0, 0.000), LatLon(0.0, 0.001), LatLon(0.0, 0.002),
                LatLon(0.0, 0.003), LatLon(0.0, 0.004),
            ),
        )

        val runs = repository.getVisitedStreetRuns(listOf(point(0.0, 0.000)))

        // only the segment touching (0,0) is within 30m; the rest of the way is ~111m+ away
        assertEquals(1, runs.size)
        assertEquals(listOf(LatLon(0.0, 0.000), LatLon(0.0, 0.001)), runs.single().points)
    }

    @Test
    fun nonConsecutiveVisitedSegmentsProduceSeparateRunsConsecutiveOnesMerge() = runTest {
        overpassClient.ways = listOf(
            listOf(
                LatLon(0.0, 0.000), LatLon(0.0, 0.001), LatLon(0.0, 0.002),
                LatLon(0.0, 0.003), LatLon(0.0, 0.004),
            ),
        )
        val visitedPoints = listOf(point(0.0, 0.000), point(0.0, 0.003))

        val runs = repository.getVisitedStreetRuns(visitedPoints)

        assertEquals(2, runs.size)
        assertEquals(listOf(LatLon(0.0, 0.000), LatLon(0.0, 0.001)), runs[0].points)
        assertEquals(
            listOf(LatLon(0.0, 0.002), LatLon(0.0, 0.003), LatLon(0.0, 0.004)),
            runs[1].points,
        )
    }

    @Test
    fun aWayShorterThanTwoNodesProducesNoRuns() = runTest {
        overpassClient.ways = listOf(listOf(LatLon(0.0, 0.0)))

        val runs = repository.getVisitedStreetRuns(listOf(point(0.0, 0.0)))

        assertTrue(runs.isEmpty())
    }

    @Test
    fun repeatedCallsWithinTheCachedAreaDoNotRefetch() = runTest {
        overpassClient.ways = listOf(listOf(LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))

        repository.getVisitedStreetRuns(listOf(point(0.0, 0.0)))
        repository.getVisitedStreetRuns(listOf(point(0.0, 0.0)))
        repository.getVisitedStreetRuns(listOf(point(0.00001, 0.00001)))

        assertEquals(1, overpassClient.fetchCallCount)
    }

    @Test
    fun aPointOutsideTheCachedPaddedAreaTriggersARefetch() = runTest {
        overpassClient.ways = listOf(listOf(LatLon(0.0, 0.0), LatLon(0.0, 0.0001)))

        repository.getVisitedStreetRuns(listOf(point(0.0, 0.0)))
        // well beyond the ~0.01 degree cache padding around the first call's bounds
        repository.getVisitedStreetRuns(listOf(point(5.0, 5.0)))

        assertEquals(2, overpassClient.fetchCallCount)
    }

    private fun point(latitude: Double, longitude: Double) = LocationPoint(
        latitude = latitude,
        longitude = longitude,
        timestampMillis = 0L,
        accuracyMeters = 5f,
    )
}
