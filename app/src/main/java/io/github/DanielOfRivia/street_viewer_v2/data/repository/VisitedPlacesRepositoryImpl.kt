package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.VisitedPlaceDto
import io.github.DanielOfRivia.street_viewer_v2.data.remote.toDomain
import io.github.DanielOfRivia.street_viewer_v2.domain.model.VisitedPlace
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedPlacesRepository
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject

class VisitedPlacesRepositoryImpl @Inject constructor(
    private val api: LocationApi,
    private val json: Json,
) : VisitedPlacesRepository {

    override suspend fun getVisitedPlaces(startMillis: Long, endMillis: Long): List<VisitedPlace> {
        return try {
            val response = api.getVisitedPlaces(startMillis, endMillis)
            if (!response.isSuccessful) return emptyList()
            val body = response.body()?.string() ?: return emptyList()
            json.decodeFromString(ListSerializer(VisitedPlaceDto.serializer()), body).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
