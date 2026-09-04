package io.github.DanielOfRivia.street_viewer_v2.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.DanielOfRivia.street_viewer_v2.data.local.AppDatabase
import io.github.DanielOfRivia.street_viewer_v2.data.local.LocationPointDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "street_viewer.db").build()

    @Provides
    fun provideLocationPointDao(database: AppDatabase): LocationPointDao =
        database.locationPointDao()
}
