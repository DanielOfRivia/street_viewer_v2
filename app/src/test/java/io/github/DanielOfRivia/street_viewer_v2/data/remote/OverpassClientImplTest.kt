package io.github.DanielOfRivia.street_viewer_v2.data.remote

import io.github.DanielOfRivia.street_viewer_v2.domain.model.LatLon
import io.github.DanielOfRivia.street_viewer_v2.domain.model.MapBounds
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OverpassClientImplTest {

    private val server = MockWebServer()
    private lateinit var client: OverpassClientImpl

    @Before
    fun setUp() {
        server.start()
        client = OverpassClientImpl(
            okHttpClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
            baseUrl = server.url("/interpreter").toString(),
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun postsABoundingBoxQueryAndParsesWayGeometry() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """
                    {"elements":[
                        {"type":"way","id":1,"geometry":[{"lat":43.65,"lon":-79.38},{"lat":43.66,"lon":-79.37}]}
                    ]}
                """.trimIndent(),
            ),
        )

        val ways = client.fetchHighways(MapBounds(south = 43.6, west = -79.4, north = 43.7, east = -79.3))

        assertEquals(listOf(listOf(LatLon(43.65, -79.38), LatLon(43.66, -79.37))), ways)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        // Overpass's documented format: the query is a "data" form field, not a raw body --
        // sending it as raw text gets a real 406 from Apache's content negotiation (confirmed
        // on-device against the real API; MockWebServer alone wouldn't have caught this since
        // it accepts any body/content-type unlike the real server).
        assertEquals("application/x-www-form-urlencoded", recorded.headers["Content-Type"]?.substringBefore(";"))
        val body = recorded.body?.utf8().orEmpty()
        assertTrue(body.startsWith("data="))
        assertTrue(body.contains("43.6") && body.contains("-79.4") && body.contains("43.7") && body.contains("-79.3"))
        assertTrue(body.contains("highway"))
        assertTrue(recorded.headers["User-Agent"]?.contains("street_viewer_v2") == true)
    }

    @Test
    fun elementsWithoutGeometryAreSkipped() = runTest {
        server.enqueue(
            MockResponse(
                code = 200,
                body = """{"elements":[{"type":"node","id":1}]}""",
            ),
        )

        val ways = client.fetchHighways(MapBounds(0.0, 0.0, 0.0, 0.0))

        assertTrue(ways.isEmpty())
    }

    @Test
    fun nonSuccessResponseReturnsEmptyListNotAnException() = runTest {
        server.enqueue(MockResponse(code = 500))

        val ways = client.fetchHighways(MapBounds(0.0, 0.0, 0.0, 0.0))

        assertTrue(ways.isEmpty())
    }

    @Test
    fun malformedResponseBodyReturnsEmptyListNotAnException() = runTest {
        server.enqueue(MockResponse(code = 200, body = "not json"))

        val ways = client.fetchHighways(MapBounds(0.0, 0.0, 0.0, 0.0))

        assertTrue(ways.isEmpty())
    }
}
