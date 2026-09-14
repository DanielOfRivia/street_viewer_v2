package io.github.DanielOfRivia.street_viewer_v2.domain.model

data class VisitedPlace(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val arrivalTimeMillis: Long,
    val departureTimeMillis: Long,
    val pointCount: Int,
    val address: String?,
    val businesses: List<Business>,
)

data class Business(
    val name: String,
    val types: List<String>,
    val placeId: String,
)
