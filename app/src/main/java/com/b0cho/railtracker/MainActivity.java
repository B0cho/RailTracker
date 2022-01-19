package com.b0cho.railtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.Set;

// MAIN ACTIVITY
public class MainActivity extends AppCompatActivity implements Tracking.OnFragmentInteractionListener {
    // location provider converter: fusedLocationProvider -> IMyLocationProvider
    static class LocationProviderConverter implements IMyLocationProvider {
        private Location lastLocation = null;
        private IMyLocationConsumer consumer;
        @Override
        public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
            consumer = myLocationConsumer;
            return true;
        }

        @Override
        public void stopLocationProvider() {

        }

        @Override
        public Location getLastKnownLocation() {
            return lastLocation;
        }

        @Override
        public void destroy() {

        }

        public void setLastLocation(Location lastLocation) {
            this.lastLocation = lastLocation;
            consumer.onLocationChanged(this.lastLocation, this);
        }
    }

    private Menu toolbarMenu = null;
    private Menu sourcesMenu = null;
    private Menu overlaysMenu = null;
    private Tracking trackingFragment;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationProviderConverter locationProviderConverter;
    private static final long locationTimestampDelay_s = 5L;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setting tracking fragment
        trackingFragment = Tracking.newInstance();

        // setting tracking to fragment container
        getSupportFragmentManager().beginTransaction().add(R.id.trackingFragmentContainer, trackingFragment).commit();

        // initializing location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationProviderConverter = new LocationProviderConverter();

        // setting listeners
        final FloatingActionButton myLocationButton = findViewById(R.id.myLocationActionButton);
        myLocationButton.setOnClickListener(view -> {
            // checking location permissions
            final boolean isLocationPermissionGranted = ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            // no location permission granted
            if (!isLocationPermissionGranted) {
                ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
                Toast.makeText(MainActivity.this, "Permission for location not granted!", Toast.LENGTH_SHORT).show();
                return;
            }

            // permission is OK, enabling my location
            // attempt to get last location
            fusedLocationClient.getLastLocation().addOnCompleteListener(MainActivity.this, task -> {
                if(task.isSuccessful() && trackingFragment.getLocationOverlay().enableMyLocation(locationProviderConverter)) {
                    final Location lastLocation = task.getResult();

                    // checking location timestamp
                    if(lastLocation == null || isLocationOutdated(lastLocation)) {
                        // location is too old or invalid
                        Toast.makeText(MainActivity.this, "Location is too old!", Toast.LENGTH_SHORT).show();

                        // TODO: obtain actual position
                        fusedLocationClient.requestLocationUpdates(
                                LocationRequest.create().setNumUpdates(1).setInterval(10 * 1000L), // 10 * 1000L
                                new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        if(locationResult != null)
                                            centerFocusOnLocation(myLocationButton, locationResult.getLastLocation());
                                    }
                                },
                                Looper.getMainLooper());
                    } else
                        centerFocusOnLocation(myLocationButton, lastLocation);
                } else {
                    // TODO: add handling, when last location / service is invalid
                    Toast.makeText(MainActivity.this, "Problem with last location service!", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean isLocationOutdated(Location location) {
        if(location != null)
        {
            return SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos() > locationTimestampDelay_s * 1000000000L;
        }
        else
            throw new IllegalArgumentException();
    }

    private void centerFocusOnLocation(FloatingActionButton myLocationButton, Location location) {
        // button handling - position found
        myLocationButton.setFocusableInTouchMode(true);
        myLocationButton.requestFocus();

        // mapView handling
        locationProviderConverter.setLastLocation(location);
        trackingFragment.getLocationOverlay().enableFollowLocation();

        // TODO: add handling position on map -> draw position
        trackingFragment.showLocationOverlay(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);

        // assigning menus
        toolbarMenu = menu;
        sourcesMenu = toolbarMenu.findItem(R.id.tileSourceSubmenu).getSubMenu();
        overlaysMenu = toolbarMenu.findItem(R.id.overlaysSubmenu).getSubMenu();

        // setting sources
        initMenuSources();
        try {
            setTilesSourceSelection(trackingFragment.getCurrentTileSourceKey());
        } catch (Exception e) {
            // TODO: add handling
        }

        // setting overlays
        initMenuOverlays();
        try {
            setOverlaysSelection(trackingFragment.getCurrentOverlaysKeys());
        } catch (Exception e) {
            // TODO: add handling
        }
        return true;
    }

    private void setOverlaysSelection(Set<Integer> currentOverlaysKeys) throws Exception {
        /*final MenuItem menuItem = overlaysMenu.findItem(key);
        if (menuItem == null)
            throw new Exception("Invalid selection key - tiles source");
        menuItem.setChecked(true);*/
        Exception e = null;
        for (Integer key : currentOverlaysKeys) {
            final MenuItem menuItem = overlaysMenu.findItem(key);
            if (menuItem == null) {
                e = new Exception("Invalid selection key - overlays");
                continue;
            }
            menuItem.setChecked(true);
        }
        if (e != null)
            throw e;
    }

    private void initMenuOverlays() {
        final SparseArray<Pair<String, Overlay>> overlaySparseArray = trackingFragment.getOfferedOverlays();
        for (int i = 0; i < overlaySparseArray.size(); i++) {
            final MenuItem menuItem = overlaysMenu.add(R.id.overlaySourceGroup, overlaySparseArray.keyAt(i), Menu.NONE, overlaySparseArray.valueAt(i).first);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menuItem.setCheckable(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // tiles
        if (item.getGroupId() == R.id.tileSourceGroup && Tracking.offeredSources().get(item.getItemId()) != null) {
            try {
                trackingFragment.setTileSource(item.getItemId());
            } catch (Exception e) {
//                TODO: add handling
            }
            item.setChecked(true);
            return true;
        }

        // overlays
        if (item.getGroupId() == R.id.overlaySourceGroup && trackingFragment.getOfferedOverlays().get(item.getItemId()) != null) {
            try {
                trackingFragment.setOverlays(item.getItemId());
            } catch (Exception e) {
                // TODO: add handling
            }
            item.setChecked(!item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initMenuSources() {
        for (int i = 0; i < Tracking.offeredSources().size(); i++) {
            final MenuItem menuItem = sourcesMenu.add(R.id.tileSourceGroup, Tracking.offeredSources().keyAt(i), Menu.NONE, Tracking.offeredSources().valueAt(i).name());
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menuItem.setCheckable(true);
        }
        sourcesMenu.setGroupCheckable(R.id.tileSourceGroup, true, true);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void initializeMapViewListeners(MapView mapView) {
        // listener for losing location tracking after moving the map
        final FloatingActionButton myLocationButton = findViewById(R.id.myLocationActionButton);
        mapView.setOnTouchListener((view1, motionEvent) -> {
            super.onTouchEvent(motionEvent);
            if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                myLocationButton.setFocusableInTouchMode(false);
                myLocationButton.clearFocus();
                trackingFragment.getLocationOverlay().disableFollowLocation();
            }
            return false;
        });
    }

    private void setTilesSourceSelection(final int key) throws Exception {
        final MenuItem menuItem = sourcesMenu.findItem(key);
        if (menuItem == null)
            throw new Exception("Invalid selection key - tiles source");
        menuItem.setChecked(true);
    }
}
