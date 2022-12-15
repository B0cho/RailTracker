package com.b0cho.railtracker;

import android.content.Context;
import android.os.SystemClock;

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
    ILocationProvider provideRailLocationProvider(@ApplicationContext Context appContext) {
        return new RailLocationProvider(appContext, LocationServices.getFusedLocationProviderClient(appContext), SystemClock::elapsedRealtimeNanos);
    }
}
