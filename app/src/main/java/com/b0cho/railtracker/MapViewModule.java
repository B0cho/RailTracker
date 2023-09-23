package com.b0cho.railtracker;

import android.content.Context;

import androidx.core.content.ContextCompat;

import org.osmdroid.views.overlay.CopyrightOverlay;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;

@Module
@InstallIn(ActivityComponent.class)
public class MapViewModule {
    @Provides
    public CopyrightOverlay provideCopyrightOverlay(@ActivityContext Context activityContext) {
        final int copyrightColor = ContextCompat.getColor(activityContext, R.color.copyrightText);
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(activityContext);
        copyrightOverlay.setOffset(10, 10);
        copyrightOverlay.setTextColor(copyrightColor);
        return copyrightOverlay;
    }

    @Provides
    public OverlayCopyrightOverlay provideOverlayCopyrightOverlay(@ActivityContext Context activityContext) {
        final int copyrightColor = ContextCompat.getColor(activityContext, R.color.copyrightText);
        OverlayCopyrightOverlay overlayCopyright = new OverlayCopyrightOverlay(activityContext);
        overlayCopyright.setOffset(10, 50);
        overlayCopyright.setTextColor(copyrightColor);
        return overlayCopyright;
    }
}
