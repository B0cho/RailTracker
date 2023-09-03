package com.b0cho.railtracker;

import android.Manifest;
import android.os.Bundle;
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

import org.osmdroid.views.MapView;

import java.util.Map;
import java.util.Objects;

public class OSMMapViewActivity extends AppCompatActivity {
    protected OSMMapViewVM osmMapViewVM;
    protected Menu sourcesMenu;
    protected Menu overlaysMenu;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        osmMapViewVM = new ViewModelProvider(this).get(OSMMapViewVM.class);
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
            osmMapViewVM.setShowMyPinLocations(!item.isChecked());
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
}
