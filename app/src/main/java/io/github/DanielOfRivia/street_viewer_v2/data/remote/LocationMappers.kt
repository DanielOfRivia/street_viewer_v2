package io.github.DanielOfRivia.street_viewer_v2.data.remote

import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.LocationDto
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint

fun LocationPoint.toDto() = LocationDto(
    latitude = latitude,
    longitude = longitude,
    timestamp = timestampMillis,
    accuracy = accuracyMeters,
)
