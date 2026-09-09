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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Request/response bodies contain the user's movement history -- never log them
        // in release builds.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY },
            )
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // The API key is scoped to this client, not the shared okHttpClient above: that one
        // is also injected into OverpassClientImpl, and Overpass -- an unrelated public
        // service -- should never see this backend's key.
        val apiKey = BuildConfig.NGROK_SECURE_API_KEY
        val backendClient = if (apiKey.isBlank()) {
            okHttpClient
        } else {
            okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .addHeader(API_KEY_HEADER, apiKey)
                            .build(),
                    )
                }
                .build()
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(backendClient)
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
