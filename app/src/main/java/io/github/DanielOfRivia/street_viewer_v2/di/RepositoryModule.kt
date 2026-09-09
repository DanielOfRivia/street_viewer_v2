package io.github.DanielOfRivia.street_viewer_v2.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClient
import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClientImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.DataStoreTrackingPreferencesRepository
import io.github.DanielOfRivia.street_viewer_v2.data.repository.LocationPointRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.StreetCoverageRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.SyncRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.TrackingStatusRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.WorkManagerSyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.StreetCoverageRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
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

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl,
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        impl: WorkManagerSyncScheduler,
    ): SyncScheduler

    @Binds
    @Singleton
    abstract fun bindTrackingPreferencesRepository(
        impl: DataStoreTrackingPreferencesRepository,
    ): TrackingPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindOverpassClient(
        impl: OverpassClientImpl,
    ): OverpassClient

    @Binds
    @Singleton
    abstract fun bindStreetCoverageRepository(
        impl: StreetCoverageRepositoryImpl,
    ): StreetCoverageRepository
}
