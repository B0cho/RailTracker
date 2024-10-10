package com.b0cho.railtracker;

import static com.b0cho.railtracker.App.logTAG;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;

// MAIN ACTIVITY
@AndroidEntryPoint
public class MainActivity extends OSMMapViewActivity implements ConfirmDialog.Listener {
    private MainActivityVM mainActivityVM;
    private FloatingActionButton myLocationButton;
    private FloatingActionButton addMyLocationButton;
    private GeoPoint mapviewCenterPoint;
    private Double mapviewZoom;
    private Boolean mapviewShowCurLocation;
    private Integer selectedTileSourceKey;
    private Set<Integer> selectedOverlaysKeys;
    private Boolean MyLocationsOverlayShown = false;
    private MyLocationInfoWindow myLocationInfoWindow;

    private final static String REMOVE_LOCATION_DIALOG = "remove_dialog";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainActivityVM = new ViewModelProvider(this).get(MainActivityVM.class);

        Toolbar toolbar = findViewById(R.id.mapViewToolbar);
        setSupportActionBar(toolbar);

        if(savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.mapFragmentContainer, new OSMMapViewFragment())
                    .commit();
        }

        // setting listeners and callbacks
        myLocationButton = findViewById(R.id.myLocationActionButton);
        addMyLocationButton = findViewById(R.id.addMyLocationActionButton);

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

        // observing values from viewmodel
        osmMapViewVM.getCenterPoint().observe(this, centerPoint -> {
            if(mapviewCenterPoint == null)
                mapviewCenterPoint = new GeoPoint(centerPoint);
            else
                mapviewCenterPoint.setCoords(centerPoint.getLatitude(), centerPoint.getLongitude());
        });
        osmMapViewVM.getZoom().observe(this, zoom -> mapviewZoom = zoom);
        osmMapViewVM.isLocationShown().observe(this, isShown -> mapviewShowCurLocation = isShown);
        osmMapViewVM.getSelectedTileSourceId().observe(this, selectedId -> selectedTileSourceKey = selectedId);
        osmMapViewVM.getSelectedOverlaysIds().observe(this, selection -> selectedOverlaysKeys = selection);
        osmMapViewVM.isMyLocationsOverlayShown().observe(this, isShown -> MyLocationsOverlayShown = isShown);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // setting listeners for MyLocationInfoWindow interactions
        try {
            myLocationInfoWindow = ((OSMMapViewFragment) Objects.requireNonNull(getSupportFragmentManager()
                    .findFragmentById(R.id.mapFragmentContainer)))
                    .getMyLocationInfoWindow()
                    .setOnBubbleClickListener(
                            (view, location) -> {
                                // TODO: open LocationViewer
                                return Unit.INSTANCE;
                            }
                    )
                    .setOnBubbleLongClickListener(
                            (view, location) -> {
                                // create and show dialog about location removal
                                ConfirmDialog.Companion.newInstance(
                                        REMOVE_LOCATION_DIALOG,
                                        location.getName(),
                                        "Are you sure to remove " + location.getName() + "?",
                                        "Confirm",
                                        "Cancel",
                                        false,
                                        location
                                ).show(getSupportFragmentManager().beginTransaction(), "remove_location");
                                return true;
                            }
                    );
        } catch (NullPointerException e) {
            Log.e(logTAG, this + "onStart: Failed to set listeners for MyLocationInfoWindow " + e);
        }
    }

    private void onMyLocationButtonClick(View view) {
        moveToCurrentLocation();
    }

    private void onAddMyLocationButtonClick(View view) {
        final Intent createNewLocation = new Intent(this, LocationEditorActivity.class);
        createNewLocation
                .putExtra(LocationEditorActivity.LAUNCH_LOCATION_PICKER, true)
                .putExtra(
                        OSMMapViewActivity.MAPVIEW_STATE,
                        new MapViewDTO(
                                mapviewCenterPoint,
                                mapviewZoom,
                                mapviewShowCurLocation,
                                selectedTileSourceKey,
                                new ArrayList<>(selectedOverlaysKeys),
                                MyLocationsOverlayShown
                                ));
        startActivity(createNewLocation);
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
            Log.d(logTAG, this + "onMyLocationButtonLongClick: Preparing manual location updates request: interval: " + intervalSecs + " expiration: " + expirationSecs);

            ILocationProvider locationProvider = osmMapViewVM.locationProvider;
            final Task<Void> requestTask = locationProvider.removeLocationUpdates(osmMapViewVM.manualUpdatesCallback)
                    .continueWithTask(task -> {
                        if(!task.isSuccessful())
                            Log.d(logTAG, this + "onMyLocationButtonLongClick: Removal of pending manual updates failed!");
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

    @Override
    public void onDialogConfirm(@org.jetbrains.annotations.Nullable String dialogTag, @org.jetbrains.annotations.Nullable Parcelable relatedObject) {
        if(dialogTag != null) {
            switch (dialogTag) {
                case REMOVE_LOCATION_DIALOG:
                    Disposable removeLocationDisposable = osmMapViewVM.removeLocation((MyLocation) relatedObject)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(new DisposableCompletableObserver() {
                                @Override
                                public void onComplete() {
                                    Toast.makeText(MainActivity.this, ((MyLocation) Objects.requireNonNull(relatedObject)).getName() + " removed.", Toast.LENGTH_SHORT).show();
                                    myLocationInfoWindow.close();
                                }

                                @Override
                                public void onError(@NonNull Throwable e) {
                                    Log.e(logTAG, this + " cast of dialog related object failed");
                                    Toast.makeText(MainActivity.this, "Removal failed!", Toast.LENGTH_SHORT).show();
                                }
                            });
                    break;
            }
        }
    }
}