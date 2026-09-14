package io.github.DanielOfRivia.street_viewer_v2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float,
)

@Serializable
data class UploadLocationsRequestDto(
    val locations: List<LocationDto>,
)

@Serializable
data class LocationRecordDto(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float,
)
