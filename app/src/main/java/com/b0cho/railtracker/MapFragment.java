package com.b0cho.railtracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MapFragment, used as map view for MainActivity
 * Working with MainActivityViewModel
 * Capable to switch tile sources and overlays
 */
@AndroidEntryPoint
public class MapFragment extends Fragment {
    private MainActivityViewModel viewModel;
    private MapView mapView;
    private MyLocationNewOverlay locationOverlay;
    private CopyrightOverlay mapCopyright;
    private OverlayCopyrightOverlay overlayCopyright;
    private Observer<IGeoPoint> locationObserver;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.map_fragment, container, false);
        final Context context = requireContext();
        // create mapview
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        mapView = view.findViewById(R.id.mapView);
        mapView.setDestroyMode(false); // used to avoid mWriter getting null after view re-creation

        locationOverlay = new MyLocationNewOverlay(mapView);

        // setting copyright overlays
        final int copyrightColor = ContextCompat.getColor(context, R.color.copyrightText);
        mapCopyright = new CopyrightOverlay(context);
        mapCopyright.setOffset(10, 10);
        mapCopyright.setTextColor(copyrightColor);

        overlayCopyright = new OverlayCopyrightOverlay(context);
        overlayCopyright.setOffset(10, 50);
        overlayCopyright.setTextColor(copyrightColor);

        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // getting viewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MainActivityViewModel.class);

        // setting location provider for viewmap
        locationOverlay.enableMyLocation(viewModel.getMyLocationProvider());

        // setting listener for moving map -> lose following location
        mapView.setOnTouchListener((view1, motionEvent) -> {
            requireActivity().onTouchEvent(motionEvent);
            if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                viewModel.setFollowingLocation(false);
            }
            return false;
        });

        /*
        SETTING OBSERVERS
         */
        // tile sources selection
        viewModel.getSelectedTileSource().observe(requireActivity(), selectedTileSource -> {
            if (selectedTileSource != null) {
                mapView.setTileSource(selectedTileSource);
            }
            mapView.invalidate();
        });

        // overlays selection
        viewModel.getSelectedOverlays().observe(requireActivity(), selectedOverlays -> {
            mapView.getOverlays().clear();
            for (Overlay selection :
                    selectedOverlays) {
                mapView.getOverlays().add(selection);
            }
            showLocationOverlay(Objects.requireNonNull(viewModel.isLocationShown().getValue()));
            // adding copyright overlays
            mapView.getOverlays().add(mapCopyright);
            mapView.getOverlays().add(overlayCopyright);
            mapView.invalidate();
        });

        // showing location
        viewModel.isLocationShown().observe(requireActivity(), show -> {
            showLocationOverlay(show);
            mapView.invalidate();
        });

        // initial setting of center position and zoom
        viewModel.getCenterPoint().observe(requireActivity(), geoPoint -> mapView.getController().setCenter(geoPoint));
        viewModel.getZoom().observe(requireActivity(), zoom -> mapView.getController().setZoom(zoom));

        // setting mapview control
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setMultiTouchControls(true);

        // following location
        locationObserver = location -> mapView.getController().animateTo(location);
        viewModel.isLocationFollowed().observe(requireActivity(), follow -> {
            if(follow) {
                locationOverlay.enableFollowLocation();
                viewModel.getLastPosition().observe(requireActivity(), locationObserver);
            } else {
                locationOverlay.disableFollowLocation();
                viewModel.getLastPosition().removeObserver(locationObserver);
            }
        });
    }

    private void showLocationOverlay(Boolean show) {
        if(show)
            mapView.getOverlays().add(locationOverlay);
        else
            mapView.getOverlays().remove(locationOverlay);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // saving state on detach
        viewModel.setZoom(mapView.getZoomLevelDouble());
        viewModel.setCenterPoint(mapView.getMapCenter());
    }

    /**
     * Extends CopyrightOverlay to handle LicensedOverlays
     */
    private static class OverlayCopyrightOverlay extends CopyrightOverlay {
        public OverlayCopyrightOverlay(Context context) {
            super(context);
        }

        @Override
        public void draw(Canvas canvas, MapView map, boolean shadow) {
            StringBuilder copyrightNotice = new StringBuilder();
            for (Overlay overlay:
            map.getOverlays()){
                if(overlay instanceof LicensedTilesOverlay){
                    if(copyrightNotice.length() != 0)
                        copyrightNotice.append(", ");
                    copyrightNotice.append(((LicensedTilesOverlay) overlay).getCopyrightNotice());
                }
            }
            setCopyrightNotice(copyrightNotice.toString());
            draw(canvas, map.getProjection());
        }
    }
}


