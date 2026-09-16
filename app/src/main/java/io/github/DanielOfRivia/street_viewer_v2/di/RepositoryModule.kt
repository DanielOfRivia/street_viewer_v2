package io.github.DanielOfRivia.street_viewer_v2.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClient
import io.github.DanielOfRivia.street_viewer_v2.data.remote.OverpassClientImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.DataStoreTrackingPreferencesRepository
import io.github.DanielOfRivia.street_viewer_v2.data.repository.LocationHistoryRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.LocationPointRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.SyncRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.TrackingStatusRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.VisitedPlacesRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.VisitedStreetCoverageRepositoryImpl
import io.github.DanielOfRivia.street_viewer_v2.data.repository.WorkManagerSyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationHistoryRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.LocationPointRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.SyncScheduler
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingPreferencesRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.TrackingStatusRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedPlacesRepository
import io.github.DanielOfRivia.street_viewer_v2.domain.repository.VisitedStreetCoverageRepository
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
    abstract fun bindVisitedStreetCoverageRepository(
        impl: VisitedStreetCoverageRepositoryImpl,
    ): VisitedStreetCoverageRepository

    @Binds
    @Singleton
    abstract fun bindLocationHistoryRepository(
        impl: LocationHistoryRepositoryImpl,
    ): LocationHistoryRepository

    @Binds
    @Singleton
    abstract fun bindVisitedPlacesRepository(
        impl: VisitedPlacesRepositoryImpl,
    ): VisitedPlacesRepository
}
