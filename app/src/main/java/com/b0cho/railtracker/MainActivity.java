package com.b0cho.railtracker;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.osmdroid.views.overlay.Overlay;

import java.util.Set;

// MAIN ACTIVITY
public class MainActivity extends AppCompatActivity implements Tracking.OnFragmentInteractionListener {
    private Menu toolbarMenu = null;
    private Menu sourcesMenu = null;
    private Menu overlaysMenu = null;
    private Tracking trackingFragment;

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

    @Override
    public void onFragmentInteraction(Uri uri) {
        // TODO:
    }

    private void setTilesSourceSelection(final int key) throws Exception {
        final MenuItem menuItem = sourcesMenu.findItem(key);
        if (menuItem == null)
            throw new Exception("Invalid selection key - tiles source");
        menuItem.setChecked(true);
    }
}
