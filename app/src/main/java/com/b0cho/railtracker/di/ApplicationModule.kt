package com.b0cho.railtracker.di

import android.content.Context
import androidx.room.Room
import com.b0cho.railtracker.AppDatabase
import com.b0cho.railtracker.ILocationProvider
import com.b0cho.railtracker.RailLocationProvider
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.MapTileProviderBasic
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {
    @Provides
    @Singleton
    fun provideApplicationDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "AppDatabase").build()
    }

    @Provides
    @Singleton
    fun provideRailLocationProvider(@ApplicationContext appContext: Context?): ILocationProvider {
        return RailLocationProvider(LocationServices.getFusedLocationProviderClient(appContext!!))
    }

    @Provides
    @Singleton
    fun provideMapTileProviderBasic(@ApplicationContext context: Context?): MapTileProviderBase {
        return MapTileProviderBasic(context)
    }

    // TODO: move all singleton providers to here
}