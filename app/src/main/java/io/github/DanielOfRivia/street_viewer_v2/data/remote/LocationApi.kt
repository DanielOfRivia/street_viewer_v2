package io.github.DanielOfRivia.street_viewer_v2.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface LocationApi {
    // The body is pre-serialized JSON rather than a typed @Body DTO: the only available
    // kotlinx.serialization Retrofit converter (com.jakewharton.retrofit2, unmaintained
    // since 2023) is compiled with a .kotlin_module that this project's Kotlin 2.2.10 (K2)
    // compiler fails to read from the classpath -- an "unresolved reference" with no
    // indication a stale converter is the cause. Serializing manually with the same Json
    // instance sidesteps needing a converter at all for this single-endpoint API.
    @POST("api/v1/locations")
    suspend fun uploadLocations(@Body body: RequestBody): Response<Void>

    // Same reasoning applies to the response side: no converter is registered, so this
    // returns the raw body for the caller to decode manually with the same Json instance.
    @GET("api/v1/locations")
    suspend fun getLocations(
        @Query("start") startMillis: Long,
        @Query("end") endMillis: Long,
    ): Response<ResponseBody>

    @GET("api/v1/visited-places")
    suspend fun getVisitedPlaces(
        @Query("start") startMillis: Long,
        @Query("end") endMillis: Long,
    ): Response<ResponseBody>
}
