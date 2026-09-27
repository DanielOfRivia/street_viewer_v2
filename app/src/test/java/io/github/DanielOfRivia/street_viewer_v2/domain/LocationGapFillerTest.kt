package io.github.DanielOfRivia.street_viewer_v2.domain

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LocationGapFillerTest {

    @Test
    fun fewerThanTwoPointsIsReturnedUnchanged() {
        val single = listOf(point(0.0, -79.3832, timestampMillis = 0L))
        assertSame(single, LocationGapFiller.fillGaps(single))
        assertEquals(emptyList<LocationPoint>(), LocationGapFiller.fillGaps(emptyList()))
    }

    @Test
    fun pointsAlreadyWithinTheMaxSegmentDistanceGetNoInterpolation() {
        // ~22m apart (0.0002 deg longitude at the equator), well under 30m
        val points = listOf(
            point(0.0, 0.0000, timestampMillis = 0L),
            point(0.0, 0.0002, timestampMillis = 10_000L),
        )

        val filled = LocationGapFiller.fillGaps(points)

        assertEquals(2, filled.size)
    }

    @Test
    fun ninetyMeterGapWithinNinetySecondsAddsExactlyTwoInterpolatedPoints() {
        // The spec's own example: 90m apart -> divided into 3 segments -> 2 new points, at
        // 30m and 60m from the first point. At the equator, 90m of longitude is ~0.000808 deg.
        val a = point(0.0, 0.0, timestampMillis = 0L)
        val b = point(0.0, 0.000808, timestampMillis = 60_000L)

        val filled = LocationGapFiller.fillGaps(listOf(a, b))

        assertEquals(4, filled.size)
        assertEquals(a, filled[0])
        assertEquals(b, filled[3])

        val distAtoFirst = GeoMath.distanceMeters(a.latitude, a.longitude, filled[1].latitude, filled[1].longitude)
        val distAtoSecond = GeoMath.distanceMeters(a.latitude, a.longitude, filled[2].latitude, filled[2].longitude)
        assertEquals(30.0, distAtoFirst, 1.0)
        assertEquals(60.0, distAtoSecond, 1.0)

        // interpolated timestamps land proportionally between a and b's times too
        assertEquals(20_000L, filled[1].timestampMillis)
        assertEquals(40_000L, filled[2].timestampMillis)
    }

    @Test
    fun gapAtOrAboveNinetySecondsIsNeverFilledRegardlessOfDistance() {
        val a = point(0.0, 0.0, timestampMillis = 0L)
        val b = point(0.0, 0.000808, timestampMillis = 90_000L) // exactly 90s -- "less than 90s" excludes this

        val filled = LocationGapFiller.fillGaps(listOf(a, b))

        assertEquals(2, filled.size)
    }

    @Test
    fun aLargeGapAfterTrackingWasOffIsNotBridgedWithInventedPoints() {
        // e.g. tracking stopped, phone moved a few km, tracking resumed -- slow on average
        val a = point(0.0, 0.0, timestampMillis = 0L)
        val b = point(0.0, 0.03, timestampMillis = 3_600_000L) // an hour later, ~3.3km away

        val filled = LocationGapFiller.fillGaps(listOf(a, b))

        assertEquals(2, filled.size)
    }

    @Test
    fun outOfOrderInputIsSortedBeforeProcessing() {
        val a = point(0.0, 0.0, timestampMillis = 0L)
        val b = point(0.0, 0.0001, timestampMillis = 10_000L)

        val filled = LocationGapFiller.fillGaps(listOf(b, a))

        assertEquals(a, filled.first())
        assertEquals(b, filled.last())
    }

    @Test
    fun multipleConsecutivePairsAreEachFilledIndependently() {
        val points = listOf(
            point(0.0, 0.000000, timestampMillis = 0L),
            point(0.0, 0.000808, timestampMillis = 60_000L), // 90m gap, filled
            point(0.0, 0.000908, timestampMillis = 70_000L), // ~11m gap, not filled
        )

        val filled = LocationGapFiller.fillGaps(points)

        assertEquals(5, filled.size) // 3 original + 2 interpolated for the first pair only
        assertEquals(points[2], filled.last())
    }

    @Test
    fun drivingPointsAreDroppedEntirely() {
        // 30s apart, ~500m each -> ~60 km/h
        val points = (0..3).map { i -> point(0.0, 0.0045 * i, timestampMillis = 30_000L * i) }

        assertEquals(emptyList<LocationPoint>(), LocationGapFiller.fillGaps(points))
    }

    @Test
    fun walkEndsAreKeptButTheDriveBetweenThemIsNeitherKeptNorBridged() {
        val walkStart = point(0.0, 0.0000, timestampMillis = 0L)
        val walkEnd = point(0.0, 0.0002, timestampMillis = 30_000L) // ~22m in 30s, walking
        val driving = point(0.0, 0.0047, timestampMillis = 60_000L) // ~500m in 30s
        val parked = point(0.0, 0.0092, timestampMillis = 89_000L) // ~500m in 29s
        val walkAgain = point(0.0, 0.0094, timestampMillis = 119_000L) // ~22m in 30s

        val filled = LocationGapFiller.fillGaps(listOf(walkStart, walkEnd, driving, parked, walkAgain))

        assertEquals(listOf(walkStart, walkEnd, parked, walkAgain), filled)
    }

    @Test
    fun subwayRideIsNotColouredEvenWithALoneFixAtAStation() {
        val beforeEntering = point(0.0, 0.0000, timestampMillis = 0L)
        val enteringStation = point(0.0, 0.0002, timestampMillis = 30_000L)
        // Underground for 5 min, one fix at a station mid-ride ~3.3km on (~40 km/h)
        val midRide = point(0.0, 0.0302, timestampMillis = 330_000L)
        // Another 5 min, surfacing another ~3.3km on
        val surfacing = point(0.0, 0.0602, timestampMillis = 630_000L)
        val walkingAway = point(0.0, 0.0604, timestampMillis = 660_000L)

        val filled = LocationGapFiller.fillGaps(
            listOf(beforeEntering, enteringStation, midRide, surfacing, walkingAway),
        )

        assertEquals(listOf(beforeEntering, enteringStation, surfacing, walkingAway), filled)
    }

    @Test
    fun customSpeedLimitIsRespected() {
        // ~22m in 5s -> ~16 km/h, e.g. cycling
        val points = listOf(
            point(0.0, 0.0000, timestampMillis = 0L),
            point(0.0, 0.0002, timestampMillis = 5_000L),
        )

        assertEquals(2, LocationGapFiller.fillGaps(points).size)
        assertEquals(0, LocationGapFiller.fillGaps(points, maxSpeedKmh = 10.0).size)
    }

    private fun point(latitude: Double, longitude: Double, timestampMillis: Long) = LocationPoint(
        id = 1,
        latitude = latitude,
        longitude = longitude,
        timestampMillis = timestampMillis,
        accuracyMeters = 5f,
    )
}
