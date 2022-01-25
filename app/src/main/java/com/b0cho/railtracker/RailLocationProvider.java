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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.ArrayList;

public class RailLocationProvider implements IMyLocationProvider {
    private Location mLastLocation;
    private final FusedLocationProviderClient mFusedLocationProvider;
    private final Context mContext;
    private IMyLocationConsumer mMyLocationConsumer;
    private ArrayList<IRailLocationListener> mLocationListeners;
    private static final long mLocationTimestampDelay_s = 5L;

    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public interface IRailLocationListener {
        void onLocationRequestFinished(Location location);
    }

    // CONSTRUCTORS
    @SuppressLint("MissingPermission")
    public RailLocationProvider(@NonNull Context context, @NonNull FusedLocationProviderClient fusedLocationProviderClient) {
        mContext = context;
        mFusedLocationProvider = fusedLocationProviderClient;
        mLastLocation = null;
        mLocationListeners = new ArrayList<>();
    }

    private void callbackAllListeners(Location location) {
        for (IRailLocationListener listener: mLocationListeners) {
            listener.onLocationRequestFinished(location);
        }
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
        /*return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;*/
        if(context == null)
            throw new IllegalArgumentException();
        for(String permission: PERMISSIONS) {
            if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void requestLocationOnce() throws IllegalStateException {
        if(!checkPermissions(mContext))
            throw new IllegalStateException("Location permissions not granted!");
        if(mLastLocation == null || !isLocationOutdated(mLastLocation))
            callbackAllListeners(mLastLocation);
        else
            mFusedLocationProvider.requestLocationUpdates(REQUEST_LOCATION_ONCE(), CALLBACK_ALL(), Looper.getMainLooper());
    }

    public void addLocationListener(@NonNull IRailLocationListener railLocationListener) {
        mLocationListeners.add(railLocationListener);
    }

    @NonNull
    private static LocationRequest REQUEST_LOCATION_ONCE() {
        return LocationRequest.create().setNumUpdates(1).setInterval(10 * 1000L);
    }

    @NonNull
    private LocationCallback CALLBACK_ALL() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    mLastLocation = locationResult.getLastLocation();
                    callbackAllListeners(mLastLocation);
                }
            }
        };
    }

    private static boolean isLocationOutdated(final Location location) throws IllegalArgumentException {
        if(location != null) {
            return SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos() > mLocationTimestampDelay_s * 1000000000L;
        } else
            throw new IllegalArgumentException();
    }
}
