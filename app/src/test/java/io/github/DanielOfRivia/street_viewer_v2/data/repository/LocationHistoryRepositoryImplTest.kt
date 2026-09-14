package io.github.DanielOfRivia.street_viewer_v2.data.repository

import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import io.github.DanielOfRivia.street_viewer_v2.domain.model.LocationHistoryResult
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

class LocationHistoryRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: LocationHistoryRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .build()
            .create(LocationApi::class.java)
        repository = LocationHistoryRepositoryImpl(api, Json { ignoreUnknownKeys = true })
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun successfulResponseIsMappedToDomainPoints() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    [
                        {"id":1,"latitude":43.6532,"longitude":-79.3832,"timestamp":1700000000000,"accuracy":6.4},
                        {"id":2,"latitude":43.6540,"longitude":-79.3820,"timestamp":1700000030000,"accuracy":5.1}
                    ]
                """.trimIndent(),
            ),
        )

        val result = repository.getLocationsInRange(1_699_999_999_000L, 1_700_000_060_000L)

        check(result is LocationHistoryResult.Success)
        assertEquals(2, result.points.size)
        assertEquals(43.6532, result.points[0].latitude, 0.0001)
        assertEquals(1700000000000L, result.points[0].timestampMillis)
        assertEquals(6.4f, result.points[0].accuracyMeters)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue(recorded.target?.contains("start=1699999999000") == true)
        assertTrue(recorded.target?.contains("end=1700000060000") == true)
    }

    @Test
    fun emptyArrayIsSuccessNotFailure() = runTest {
        server.enqueue(MockResponse(code = 200, body = "[]"))

        val result = repository.getLocationsInRange(0L, 1L)

        assertEquals(LocationHistoryResult.Success(emptyList()), result)
    }

    @Test
    fun nonSuccessResponseIsAFailureWithTheStatusCode() = runTest {
        server.enqueue(MockResponse(code = 401))

        val result = repository.getLocationsInRange(0L, 1L)

        check(result is LocationHistoryResult.Failure)
        assertTrue(result.reason.contains("401"))
    }

    @Test
    fun malformedResponseBodyIsAFailureNotAThrow() = runTest {
        server.enqueue(MockResponse(code = 200, body = "not json"))

        val result = repository.getLocationsInRange(0L, 1L)

        assertTrue(result is LocationHistoryResult.Failure)
    }
}
