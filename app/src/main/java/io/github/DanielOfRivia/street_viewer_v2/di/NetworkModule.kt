package io.github.DanielOfRivia.street_viewer_v2.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.DanielOfRivia.street_viewer_v2.BuildConfig
import io.github.DanielOfRivia.street_viewer_v2.data.remote.LocationApi
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    private fun baseClientBuilder(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

    // Request/response bodies contain the user's movement history -- never log them in
    // release builds. Must be the LAST interceptor added on any client: OkHttp interceptors
    // added earlier see the request before ones added later modify it, so logging added
    // before e.g. the API key interceptor below would never show that header even though
    // it's genuinely still sent -- it'd just look like it's missing.
    private fun OkHttpClient.Builder.withDebugLoggingLast(): OkHttpClient.Builder = apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    // Logs the rest of each request, but never the backend's key itself --
                    // anyone with adb access could otherwise read it straight out of logcat.
                    redactHeader(API_KEY_HEADER)
                },
            )
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = baseClientBuilder().withDebugLoggingLast().build()

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        // Built independently from provideOkHttpClient's instance, not derived via
        // newBuilder(): that client is also injected into OverpassClientImpl, and Overpass --
        // an unrelated public service -- should never see this backend's key.
        val apiKey = BuildConfig.NGROK_SECURE_API_KEY
        val builder = baseClientBuilder()
        if (apiKey.isNotBlank()) {
            builder.addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().addHeader(API_KEY_HEADER, apiKey).build())
            }
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(builder.withDebugLoggingLast().build())
            .build()
    }

    @Provides
    @Singleton
    fun provideLocationApi(retrofit: Retrofit): LocationApi = retrofit.create(LocationApi::class.java)

    @Provides
    @SyncPageSize
    fun provideSyncPageSize(): Int = 500

    @Provides
    @RetentionWindowMillis
    fun provideRetentionWindowMillis(): Long = TimeUnit.DAYS.toMillis(30)

    @Provides
    @OverpassBaseUrl
    fun provideOverpassBaseUrl(): String = "https://overpass-api.de/api/interpreter"

    private const val API_KEY_HEADER = "X-API-Key"
}
