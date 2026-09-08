package io.github.DanielOfRivia.street_viewer_v2.domain.repository

import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncResult

interface SyncRepository {
    suspend fun uploadPendingPoints(): SyncResult
}
