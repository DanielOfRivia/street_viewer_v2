package io.github.DanielOfRivia.street_viewer_v2.domain.model

sealed interface SyncResult {
    data class Success(val uploadedCount: Int) : SyncResult
    data class Failure(val uploadedCount: Int, val reason: String) : SyncResult
}
