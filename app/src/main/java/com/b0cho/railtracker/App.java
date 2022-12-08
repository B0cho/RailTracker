package com.b0cho.railtracker;

import android.app.Application;
import android.os.SystemClock;

import com.google.android.gms.location.LocationServices;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public final class App extends Application {
    private RailLocationProvider railLocationProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        railLocationProvider = new RailLocationProvider(getApplicationContext(), LocationServices.getFusedLocationProviderClient(this), SystemClock::elapsedRealtimeNanos);

    }

    public RailLocationProvider getRailLocationProvider() {
        return railLocationProvider;
    }

}
