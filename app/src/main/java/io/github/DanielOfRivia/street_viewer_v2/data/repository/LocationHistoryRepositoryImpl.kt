package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.LocationRecordDto
import io.github.DanielOfRivia.street_viewer_v2.data.remote.toDomain
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class LocationHistoryRepositoryImpl @Inject constructor(
    private val api: LocationApi,
    private val json: Json,
) : LocationHistoryRepository {

    override suspend fun getLocationsInRange(startMillis: Long, endMillis: Long): LocationHistoryResult {
        return try {
            val response = api.getLocations(startMillis, endMillis)
            if (!response.isSuccessful) {
                return LocationHistoryResult.Failure("Server returned HTTP ${response.code()}")
            }
            val body = response.body()?.string()
                ?: return LocationHistoryResult.Failure("Empty response")
            val records = json.decodeFromString(ListSerializer(LocationRecordDto.serializer()), body)
            LocationHistoryResult.Success(records.map { it.toDomain() })
        } catch (e: IOException) {
            LocationHistoryResult.Failure(e.message ?: "Network error")
        } catch (e: SerializationException) {
            LocationHistoryResult.Failure("Malformed response")
        }
    }
}
