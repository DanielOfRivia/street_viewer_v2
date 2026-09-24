package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.UploadLocationsRequestDto
import io.github.DanielOfRivia.street_viewer_v2.data.remote.toDto
import io.github.DanielOfRivia.street_viewer_v2.di.RetentionWindowMillis
import io.github.DanielOfRivia.street_viewer_v2.di.SyncPageSize
import io.github.DanielOfRivia.street_viewer_v2.domain.Clock
import io.github.DanielOfRivia.street_viewer_v2.domain.model.SyncResult
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val api: LocationApi,
    private val locationPointRepository: LocationPointRepository,
    private val json: Json,
    private val clock: Clock,
    @param:SyncPageSize private val pageSize: Int,
    @param:RetentionWindowMillis private val retentionWindowMillis: Long,
) : SyncRepository {

    // The periodic and "sync now" workers have different unique-work names, so WorkManager
    // can run both at once. Without this, both read the same unsynced page before either
    // marks it synced, and the server receives that page twice.
    private val uploadMutex = Mutex()

    override suspend fun uploadPendingPoints(): SyncResult = uploadMutex.withLock {
        val result = uploadAllPendingPages()
        // Runs regardless of the upload outcome above: it only touches points that have
        // already been synced (in this run or an earlier one), so it's independent of
        // whether there was anything pending, or whether pending uploads just failed.
        // Never-synced points are exempt no matter their age -- only a successful upload
        // makes a point eligible for this cleanup, so nothing is lost before the server
        // has actually acknowledged it.
        locationPointRepository.deleteSyncedOlderThan(clock.nowMillis() - retentionWindowMillis)
        result
    }

    private suspend fun uploadAllPendingPages(): SyncResult {
        var totalUploaded = 0

        while (true) {
            val page = locationPointRepository.getUnsyncedPage(pageSize)
            if (page.isEmpty()) {
                return SyncResult.Success(totalUploaded)
            }

            val body = json.encodeToString(
                UploadLocationsRequestDto.serializer(),
                UploadLocationsRequestDto(page.map { it.toDto() }),
            ).toRequestBody("application/json".toMediaType())

            val response = try {
                api.uploadLocations(body)
            } catch (e: IOException) {
                return SyncResult.Failure(totalUploaded, e.message ?: "Network error")
            }

            if (!response.isSuccessful) {
                return SyncResult.Failure(totalUploaded, "Server returned HTTP ${response.code()}")
            }

            locationPointRepository.markSynced(page.map { it.id }, clock.nowMillis())
            totalUploaded += page.size
        }
    }
}
