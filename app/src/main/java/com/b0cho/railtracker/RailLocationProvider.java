package com.b0cho.railtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.Task;

import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.ArrayList;
import java.util.Collections;

/**
 * RailLocationProvider
 * Delivers location updates using injected FusedLocationProvider.
 * Implements IMyLocationConsumer, so it can also feed OSM mapView with location updates.
 * Class caches last available location and sends an update request only when it is outdated acc. to set expiration time.
 */
public class RailLocationProvider implements IMyLocationProvider {
    public RailLocationProvider(Context context, FusedLocationProviderClient fusedLocationProviderClient, ISystemClock systemClock, Location lastLocation) {
        this(context, fusedLocationProviderClient, systemClock);
        mLastLocation = lastLocation;
    }

    @FunctionalInterface
    public interface ISystemClock {
        long getElapsedTimeNanos();
    }

    private final ISystemClock systemClock;
    private Location mLastLocation;
    private final FusedLocationProviderClient mFusedLocationProvider;
    private final Context mContext;

    private IMyLocationConsumer mMyLocationConsumer;
    private final long locationExpirationSec = 25;

    /**
     * NECESSARY PERMISSIONS FOR  RailLocationProvider
     */
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * INNER OPAQUE-CLASS FOR CUSTOMER CALLBACKS
     * Allows to call customer callback + update of RailLocationProvider mLastLocation
     */
    private class RailLocationProviderCallback extends LocationCallback {
        private final LocationCallback originalCallback;
        public RailLocationProviderCallback(LocationCallback callback) {
            originalCallback = callback;
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            originalCallback.onLocationAvailability(locationAvailability);
        }

        @Override
        public void onLocationResult(LocationResult locationResult) {
            mLastLocation = locationResult.getLastLocation();
            mMyLocationConsumer.onLocationChanged(mLastLocation, RailLocationProvider.this);
            originalCallback.onLocationResult(locationResult);
        }
    }

    /**
     * @param context Context of parent Activity
     * @param fusedLocationProviderClient Injection of FusedLocationProviderClient
     * @param systemClock Clock to be used for timestamps comparison
     */
    @SuppressLint("MissingPermission")
    public RailLocationProvider(@NonNull Context context, @NonNull FusedLocationProviderClient fusedLocationProviderClient, ISystemClock systemClock) {
        mContext = context;
        mFusedLocationProvider = fusedLocationProviderClient;
        this.systemClock = systemClock;
        mLastLocation = null;
    }

    /// IMyLocationProvider methods
    @Override
    public boolean startLocationProvider(IMyLocationConsumer locationConsumer) {
        if(locationConsumer == null)
            return false;
        mMyLocationConsumer = locationConsumer;
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

    /**
     * Checks if applicatiion (context) was granted with necessary permissions for Location Provider to work.
     * @param context Context of app
     * @return True, if app (context) has all necessary permissions. Otherwise false.
     */
    public static boolean checkPermissions(final Context context) {
        for(String permission: PERMISSIONS) {
            if(context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * Requests single location update, that is sent to locationCallback. If last location is not outdated, last cached location is returned.
     * @param locationCallback Customer LocationCallback to be called when location update is obtained.
     * @throws IllegalStateException thrown when necessary permissions are not granted.
     * @return A Task, if cached location is not available/valid and will be obtained. Null, if there is valid cached location that is sent to locationCallback.
     */
    @SuppressLint("MissingPermission")
    @Nullable
    public Task<Void> requestSingleLocationUpdate(LocationCallback locationCallback) throws IllegalStateException {
        // checking location permissions
        boolean isPermissionGranted = checkPermissions(mContext);

        if(!isPermissionGranted)
            throw new IllegalStateException("Location permissions not granted!");

        if(mLastLocation != null && !isLocationOutdated(mLastLocation)) {
            // Cached location is available and is not outdated
            locationCallback.onLocationResult(LocationResult.create(new ArrayList<>(Collections.singletonList(mLastLocation))));
            mMyLocationConsumer.onLocationChanged(mLastLocation, this);
            return null;
        } else {
            // Calling single request update
            return mFusedLocationProvider.requestLocationUpdates(
                    LocationRequest.create()
                        .setNumUpdates(1)
                        .setInterval(10 * 1000L)
                        .setSmallestDisplacement(100)
                        .setExpirationDuration(10 * 1000),
                    new RailLocationProviderCallback(locationCallback),
                    Looper.getMainLooper());
        }
    }

    /**
     * Requests frequent location updates, that are delivered to consumers (LiveData, mapview)
     * @param locationCallback - Location callback, that is executed each location update occurs
     * @param locationRequest - Location request to be executed.
     * @return Task, that is run to obtain location updates.
     * @throws IllegalStateException if, necessary permission is not granted
     */
    @SuppressLint("MissingPermission")
    @NonNull
    public Task<Void> requestLocationUpdates(LocationCallback locationCallback, LocationRequest locationRequest) throws IllegalStateException {
        // checking location permissions
        boolean isPermissionGranted = checkPermissions(mContext);

        if(!isPermissionGranted)
            throw new IllegalStateException("Location permissions not granted!");

        return mFusedLocationProvider.requestLocationUpdates(locationRequest,
                new RailLocationProviderCallback(locationCallback),
                Looper.getMainLooper());
    }

    /**
     * Checks, if provided Location is outdated in acc. to class expiration time
     * @param location to be checked
     * @return True, if location is outdated and new should be obtained. Otherwise false.
     */
    private boolean isLocationOutdated(final @NonNull Location location) {
        return systemClock.getElapsedTimeNanos() - location.getElapsedRealtimeNanos() > locationExpirationSec * 1_000_000_000L;
    }
}
