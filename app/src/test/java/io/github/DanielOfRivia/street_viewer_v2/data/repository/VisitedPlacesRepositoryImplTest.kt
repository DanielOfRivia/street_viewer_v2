package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class VisitedPlacesRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: VisitedPlacesRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .build()
            .create(LocationApi::class.java)
        repository = VisitedPlacesRepositoryImpl(api, Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun successfulResponseIsMappedToDomainPlacesIncludingBusinesses() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    [
                        {
                            "id": 7,
                            "latitude": 43.6532,
                            "longitude": -79.3832,
                            "arrival_time": 1700000000000,
                            "departure_time": 1700003600000,
                            "point_count": 42,
                            "address": "123 Main St",
                            "businesses": [
                                {"name": "Cafe X", "types": ["cafe", "food"], "place_id": "abc123"}
                            ]
                        }
                    ]
                """.trimIndent(),
            ),
        )

        val places = repository.getVisitedPlaces(1_699_999_000_000L, 1_700_004_000_000L)

        assertEquals(1, places.size)
        val place = places.single()
        assertEquals(7L, place.id)
        assertEquals(43.6532, place.latitude, 0.0001)
        assertEquals(1700000000000L, place.arrivalTimeMillis)
        assertEquals(1700003600000L, place.departureTimeMillis)
        assertEquals(42, place.pointCount)
        assertEquals("123 Main St", place.address)
        assertEquals(1, place.businesses.size)
        assertEquals("Cafe X", place.businesses.single().name)
        assertEquals("abc123", place.businesses.single().placeId)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue(recorded.target?.contains("/api/v1/visited-places") == true)
    }

    @Test
    fun nullAddressAndMissingBusinessesDefaultSensibly() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    [{"id":1,"latitude":0.0,"longitude":0.0,"arrival_time":0,"departure_time":1,"point_count":1}]
                """.trimIndent(),
            ),
        )

        val places = repository.getVisitedPlaces(0L, 1L)

        val place = places.single()
        assertEquals(null, place.address)
        assertTrue(place.businesses.isEmpty())
    }

    @Test
    fun nonSuccessResponseReturnsEmptyListNotAnException() = runTest {
        server.enqueue(MockResponse(code = 401))

        val places = repository.getVisitedPlaces(0L, 1L)

        assertTrue(places.isEmpty())
    }

    @Test
    fun malformedResponseBodyReturnsEmptyListNotAnException() = runTest {
        server.enqueue(MockResponse(code = 200, body = "not json"))

        val places = repository.getVisitedPlaces(0L, 1L)

        assertTrue(places.isEmpty())
    }
}
