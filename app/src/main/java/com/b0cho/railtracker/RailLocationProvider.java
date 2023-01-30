package com.b0cho.railtracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.Task;

import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.HashSet;
import java.util.Set;

/**
 * RailLocationProvider
 * Delivers location updates using injected FusedLocationProvider from Google Play services.
 * Implements IMyLocationProvider, so it can also feed OSM mapViews with location updated.
 * Object requests location updates and returns tasks, but also feeds given consumers.
 */
public class RailLocationProvider implements IMyLocationProvider, ILocationProvider {
    private final FusedLocationProviderClient mFusedLocationProvider;
    private Location mLastLocation;
    private final Set<IMyLocationConsumer> mLocationConsumers;
    private final LocationCallback updateConsumersCallback;

    private final Set<LocationCallback> pendingRequests;

    public RailLocationProvider(FusedLocationProviderClient fusedLocationProviderClient) {
        mFusedLocationProvider = fusedLocationProviderClient;
        mLastLocation = null;
        mLocationConsumers = new HashSet<>();
        updateConsumersCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateConsumers(locationResult.getLastLocation());
            }
        };

        pendingRequests = new HashSet<>();
    }

    /**
     * NECESSARY PERMISSIONS FOR  RailLocationProvider
     */
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * Checks if applicatiion (context) was granted with necessary permissions for Location Provider to work.
     * @param context Context of app
     * @return True, if app (context) has all necessary permissions. Otherwise false.
     */
    @Override
    public boolean hasPermissions(final @NonNull Context context) {
        for(String permission: PERMISSIONS) {
            if(context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    /**
     * Requests frequent location updates, that are delivered to given callback.
     * Consumers are NOT updated automatically with obtained location .
     * @param locationRequest to be executed
     * @param locationCallback to be invoked when location is obtained. For updating consumers, use NewRailLocationProvider.updateConsumersCallback.
     * @return Task, that is run to obtain location updates.
     * @throws SecurityException if necessary permissions are not granted.
     */
    @NonNull
    @Override
    public Task<Void> requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) throws SecurityException {
        return mFusedLocationProvider.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()).addOnSuccessListener(unused -> pendingRequests.add(locationCallback));
    }

    /**
     * Removed location updates for given callback
     * @param locationCallback to be removed
     * @return Task, that is run to remove updated.
     */
    @NonNull
    @Override
    public Task<Void> removeLocationUpdates(LocationCallback locationCallback) {
        return mFusedLocationProvider.removeLocationUpdates(locationCallback).addOnSuccessListener(unused -> pendingRequests.remove(locationCallback));
    }

    private void updateConsumers(Location location) {
        mLastLocation = location;
        for (IMyLocationConsumer locationConsumer :
                mLocationConsumers) {
            locationConsumer.onLocationChanged(mLastLocation, this);
        }
    }

    /**
     * Adds location consumer.
     * @param myLocationConsumer to be added for feeding. Should not be null.
     * @return false if myLocationConsumer was null. Otherwise true.
     */
    @Override
    public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
        if(myLocationConsumer == null)
            return false;
        mLocationConsumers.add(myLocationConsumer);
        return true;
    }

    /**
     * Clears all location consumers.
     */
    @Override
    public void stopLocationProvider() {
        mLocationConsumers.clear();
    }

    /**
     * Returns last cached location.
     * Consumers are updated automatically with valid location.
     * @return last cached location if given. Otherwise null.
     * @throws SecurityException if necessary permissions are not granted.
     */
    @Override
    public Location getLastKnownLocation() throws SecurityException {
        mFusedLocationProvider.getLastLocation().addOnSuccessListener(this::updateConsumers);
        return mLastLocation;
    }

    @Override
    public void destroy() {
        for (LocationCallback pendingRequest :
                pendingRequests) {
            mFusedLocationProvider.removeLocationUpdates(pendingRequest);
        }
    }

    @NonNull
    public LocationCallback getUpdateConsumersCallback() {
        return updateConsumersCallback;
    }
}
