package io.github.DanielOfRivia.street_viewer_v2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OverpassResponseDto(
    val elements: List<OverpassElementDto> = emptyList(),
)

@Serializable
data class OverpassElementDto(
    val id: Long,
    val type: String,
    val geometry: List<OverpassNodeDto>? = null,
)

@Serializable
data class OverpassNodeDto(
    val lat: Double,
    val lon: Double,
)
