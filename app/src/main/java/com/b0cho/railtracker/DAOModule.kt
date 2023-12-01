package com.b0cho.railtracker

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(ActivityRetainedComponent::class)
object DAOModule {
    @Provides
    @ActivityRetainedScoped
    fun provideMyLocationDao(appDatabase: AppDatabase): MyLocationDao {
        return appDatabase.provideMyLocationDao()
    }
}