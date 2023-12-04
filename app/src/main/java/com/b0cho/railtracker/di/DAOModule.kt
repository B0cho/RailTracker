package com.b0cho.railtracker.di

import com.b0cho.railtracker.AppDatabase
import com.b0cho.railtracker.MyLocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object DAOModule {
    @Provides
    @ActivityRetainedScoped
    fun provideMyLocationDao(appDatabase: AppDatabase): MyLocationDao {
        return appDatabase.provideMyLocationDao()
    }
}