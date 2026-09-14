package io.github.DanielOfRivia.street_viewer_v2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VisitedPlaceDto(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    @SerialName("arrival_time") val arrivalTime: Long,
    @SerialName("departure_time") val departureTime: Long,
    @SerialName("point_count") val pointCount: Int,
    val address: String? = null,
    val businesses: List<BusinessDto> = emptyList(),
)

@Serializable
data class BusinessDto(
    val name: String,
    val types: List<String> = emptyList(),
    @SerialName("place_id") val placeId: String,
)
