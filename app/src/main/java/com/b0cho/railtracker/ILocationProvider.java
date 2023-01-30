package com.b0cho.railtracker;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;

/**
 * Interface for location provider
 */
public interface ILocationProvider {
    /**
     * Should check if app (context) was granted with necessary permissions for Location Provider to work.
     * @param context Context of app
     * @return True, if app (context) has all necessary permissions. Otherwise false.
     */
    boolean hasPermissions(@NonNull Context context);

    /**
     * Should request frequent location updates, that are delivered to consumers (LiveData, mapview)
     * @param locationCallback - Location callback, that is executed each location update occurs
     * @param locationRequest - Location request to be executed.
     * @return Task, that is run to obtain location updates.
     */
    @NonNull
    Task<Void> requestLocationUpdates(LocationRequest locationRequest, LocationCallback locationCallback) throws Exception;

    /**
     * Should remove location updates, connected with given locationCallback
     * @param locationCallback of location requests to be removed
     * @return Task, that is run to remove location updates.
     */
    @NonNull
    Task<Void> removeLocationUpdates(LocationCallback locationCallback);

    /**
     * @return Callback, that can be used for requestLocationUpdates, to feed set consumers
     */
    @NonNull
    LocationCallback getUpdateConsumersCallback();

}
