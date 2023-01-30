package com.b0cho.railtracker;

import android.content.Context;

import com.google.android.gms.location.LocationServices;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class LocationProviderModule {
    @Provides
    @Singleton
    ILocationProvider provideNewRailLocationProvider(@ApplicationContext Context appContext) {
        return new RailLocationProvider(LocationServices.getFusedLocationProviderClient(appContext));
    }
}
