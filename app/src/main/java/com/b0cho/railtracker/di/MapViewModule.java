package com.b0cho.railtracker.di;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.b0cho.railtracker.CopyrightOverlay;
import com.b0cho.railtracker.R;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ActivityContext;

@Module
@InstallIn(ActivityComponent.class)
public class MapViewModule {
    @Provides
    public org.osmdroid.views.overlay.CopyrightOverlay provideCopyrightOverlay(@ActivityContext Context activityContext) {
        final int copyrightColor = ContextCompat.getColor(activityContext, R.color.copyrightText);
        org.osmdroid.views.overlay.CopyrightOverlay copyrightOverlay = new org.osmdroid.views.overlay.CopyrightOverlay(activityContext);
        copyrightOverlay.setOffset(10, 10);
        copyrightOverlay.setTextColor(copyrightColor);
        return copyrightOverlay;
    }

    @Provides
    public CopyrightOverlay provideOverlayCopyrightOverlay(@ActivityContext Context activityContext) {
        final int copyrightColor = ContextCompat.getColor(activityContext, R.color.copyrightText);
        CopyrightOverlay overlayCopyright = new CopyrightOverlay(activityContext);
        overlayCopyright.setOffset(10, 50);
        overlayCopyright.setTextColor(copyrightColor);
        return overlayCopyright;
    }
}
