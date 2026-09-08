package io.github.DanielOfRivia.street_viewer_v2.data.local

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationPoint

fun LocationPointEntity.toDomain() = LocationPoint(
    id = id,
    latitude = latitude,
    longitude = longitude,
    timestampMillis = timestamp,
    accuracyMeters = accuracy,
    syncedAtMillis = syncedAtMillis,
)

fun LocationPoint.toEntity() = LocationPointEntity(
    id = id,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestampMillis,
    accuracy = accuracyMeters,
    syncedAtMillis = syncedAtMillis,
)
