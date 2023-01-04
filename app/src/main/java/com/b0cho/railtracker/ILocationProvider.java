package com.b0cho.railtracker;

import android.content.Context;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;

import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

/**
 * Interface for location provider
 */
public interface ILocationProvider extends IMyLocationProvider {
    /**
     * Requests single location update, that is sent to locationCallback. If last location is not outdated, last cached location is returned.
     * @param locationCallback Customer LocationCallback to be called when location update is obtained.
     * @return A Task, if cached location is not available/valid and will be obtained. Null, if there is valid cached location that is sent to locationCallback.
     */
    Task<Void> requestSingleLocationUpdate(LocationCallback locationCallback);

    /**
     * Requests frequent location updates, that are delivered to consumers (LiveData, mapview)
     * @param locationCallback - Location callback, that is executed each location update occurs
     * @param locationRequest - Location request to be executed.
     * @return Task, that is run to obtain location updates.
     */
    Task<Void> requestLocationUpdates(LocationCallback locationCallback, LocationRequest locationRequest);

    /**
     * Checks if applicatiion (context) was granted with necessary permissions for Location Provider to work.
     * @param context Context of app
     * @return True, if app (context) has all necessary permissions. Otherwise false.
     */
    boolean checkPermissions(Context context);

    /**
     * Sets timeout in seconds for location expiration
     * @param locationExpirationSec New value of location timeout in seconds (must be > 0 s)
     */
    void setLocationTimeoutSec(long locationExpirationSec);
}
