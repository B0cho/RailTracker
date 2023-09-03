package com.b0cho.railtracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import dagger.hilt.android.AndroidEntryPoint;

// MAIN ACTIVITY
@AndroidEntryPoint
public class MainActivity extends OSMMapViewActivity {
    private MainActivityViewModel mainActivityViewModel;
    private FloatingActionButton myLocationButton;
    private final static String d_TAG = "MainActivity: ";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        Toolbar toolbar = findViewById(R.id.mapViewToolbar);
        setSupportActionBar(toolbar);

        // setting listeners and callbacks
        myLocationButton = findViewById(R.id.myLocationActionButton);

        osmMapViewVM.isLocationFollowed().observe(this, follow -> {
            if(follow) {
                myLocationButton.setFocusableInTouchMode(true);
                myLocationButton.requestFocus();
            } else {
                myLocationButton.clearFocus();
                myLocationButton.setFocusableInTouchMode(false);
            }
        });

        // single location update + center on new location
        myLocationButton.setOnClickListener(this::onMyLocationButtonClick);

        // manual starting frequent location update
        myLocationButton.setOnLongClickListener(this::onMyLocationButtonLongClick);

        // add my location button
        addMyLocationButton.setOnClickListener(this::onAddMyLocationButtonClick);
    }

    private void onMyLocationButtonClick(View view) {
        moveToCurrentLocation();
    }

    /**
     * Sending request for 5 mins long location updates
     */
    private boolean onMyLocationButtonLongClick(View view) {
        if (!osmMapViewVM.locationProvider.hasPermissions(getApplicationContext())) {
            requestLocationPermission();
            return true;
        }

        // sending request and getting result task
        try {
            // getting values for request from preferences
            final long intervalSecs = getLocationIntervalSecs();
            final long expirationSecs = 5 * 60;
            Log.d(d_TAG, "Preparing manual location updates request: interval: " + intervalSecs + " expiration: " + expirationSecs);

            ILocationProvider locationProvider = osmMapViewVM.locationProvider;
            final Task<Void> requestTask = locationProvider.removeLocationUpdates(osmMapViewVM.manualUpdatesCallback)
                    .continueWithTask(task -> {
                        if(!task.isSuccessful())
                            Log.d(d_TAG, "Removal of pending manual updates failed!");
                        LocationRequest locationRequest = LocationRequest.create();
                        return locationProvider.requestLocationUpdates(locationRequest, osmMapViewVM.manualUpdatesCallback);
                    });

            // setting reaction, when task is successful
            requestTask.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Snackbar.make(findViewById(R.id.activityMain), "Location tracking for 5 mins activated", Snackbar.LENGTH_SHORT).show();
                    osmMapViewVM.setFollowingLocation(true);
                    osmMapViewVM.setShowCurrentLocation(true);
                } else
                    Toast.makeText(this, "Request for location updates failed!", Toast.LENGTH_SHORT).show();
            });
            return true;
        } catch (Exception e) {
            Snackbar.make(findViewById(R.id.activityMain), "Location access permissions are lost! Location service is no longer available", Snackbar.LENGTH_SHORT).show();
            return true;
        }
    }
}