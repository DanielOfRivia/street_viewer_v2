package io.github.DanielOfRivia.street_viewer_v2.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun pointExactlyOnTheSegmentIsZeroDistance() {
        // segment running north along the same longitude; midpoint sits exactly on it
        val distance = GeoMath.distanceToSegmentMeters(
            pointLat = 43.6536, pointLon = -79.3832,
            aLat = 43.6532, aLon = -79.3832,
            bLat = 43.6540, bLon = -79.3832,
        )
        assertTrue(distance < 0.5)
    }

    @Test
    fun pointBesideTheMiddleOfTheSegmentMatchesPerpendicularDistance() {
        // ~0.0003 degrees of longitude at this latitude is roughly 24m
        val distance = GeoMath.distanceToSegmentMeters(
            pointLat = 43.6536, pointLon = -79.3829,
            aLat = 43.6532, aLon = -79.3832,
            bLat = 43.6540, bLon = -79.3832,
        )
        assertEquals(24.0, distance, 2.0)
    }

    @Test
    fun pointBeyondEndpointBUsesDistanceToBNotTheInfiniteLine() {
        val distanceBeyondB = GeoMath.distanceToSegmentMeters(
            pointLat = 43.6545, pointLon = -79.3832,
            aLat = 43.6532, aLon = -79.3832,
            bLat = 43.6540, bLon = -79.3832,
        )
        val distanceToBItself = GeoMath.distanceToSegmentMeters(
            pointLat = 43.6545, pointLon = -79.3832,
            aLat = 43.6540, aLon = -79.3832,
            bLat = 43.6540, bLon = -79.3832,
        )
        assertEquals(distanceToBItself, distanceBeyondB, 0.5)
    }

    @Test
    fun farAwayPointIsWellOutsideAThirtyMeterRadius() {
        val distance = GeoMath.distanceToSegmentMeters(
            pointLat = 43.7000, pointLon = -79.3832,
            aLat = 43.6532, aLon = -79.3832,
            bLat = 43.6540, bLon = -79.3832,
        )
        assertTrue(distance > 30.0)
    }

    @Test
    fun zeroLengthSegmentIsJustDistanceToThatPoint() {
        val distance = GeoMath.distanceToSegmentMeters(
            pointLat = 43.6536, pointLon = -79.3832,
            aLat = 43.6532, aLon = -79.3832,
            bLat = 43.6532, bLon = -79.3832,
        )
        // roughly 44m of latitude difference (0.0004 deg * 111_320 m/deg)
        assertEquals(44.5, distance, 2.0)
    }

    @Test
    fun distanceMetersMatchesKnownLatitudeOnlyDifference() {
        // same longitude, 0.0004 deg of latitude apart -> ~44.5m, same value the
        // point-to-segment zero-length case above independently confirms
        val distance = GeoMath.distanceMeters(43.6532, -79.3832, 43.6536, -79.3832)
        assertEquals(44.5, distance, 2.0)
    }

    @Test
    fun distanceMetersOfAPointToItselfIsZero() {
        assertEquals(0.0, GeoMath.distanceMeters(43.6532, -79.3832, 43.6532, -79.3832), 0.001)
    }

    @Test
    fun bearingDegreesDueNorthIsZero() {
        val bearing = GeoMath.bearingDegrees(43.6532, -79.3832, 43.6540, -79.3832)
        assertEquals(0.0, bearing, 0.5)
    }

    @Test
    fun bearingDegreesDueSouthIsOneEighty() {
        val bearing = GeoMath.bearingDegrees(43.6540, -79.3832, 43.6532, -79.3832)
        assertEquals(180.0, bearing, 0.5)
    }

    @Test
    fun bearingDegreesDueEastIsNinety() {
        val bearing = GeoMath.bearingDegrees(43.6532, -79.3832, 43.6532, -79.3820)
        assertEquals(90.0, bearing, 1.0)
    }

    @Test
    fun bearingDegreesDueWestIsTwoSeventy() {
        val bearing = GeoMath.bearingDegrees(43.6532, -79.3820, 43.6532, -79.3832)
        assertEquals(270.0, bearing, 1.0)
    }
}
