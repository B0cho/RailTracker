package com.b0cho.railtracker;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

// MAIN ACTIVITY
public class MainActivity extends AppCompatActivity {
    private MapView mapView = null;
    private Menu toolbarMenu = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setting toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // mapview init
        mapViewInit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.toolbar_menu, menu);
        toolbarMenu = menu;

        // setting set tile source
        setMenuTileSource();

        return true;
    }

    private void setMenuTileSource() {
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
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mapView.onResume();
    }

    @Override
    public  void onPause() {
        super.onPause();

        mapView.onPause();
    }

    private void mapViewInit() {
        // loading osmdroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        mapView = findViewById(R.id.mapView);

        // default tile source
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // default OpenRailwayMap overlay
        final MapTileProviderBasic ORM_tileProvider = new MapTileProviderBasic(getApplicationContext());
        final ITileSource ORM_tileSource = new XYTileSource(
                "OpenRailwayMap",
                1,
                16,
                256,
                ".png",
                new String[]{
                        "http://a.tiles.openrailwaymap.org/standard/",
                        "http://b.tiles.openrailwaymap.org/standard/",
                        "http://c.tiles.openrailwaymap.org/standard/"});
        ORM_tileProvider.setTileSource(ORM_tileSource);
        final TilesOverlay ORM_overlay = new TilesOverlay(ORM_tileProvider, this.getBaseContext());
        ORM_overlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        mapView.getOverlays().add(ORM_overlay);

        // zooming settings
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setMultiTouchControls(true);
        IMapController mapController = mapView.getController();
        mapController.setZoom(7.5);

        // starting point
        GeoPoint startingPoint = new GeoPoint(51.46, 19.27);
        mapController.setCenter(startingPoint);
    }
}
