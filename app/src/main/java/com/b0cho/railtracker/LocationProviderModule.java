package com.b0cho.railtracker;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import androidx.preference.PreferenceManager;

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
    private final static String d_TAG = "LocationProviderModule: ";

    @Provides
    @Singleton
    ILocationProvider provideRailLocationProvider(@ApplicationContext Context appContext) {
        final long locationTimeout = Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(appContext)
                .getString(appContext.getString(R.string.location_timeout_key), "25"));

        Log.d(d_TAG, "Creating RailLocationProvider");
        Log.d(d_TAG, "Retrieved locationTimeout = " + locationTimeout);

        RailLocationProvider railLocationProvider = new RailLocationProvider(appContext,
                LocationServices.getFusedLocationProviderClient(appContext),
                SystemClock::elapsedRealtimeNanos);

        railLocationProvider.setLocationTimeoutSec(locationTimeout > 0 ? locationTimeout : 25);
        return railLocationProvider;
    }
}
