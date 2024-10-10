package com.b0cho.railtracker;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public final class App extends Application {
    public final static String logTAG = "KB/";
    @Override
    public void onCreate() {
        super.onCreate();

    }
}
