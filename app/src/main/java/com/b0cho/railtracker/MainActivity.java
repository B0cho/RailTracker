package com.b0cho.railtracker;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

// MAIN ACTIVITY
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private MainActivityViewModel viewModel;
    private Menu sourcesMenu;
    private Menu overlaysMenu;
    private FloatingActionButton myLocationButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // obtaining viewmodel
        viewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);

        // setting toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // setting listeners and callbacks
        myLocationButton = findViewById(R.id.myLocationActionButton);

        viewModel.isLocationFollowed().observe(this, follow -> {
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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);

        // assigning menus
        sourcesMenu = menu.findItem(R.id.tileSourceSubmenu).getSubMenu();
        overlaysMenu = menu.findItem(R.id.overlaysSubmenu).getSubMenu();

        // creating source menu items
        for (Map.Entry<Integer, String> entry :
                viewModel.offeredSourcesMenuInput().entrySet()) {
            final MenuItem menuItem = sourcesMenu.add(R.id.tileSourceGroup, entry.getKey(), Menu.NONE, entry.getValue());
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        sourcesMenu.setGroupCheckable(R.id.tileSourceGroup, true, true);

        // creating source menu items
        for (Map.Entry<Integer, String> entry :
                viewModel.offeredOverlaysMenuInput().entrySet()) {
            final MenuItem menuItem = overlaysMenu.add(R.id.overlaySourceGroup, entry.getKey(), Menu.NONE, entry.getValue());
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menuItem.setCheckable(true);
        }

        // setting viewmodel observers
        viewModel.getSelectedTileSourceId().observe(this, key -> {
            final MenuItem menuitem = sourcesMenu.findItem(key);
            menuitem.setChecked(!menuitem.isChecked());
        });

        viewModel.getSelectedOverlaysIds().observe(this, keys -> {
            for(int id = 0; id < overlaysMenu.size(); id++) {
                overlaysMenu.findItem(id).setChecked(keys.contains(id));
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int groupId = item.getGroupId();
        final int itemId = item.getItemId();
        // tile sources
        if (groupId == R.id.tileSourceGroup && viewModel.offeredSourcesMenuInput().containsKey(itemId) ) {
            viewModel.setTileSourceSelection(itemId);
            return true;
        }

        // overlays
        if (groupId == R.id.overlaySourceGroup && viewModel.offeredOverlaysMenuInput().containsKey(itemId)) {
            viewModel.updateOverlaysSelection(itemId, !item.isChecked());
            return true;
        }

        // settings menu button
        if(itemId == R.id.settingsMenuButton) {
            final Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(openSettingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onMyLocationButtonClick(View view) {
        // no location permission granted
        if (!viewModel.hasLocationPermissions(getApplicationContext())) {
            requestPermissions();
            return;
        }

        // sending request and getting result task
        final Task<Void> locationRequestTask;
        try {
            locationRequestTask = viewModel.requestSingleLocationUpdate();
        } catch (IllegalStateException e) {
            // permissions were changed in meantime, provider is no longer working
            Snackbar.make(findViewById(R.id.activityMain), "Location access permissions are lost! Location service is no longer available", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // set reaction, if no task was returned (cached result)
        if(locationRequestTask == null) {
            viewModel.setFollowingLocation(true);
            viewModel.setShowingLocation(true);
            return;
        }

        // setting reaction, when task was started and is successful
        locationRequestTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                viewModel.setFollowingLocation(true);
                viewModel.setShowingLocation(true);
            } else
                Toast.makeText(this, "Request for location failed!", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean onMyLocationButtonLongClick(View view) {
        // no location permission granted
        if (!viewModel.hasLocationPermissions(getApplicationContext())) {
            requestPermissions();
            return true;
        }

        // sending request and getting result task
        final Task<Void> locationRequestTask;
        try {
            locationRequestTask = viewModel.requestLocationUpdates(viewModel.manualLocationUpdateRequest);
        } catch (IllegalStateException e) {
            Snackbar.make(findViewById(R.id.activityMain), "Location access permissions are lost! Location service is no longer available", Snackbar.LENGTH_SHORT).show();
            return true;
        }

        // setting reaction, when task is successful
        locationRequestTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Snackbar.make(findViewById(R.id.activityMain), "Location tracking for 5 mins activated", Snackbar.LENGTH_SHORT).show();
                viewModel.setFollowingLocation(true);
                viewModel.setShowingLocation(true);
            } else
                Toast.makeText(this, "Request for location updates failed!", Toast.LENGTH_SHORT).show();
        });
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                1);
        Toast.makeText(this, "Permission for location not granted!", Toast.LENGTH_SHORT).show();
    }

}
