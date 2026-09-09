package io.github.DanielOfRivia.street_viewer_v2.data.remote

import io.github.DanielOfRivia.street_viewer_v2.data.remote.dto.OverpassResponseDto
import io.github.DanielOfRivia.street_viewer_v2.di.OverpassBaseUrl
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class OverpassClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    @param:OverpassBaseUrl private val baseUrl: String,
) : OverpassClient {

    override suspend fun fetchHighways(bounds: MapBounds): List<List<LatLon>> =
        withContext(Dispatchers.IO) {
            // This overlay is decorative, not core data: any failure (offline, Overpass down,
            // fair-use throttling, malformed response) just means no colored streets for this
            // session rather than a crash -- the raw track and everything else still works.
            try {
                val request = Request.Builder()
                    .url(baseUrl)
                    .post(buildQuery(bounds).toRequestBody("text/plain".toMediaType()))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    json.decodeFromString(OverpassResponseDto.serializer(), body)
                        .elements
                        .mapNotNull { it.geometry }
                        .map { nodes -> nodes.map { LatLon(it.lat, it.lon) } }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    private fun buildQuery(bounds: MapBounds): String =
        "[out:json][timeout:25];" +
            "way[\"highway\"~\"^($HIGHWAY_TYPES)$\"]" +
            "(${bounds.south},${bounds.west},${bounds.north},${bounds.east});" +
            "out geom;"

    companion object {
        // Real streets, deliberately excluding footway/cycleway/path/steps/service/track --
        // this app colors streets, not every path OSM tags as a "highway".
        private const val HIGHWAY_TYPES =
            "motorway|trunk|primary|secondary|tertiary|unclassified|residential|living_street"
    }
}
