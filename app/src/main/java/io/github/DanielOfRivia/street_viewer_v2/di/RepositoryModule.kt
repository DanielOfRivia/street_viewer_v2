package io.github.DanielOfRivia.street_viewer_v2.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.DanielOfRivia.street_viewer_v2.data.repository.LocationPointRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.TrackingStatusRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationPointRepository(
        impl: LocationPointRepositoryImpl,
    ): LocationPointRepository

    @Binds
    @Singleton
    abstract fun bindTrackingStatusRepository(
        impl: TrackingStatusRepositoryImpl,
    ): TrackingStatusRepository
}
