package com.b0cho.railtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Pair;
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
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Tracking extends Fragment {
    private Integer currentTileSourceKey;
    private final static SparseArray<ITileSource> offeredSources;

    static {
        // initialization of offeredSources
        offeredSources = new SparseArray<>();
        /* TODO: Add tile offeredSources below
         *  Mark default tile source with key = 0
         */
        offeredSources.append(0, TileSourceFactory.DEFAULT_TILE_SOURCE);
    }

    private Set<Integer> currentOverlaysKeys;

    private OnFragmentInteractionListener mListener;
    private MapView mapView;
    private SparseArray<Pair<String, Overlay>> offeredOverlays;
    private MyLocationNewOverlay locationOverlay;

    public static SparseArray<ITileSource> offeredSources() {
        return offeredSources;
    }

    private TilesOverlay OpenRailwayMap_overlay(Context context) {
        final MapTileProviderBasic ORM_tileProvider = new MapTileProviderBasic(context);
        final String[] ORM_tileUrls = getResources().getStringArray(R.array.ORM_tileUrls);
        ORM_tileProvider.setTileSource(new XYTileSource(
                "OpenRailwayMap",
                1,
                16,
                256,
                ".png",
                ORM_tileUrls));
        final TilesOverlay ORM_overlay = new TilesOverlay(ORM_tileProvider, context);
        ORM_overlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        return ORM_overlay;
    }

    public Integer getCurrentTileSourceKey() {
        return currentTileSourceKey;
    }

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

        // saving current overlays
        outState.putIntegerArrayList(getString(R.string.bundleOverlaysKey), new ArrayList<>(getCurrentOverlaysKeys()));
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
        // saving current tile source
        editor.putInt(getString(R.string.bundleTileSourceKey), currentTileSourceKey);
        // saving current overlays
        Set<String> stringSet = new HashSet<>();
        for (Integer key : currentOverlaysKeys)
            stringSet.add(key.toString());
        editor.putStringSet(getString(R.string.bundleOverlaysKey), stringSet);
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

        // initializing locationOverlay
        locationOverlay = new MyLocationNewOverlay(mapView);

        // initializing overlays
        offeredOverlays = new SparseArray<>();
        currentOverlaysKeys = new HashSet<>();

        /* TODO: Append offered overlays
         */
        offeredOverlays.append(0, new Pair<>(getString(R.string.ORMStandard_name), (Overlay) OpenRailwayMap_overlay(ctx)));

        // loading overlays
        ArrayList<Integer> overlaysKeys;
        if (savedInstanceState == null) {
            // restoring state from last launch
            final Set<String> overlaysKeysStrings = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE).getStringSet(getString(R.string.bundleOverlaysKey), new HashSet<>(Collections.singletonList("0")));
            overlaysKeys = new ArrayList<>();
            assert overlaysKeysStrings != null : "overlaysKeysStrings = null";
            for (String key : overlaysKeysStrings)
                overlaysKeys.add(Integer.parseInt(key));
        } else
            overlaysKeys = savedInstanceState.getIntegerArrayList(getString(R.string.bundleOverlaysKey));

        try {
            setOverlays(Objects.requireNonNull(overlaysKeys).toArray(new Integer[0]));
        } catch (Exception e) {
            // TODO: add handling
        }

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

    public void setOverlays(Integer... keys) throws Exception {
//        TODO: add handling for null parameter and test
        Exception exception = null;
        for (Integer key : keys) {
            if (key < 0)
                if (exception == null)
                    exception = new Exception("Fail during setting Overlay");
            if (currentOverlaysKeys.contains(key)) {
                // turning off overlay
                mapView.getOverlays().remove(offeredOverlays.valueAt(key).second);
                currentOverlaysKeys.remove(key);
            } else {
                // turning on overlay
                mapView.getOverlays().add(offeredOverlays.valueAt(key).second);
                currentOverlaysKeys.add(key);
            }
            mapView.invalidate();
        }
        if (exception != null)
            throw exception;
    }

    public final SparseArray<Pair<String, Overlay>> getOfferedOverlays() {
        return offeredOverlays;
    }

    public Set<Integer> getCurrentOverlaysKeys() {
        return currentOverlaysKeys;
    }

    public MyLocationNewOverlay getLocationOverlay() {
        return locationOverlay;
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


