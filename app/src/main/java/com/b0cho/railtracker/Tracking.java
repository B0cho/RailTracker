package com.b0cho.railtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.Objects;

public class Tracking extends Fragment {
    private Integer currentTileSourceKey;

    private OnFragmentInteractionListener mListener;
    private MapView mapView;

    public static SparseArray<ITileSource> offeredSources() {
        final SparseArray<ITileSource> sources = new SparseArray<>();
        /* TODO: Add tile sources below
         *  Mark default tile source with key = 0
         */
        sources.append(0, TileSourceFactory.DEFAULT_TILE_SOURCE);
        sources.append(1, TileSourceFactory.PUBLIC_TRANSPORT);
        return sources;
    }

    public Integer getCurrentTileSourceKey() {
        return currentTileSourceKey;
    }

    /*public static HashMap<Integer, TilesOverlay> offeredOverlays() {
        final HashMap<Integer, TilesOverlay> overlays = new HashMap();


        return overlays;
    }*/

    public Tracking() {
        // Required empty public constructor
    }

    public static Tracking newInstance() {
        return new Tracking();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tracking, container, false);

        // initialize mapview
        try {
            mapViewInit(view, savedInstanceState);
        } catch (Exception e) {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//        TODO: Add saving map view state

        // saving current tile source
        outState.putInt(getString(R.string.bundleTileSourceKey), currentTileSourceKey);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        // saving fragment settings
        SharedPreferences.Editor editor = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE).edit();
        editor.putInt(getString(R.string.bundleTileSourceKey), currentTileSourceKey);
        editor.apply();
    }

    private void mapViewInit(View view, Bundle savedInstanceState) {
        // loading osmdroid configuration
        final Context ctx = getContext();
        assert ctx != null;
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        try {
            mapView = (MapView) Objects.requireNonNull(view.findViewById(R.id.mapView));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(ctx, "ERROR: No MapView found", Toast.LENGTH_SHORT).show();
            return;
        }

        // default OpenRailwayMap overlay
        final MapTileProviderBasic ORM_tileProvider = new MapTileProviderBasic(ctx);
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
        final TilesOverlay ORM_overlay = new TilesOverlay(ORM_tileProvider, ctx);
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

        // loading tile source
        int tileSource;
        if (savedInstanceState == null)
            tileSource = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE).getInt(getString(R.string.bundleTileSourceKey), 0);
        else
            tileSource = savedInstanceState.getInt(getString(R.string.bundleTileSourceKey), 0);

        // attempt to set tile source
        try {
            setTileSource(tileSource);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error during restoring tile source. Loading default", Toast.LENGTH_SHORT).show();
            mapView.setTileSource(offeredSources().get(0));
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void setTileSource(final Integer sourceKey) throws Exception {
        if (offeredSources().indexOfKey(sourceKey) < 0)
            throw new Exception("Invalid tile source key");
        currentTileSourceKey = sourceKey;
        mapView.setTileSource(offeredSources().get(currentTileSourceKey));
    }
}


