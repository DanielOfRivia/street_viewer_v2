package io.github.DanielOfRivia.street_viewer_v2.domain.model

data class LocationPoint(
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float,
    val syncedAtMillis: Long? = null,
) {
    /**
     * Precise enough to draw as part of the track or match against streets. Coarser fixes (weak
     * signal, indoors, Wi-Fi/cell only) are still recorded and uploaded -- the server's stay
     * detection works at a 200 m scale, where they're what places the user during an indoor
     * stay -- but on the map they'd show up as spurious jumps off the actual street.
     */
    val isPrecise: Boolean get() = accuracyMeters <= PRECISE_ACCURACY_METERS

    companion object {
        const val PRECISE_ACCURACY_METERS = 50f
    }
}
