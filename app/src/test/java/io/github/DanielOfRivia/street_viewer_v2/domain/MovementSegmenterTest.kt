package io.github.DanielOfRivia.street_viewer_v2.domain

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementSegmenterTest {

    @Test
    fun emptyPointsProducesNoSegments() {
        assertEquals(emptyList<List<LocationPoint>>(), MovementSegmenter.segmentByMovement(emptyList(), emptyList()))
    }

    @Test
    fun noStaysKeepsAllPointsAsOneSegment() {
        val points = listOf(point(0L), point(10_000L), point(20_000L))

        val segments = MovementSegmenter.segmentByMovement(points, stays = emptyList())

        assertEquals(listOf(points), segments)
    }

    @Test
    fun pointsEntirelyInsideOneStayProduceNoSegments() {
        val points = listOf(point(0L), point(1_000L), point(2_000L))
        val stay = stay(arrival = 0L, departure = 2_000L)

        val segments = MovementSegmenter.segmentByMovement(points, listOf(stay))

        assertTrue(segments.isEmpty())
    }

    @Test
    fun aStayInTheMiddleSplitsMovementBeforeAndAfterAnchoredToThePlaceOnEachSide() {
        val before = listOf(point(0L), point(1_000L))
        val duringStay = listOf(point(2_000L), point(3_000L))
        val after = listOf(point(10_000L), point(11_000L))
        val stay = stay(arrival = 2_000L, departure = 3_000L, lat = 1.0, lon = 1.0)

        val segments = MovementSegmenter.segmentByMovement(before + duringStay + after, listOf(stay))

        // The segment leading into the stay gets the place's own coordinates appended as its
        // last point (rather than stopping short at whichever real fix came right before
        // arrival), and the segment leaving it gets those same coordinates prepended.
        assertEquals(
            listOf(
                before + anchor(1.0, 1.0, 1_000L),
                listOf(anchor(1.0, 1.0, 10_000L)) + after,
            ),
            segments,
        )
    }

    @Test
    fun aMovementSegmentWithNoAdjacentStayIsNotAnchored() {
        val points = listOf(point(0L), point(1_000L))

        val segments = MovementSegmenter.segmentByMovement(points, stays = emptyList())

        assertEquals(listOf(points), segments)
    }

    @Test
    fun pointsExactlyAtTheArrivalOrDepartureBoundaryAreTreatedAsPartOfTheStay() {
        val arrival = point(1_000L)
        val departure = point(2_000L)
        val stay = stay(arrival = 1_000L, departure = 2_000L)

        val segments = MovementSegmenter.segmentByMovement(listOf(arrival, departure), listOf(stay))

        assertTrue(segments.isEmpty())
    }

    @Test
    fun outOfOrderInputIsSortedBeforeSegmenting() {
        val a = point(0L)
        val b = point(10_000L)

        val segments = MovementSegmenter.segmentByMovement(listOf(b, a), stays = emptyList())

        assertEquals(listOf(listOf(a, b)), segments)
    }

    @Test
    fun multipleStaysProduceAnAnchoredSegmentForEachMovementRunBetweenThem() {
        val leg1 = listOf(point(0L))
        val atStayA = listOf(point(1_500L))
        val leg2 = listOf(point(5_000L))
        val atStayB = listOf(point(6_500L))
        val leg3 = listOf(point(9_000L))
        val stayA = stay(arrival = 1_000L, departure = 2_000L, lat = 1.0, lon = 1.0)
        val stayB = stay(arrival = 6_000L, departure = 7_000L, lat = 2.0, lon = 2.0)

        val segments = MovementSegmenter.segmentByMovement(
            leg1 + atStayA + leg2 + atStayB + leg3,
            listOf(stayA, stayB),
        )

        assertEquals(
            listOf(
                leg1 + anchor(1.0, 1.0, 0L),
                listOf(anchor(1.0, 1.0, 5_000L)) + leg2 + anchor(2.0, 2.0, 5_000L),
                listOf(anchor(2.0, 2.0, 9_000L)) + leg3,
            ),
            segments,
        )
    }

    private fun point(timestampMillis: Long) = LocationPoint(
        id = 1,
        latitude = 43.6532,
        longitude = -79.3832,
        timestampMillis = timestampMillis,
        accuracyMeters = 5f,
    )

    private fun anchor(lat: Double, lon: Double, timestampMillis: Long) = LocationPoint(
        latitude = lat,
        longitude = lon,
        timestampMillis = timestampMillis,
        accuracyMeters = 0f,
    )

    private fun stay(arrival: Long, departure: Long, lat: Double = 43.6532, lon: Double = -79.3832) = VisitedPlace(
        id = 1,
        latitude = lat,
        longitude = lon,
        arrivalTimeMillis = arrival,
        departureTimeMillis = departure,
        pointCount = 5,
        address = null,
        businesses = emptyList(),
    )
}
