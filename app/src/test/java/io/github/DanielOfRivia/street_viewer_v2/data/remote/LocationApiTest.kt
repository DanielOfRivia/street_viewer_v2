package io.github.DanielOfRivia.street_viewer_v2.data.remote

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class LocationApiTest {

    private val server = MockWebServer()
    private lateinit var api: LocationApi

    @Before
    fun setUp() {
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .build()
        api = retrofit.create(LocationApi::class.java)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun postsToTheDocumentedPathWithTheDocumentedBody() = runTest {
        server.enqueue(MockResponse(code = 200))

        val body = """{"locations":[{"latitude":43.6532,"longitude":-79.3832,"timestamp":1700000000000,"accuracy":6.4}]}"""
        val response = api.uploadLocations(body.toRequestBody("application/json".toMediaType()))

        assertTrue(response.isSuccessful)
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/locations", recorded.target)
        assertTrue(recorded.body?.utf8()?.contains("\"latitude\":43.6532") == true)
    }

    @Test
    fun anyTwoXxCodeIsSuccessful() = runTest {
        server.enqueue(MockResponse(code = 204))

        val response = api.uploadLocations("{}".toRequestBody("application/json".toMediaType()))

        assertTrue(response.isSuccessful)
    }

    @Test
    fun nonTwoXxResponseIsSurfacedAsUnsuccessfulNotAnException() = runTest {
        server.enqueue(MockResponse(code = 500))

        val response = api.uploadLocations("{}".toRequestBody("application/json".toMediaType()))

        assertFalse(response.isSuccessful)
        assertEquals(500, response.code())
    }
}
