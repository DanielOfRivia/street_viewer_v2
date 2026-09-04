package io.github.DanielOfRivia.street_viewer_v2.domain.model

data class TrackingStatus(
    val isActive: Boolean = false,
    val stopReason: TrackingStopReason? = null,
)

enum class TrackingStopReason {
    USER_REQUESTED,
    PERMISSION_MISSING,
    START_FOREGROUND_FAILED,
    LOCATION_REQUEST_FAILED,
}
