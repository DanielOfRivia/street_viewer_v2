package io.github.DanielOfRivia.street_viewer_v2.data.remote

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface LocationApi {
    // The body is pre-serialized JSON rather than a typed @Body DTO: the only available
    // kotlinx.serialization Retrofit converter (com.jakewharton.retrofit2, unmaintained
    // since 2023) is compiled with a .kotlin_module that this project's Kotlin 2.2.10 (K2)
    // compiler fails to read from the classpath -- an "unresolved reference" with no
    // indication a stale converter is the cause. Serializing manually with the same Json
    // instance sidesteps needing a converter at all for this single-endpoint API.
    @POST("api/v1/locations")
    suspend fun uploadLocations(@Body body: RequestBody): Response<Void>
}
