package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.domain.Clock

class FakeClock(var currentMillis: Long = 0L) : Clock {
    override fun nowMillis(): Long = currentMillis
}
