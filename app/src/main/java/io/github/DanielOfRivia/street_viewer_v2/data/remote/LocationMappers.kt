package io.github.DanielOfRivia.street_viewer_v2.data.remote

import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.BusinessDto
import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.LocationDto
import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.LocationRecordDto
import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.VisitedPlaceDto
import io.github.DanielOfRivia.street_viewer_v2.domain.model.Business
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace

fun LocationPoint.toDto() = LocationDto(
    latitude = latitude,
    longitude = longitude,
    timestamp = timestampMillis,
    accuracy = accuracyMeters,
)

fun LocationRecordDto.toDomain() = LocationPoint(
    id = id,
    latitude = latitude,
    longitude = longitude,
    timestampMillis = timestamp,
    accuracyMeters = accuracy,
)

fun VisitedPlaceDto.toDomain() = VisitedPlace(
    id = id,
    latitude = latitude,
    longitude = longitude,
    arrivalTimeMillis = arrivalTime,
    departureTimeMillis = departureTime,
    pointCount = pointCount,
    address = address,
    businesses = businesses.map { it.toDomain() },
)

fun BusinessDto.toDomain() = Business(
    name = name,
    types = types,
    placeId = placeId,
)
