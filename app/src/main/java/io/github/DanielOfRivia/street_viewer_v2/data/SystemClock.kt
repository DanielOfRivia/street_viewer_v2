package io.github.DanielOfRivia.street_viewer_v2.data

import io.github.DanielOfRivia.street_viewer_v2.domain.Clock
import javax.inject.Inject

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
