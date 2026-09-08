package io.github.DanielOfRivia.street_viewer_v2.testutil

import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import okhttp3.RequestBody
import retrofit2.Response

class FakeLocationApi : LocationApi {

    var responseProvider: (Int) -> Response<Void> = { Response.success(null) }
    var onUpload: suspend (Int) -> Unit = {}
    var callCount: Int = 0
        private set

    override suspend fun uploadLocations(body: RequestBody): Response<Void> {
        callCount++
        onUpload(callCount)
        return responseProvider(callCount)
    }
}
