package com.b0cho.railtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RailLocationProvider implements IMyLocationProvider {
    private Location mLastLocation;
    private final FusedLocationProviderClient mFusedLocationProvider;
    private final Context mContext;
    private IMyLocationConsumer mMyLocationConsumer;
    private static final long mLocationTimestampDelay_s = 10;

    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // CONSTRUCTORS
    @SuppressLint("MissingPermission")
    public RailLocationProvider(@NonNull Context context, @NonNull FusedLocationProviderClient fusedLocationProviderClient) {
        mContext = context;
        mFusedLocationProvider = fusedLocationProviderClient;
        mLastLocation = null;
    }

    /// IMyLocationProvider methods
    @Override
    public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
        if(myLocationConsumer == null)
            return false;
        mMyLocationConsumer = myLocationConsumer;
        return true;
    }

    @Override
    public void stopLocationProvider() {
        mMyLocationConsumer = null;
    }

    @Override
    public Location getLastKnownLocation() {
        return mLastLocation;
    }

    @Override
    public void destroy() {

    }

    public boolean checkPermissions(final Context context) throws IllegalArgumentException {
        if(context == null)
            throw new IllegalArgumentException();
        for(String permission: PERMISSIONS) {
            if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void requestLocationUpdate(LocationRequest locationRequest, LocationCallback locationCallback) throws IllegalStateException {
        if(!checkPermissions(mContext))
            throw new IllegalStateException("Location permissions not granted!");
        if(mLastLocation != null && !isLocationOutdated(mLastLocation)) {
            locationCallback.onLocationResult(LocationResult.create(new ArrayList<>(Collections.singletonList(mLastLocation))));
            mMyLocationConsumer.onLocationChanged(mLastLocation, this);
        } else
            Toast.makeText(mContext, "Location old", Toast.LENGTH_SHORT).show();
            mFusedLocationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    .addOnSuccessListener(unused -> mFusedLocationProvider.getLastLocation()
                            .addOnCompleteListener(task -> {
                                mLastLocation = task.getResult();
                                mMyLocationConsumer.onLocationChanged(mLastLocation, this);
                            }));
    }

    public void requestLocationOnce(LocationCallback locationCallback) throws IllegalStateException {
        requestLocationUpdate(REQUEST_LOCATION_ONCE(), locationCallback);
    }

    @NonNull
    private static LocationRequest REQUEST_LOCATION_ONCE() {
        return LocationRequest.create().setNumUpdates(1).setInterval(10 * 1000L);
    }

    private static boolean isLocationOutdated(final Location location) {
        if(location != null) {
            return SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos() > mLocationTimestampDelay_s * 1000000000L;
        } else
            return true;
    }
}
