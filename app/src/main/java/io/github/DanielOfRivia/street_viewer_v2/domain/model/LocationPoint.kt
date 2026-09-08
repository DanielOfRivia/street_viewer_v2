package io.github.DanielOfRivia.street_viewer_v2.domain.model

data class LocationPoint(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float,
    val syncedAtMillis: Long? = null,
)
