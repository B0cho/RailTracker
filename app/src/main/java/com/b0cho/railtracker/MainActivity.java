package com.b0cho.railtracker;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;

// MAIN ACTIVITY
public class MainActivity extends AppCompatActivity implements Tracking.OnFragmentInteractionListener {
    private Menu toolbarMenu = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        toolbarMenu = menu;
        setMenuTileSource();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    private void setMenuTileSource() {
        /*
        final MapView mapView = findViewById(R.id.mapView);
        assert mapView != null;

        mapView.setTileSource(TileSourceFactory.MAPNIK);

        MenuItem option = toolbarMenu.findItem(R.id.MAPNIK_source);
        option.setChecked(true);

        MenuItem option = null;
        final ITileSource source = mapView.getTileProvider().getTileSource();


        // MAPNIK
        if (source == TileSourceFactory.MAPNIK)
            option = toolbarMenu.findItem(R.id.MAPNIK_source);

        // setting checked
        try {
            //noinspection ConstantConditions
            option.setChecked(true);
        } catch (Exception e) {
            // if no option was found
            Log.e(null, e.getLocalizedMessage(), e);
            Toast.makeText(getApplicationContext(), "ERROR: No tile source radio button found.", Toast.LENGTH_LONG).show();
        }*/
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        // TODO:
    }
}
