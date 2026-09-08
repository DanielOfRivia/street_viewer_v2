package io.github.DanielOfRivia.street_viewer_v2.domain

fun interface Clock {
    fun nowMillis(): Long
}
