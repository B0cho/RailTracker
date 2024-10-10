package com.b0cho.railtracker;

import static com.b0cho.railtracker.App.logTAG;

import android.Manifest;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Map;

public class OSMMapViewActivity extends AppCompatActivity {
    protected OSMMapViewVM osmMapViewVM;
    protected Menu sourcesMenu;
    protected Menu overlaysMenu;

    // KEYS FOR INTENT EXTRAS
    public static final String MAPVIEW_STATE = "PARCELABLE_MAPVIEW_STATE";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        osmMapViewVM = new ViewModelProvider(this).get(OSMMapViewVM.class);

        // activity initial setting
        final MapViewDTO mapViewLaunchState = getIntent().getParcelableExtra(MAPVIEW_STATE, MapViewDTO.class);
        if(savedInstanceState == null && mapViewLaunchState != null) {
            try {
                // center point
                osmMapViewVM.setCenterPoint(mapViewLaunchState.center);

                // zoom
                if(mapViewLaunchState.zoom > 0)
                    osmMapViewVM.setZoom(mapViewLaunchState.zoom);
                else
                    Log.e(logTAG,this + "onCreate: Invalid intent extra mapview zoom = " + mapViewLaunchState.zoom + " - ignored.");

                // current position
                osmMapViewVM.setShowCurrentLocation(mapViewLaunchState.showCurrentPosition);

                // tile source key
                if(osmMapViewVM.offeredSourcesMenuInput().containsKey(mapViewLaunchState.selectedTileSourceKey))
                    osmMapViewVM.setTileSourceSelection(mapViewLaunchState.selectedTileSourceKey);
                else
                    Log.e(logTAG, this + "onCreate: Invalid intent extra mapview tile source key = " + mapViewLaunchState.selectedTileSourceKey + " - ignored.");

                // overlays keys
                osmMapViewVM.getOverlaysKeys().forEach(key -> osmMapViewVM.updateOverlaysSelection(key, false));
                mapViewLaunchState.selectedOverlaysKeys.forEach(key -> {
                    if(osmMapViewVM.getOverlaysKeys().contains(key)) {
                        osmMapViewVM.updateOverlaysSelection(key, true);
                    }
                    else
                        Log.e(logTAG, this + "onCreate: Invalid intent extra mapview overlay key = " + key + " - ignored.");
                });

                // 'My Locations' overlay
                osmMapViewVM.setShowMyLocationsOverlay(mapViewLaunchState.showMyLocationsOverlay);
            } catch (NullPointerException e) {
                Log.e(logTAG, this + "onCreate: Fatal error when retrieving MapViewDTO: " + e);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mapview_toolbar_menu, menu);

        // assigning menus
        sourcesMenu = menu.findItem(R.id.tileSourceSubmenu).getSubMenu();
        overlaysMenu = menu.findItem(R.id.overlaysSubmenu).getSubMenu();

        // creating source menu items
        for (Map.Entry<Integer, String> entry :
                osmMapViewVM.offeredSourcesMenuInput().entrySet()) {
            final MenuItem menuItem = sourcesMenu.add(R.id.tileSourceGroup, entry.getKey(), Menu.NONE, entry.getValue());
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        sourcesMenu.setGroupCheckable(R.id.tileSourceGroup, true, true);

        // creating source menu items
        for (Map.Entry<Integer, String> entry :
                osmMapViewVM.offeredOverlaysMenuInput().entrySet()) {
            final MenuItem menuItem = overlaysMenu.add(R.id.overlaySourceGroup, entry.getKey(), Menu.NONE, entry.getValue());
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menuItem.setCheckable(true);
        }

        // setting viewmodel observers
        osmMapViewVM.getSelectedTileSourceId().observe(this, key -> {
            final MenuItem menuitem = sourcesMenu.findItem(key);
            menuitem.setChecked(!menuitem.isChecked());
        });

        osmMapViewVM.getSelectedOverlaysIds().observe(this, selectedKeys -> {
            for (int overlayKey :
                    osmMapViewVM.getOverlaysKeys()) {
                overlaysMenu.findItem(overlayKey).setChecked(selectedKeys.contains(overlayKey));
            }
        });

        osmMapViewVM.isMyLocationsOverlayShown().observe(this, isShown -> overlaysMenu.findItem(R.id.myLocationsOverlayCheck).setChecked(isShown));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int groupId = item.getGroupId();
        final int itemId = item.getItemId();
        // tile sources
        if (groupId == R.id.tileSourceGroup && osmMapViewVM.offeredSourcesMenuInput().containsKey(itemId) ) {
            osmMapViewVM.setTileSourceSelection(itemId);
            return true;
        }

        // overlays
        if (groupId == R.id.overlaySourceGroup && osmMapViewVM.offeredOverlaysMenuInput().containsKey(itemId)) {
            osmMapViewVM.updateOverlaysSelection(itemId, !item.isChecked());
            return true;
        }
        if(groupId == R.id.otherOverlaysGroup && itemId == R.id.myLocationsOverlayCheck) {
            osmMapViewVM.setShowMyLocationsOverlay(!item.isChecked());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void moveToCurrentLocation() {
        // no location permission granted
        if(!osmMapViewVM.locationProvider.hasPermissions(getApplicationContext())) {
            requestLocationPermission();
            return;
        }

        // sending request and getting result task
        final Task<Void> locationRequestTask;
        try {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequestTask = osmMapViewVM.locationProvider.requestLocationUpdates(locationRequest, osmMapViewVM.singleUpdateCallback);
        } catch (Exception e) {
            // permissions were changed in meantime, provider is no longer working
            Snackbar.make(this.findViewById(android.R.id.content), "Needed location permissions are not granted! Request for current location failed!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // setting reaction, when task was started and is successful
        locationRequestTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                osmMapViewVM.setFollowingLocation(true);
                osmMapViewVM.setShowCurrentLocation(true);
            } else
                Toast.makeText(this, "Request for current location failed!", Toast.LENGTH_SHORT).show();
        });
    }

    protected void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
        Toast.makeText(this, "Permission for location not granted!", Toast.LENGTH_SHORT).show();
    }

    protected long getLocationIntervalSecs() {
        return Long.parseLong(PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.location_timeout_key), "25"));
    }

    public static class MapViewDTO implements Parcelable {
        public GeoPoint center;
        public double zoom;
        public boolean showCurrentPosition;
        public int selectedTileSourceKey;
        public ArrayList<Integer> selectedOverlaysKeys;
        public boolean showMyLocationsOverlay;

        public MapViewDTO(
                final GeoPoint mapCenter,
                final double mapZoom,
                final boolean mapShowingCurrPosition,
                final int mapTileSourceKey,
                final ArrayList<Integer> mapSelectedOverlays,
                final boolean isMyLocationsOverlayShown) {
            center = mapCenter;
            zoom = mapZoom;
            showCurrentPosition = mapShowingCurrPosition;
            selectedTileSourceKey = mapTileSourceKey;
            selectedOverlaysKeys = mapSelectedOverlays;
            showMyLocationsOverlay = isMyLocationsOverlayShown;
        }

        protected MapViewDTO(Parcel in) {
            center = in.readParcelable(GeoPoint.class.getClassLoader(), GeoPoint.class);
            zoom = in.readDouble();
            showCurrentPosition = in.readByte() != 0;
            selectedTileSourceKey = in.readInt();
            selectedOverlaysKeys = in.readArrayList(ArrayList.class.getClassLoader(), Integer.class);
            showMyLocationsOverlay = in.readBoolean();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(center, flags);
            dest.writeDouble(zoom);
            dest.writeByte((byte) (showCurrentPosition ? 1 : 0));
            dest.writeInt(selectedTileSourceKey);
            dest.writeList(selectedOverlaysKeys);
            dest.writeByte((byte) (showMyLocationsOverlay ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<MapViewDTO> CREATOR = new Creator<MapViewDTO>() {
            @Override
            public MapViewDTO createFromParcel(Parcel in) {
                return new MapViewDTO(in);
            }

            @Override
            public MapViewDTO[] newArray(int size) {
                return new MapViewDTO[size];
            }
        };
    }
}
