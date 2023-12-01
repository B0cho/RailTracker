package com.b0cho.railtracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * MapFragment, used as map view for MainActivity
 * Working with MainActivityViewModel
 * Capable to switch tile sources and overlays
 */
@AndroidEntryPoint
public class OSMMapViewFragment extends Fragment {
    private OSMMapViewVM osmMapViewVM;
    private MapView mapView;
    private Observer<IGeoPoint> locationObserver;
    private MyLocationNewOverlay locationOverlay;
    @Inject
    public CopyrightOverlay mapCopyright;
    @Inject
    public OverlayCopyrightOverlay overlayCopyright;

    private boolean showMyPinLocationMarkers;
    private ArrayList<Overlay> selectedOverlays;
    private boolean showMyCurrentLocation;
    private List<Marker> myPinLocationsMarkers;
    private MarkerInfoWindow myPinLocationIW;
    private Marker targetMarker;

    public OSMMapViewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mapview, container, false);
        final Context context = requireContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        mapView = view.findViewById(R.id.mapView);
        mapView.setDestroyMode(false); // used to avoid mWriter getting null after view re-creation

        locationOverlay = new MyLocationNewOverlay(mapView);
        myPinLocationsMarkers = new ArrayList<>();
        targetMarker = null;

        myPinLocationIW = new PinLocationInfoWindow(R.layout.marker_info_window_my_location, mapView, new PinLocationInfoWindow.OnVisibilityChangeListener() {
            @Override
            public void onOpen(@Nullable Object item) {
                if(item instanceof MyLocation)
                    osmMapViewVM.setSelectedPinLocationIWObject((MyLocation) item);
            }

            @Override
            public void onClose() {
                osmMapViewVM.setSelectedPinLocationIWObject(null);
            }
        }, null, null);
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        osmMapViewVM = new ViewModelProvider(requireActivity()).get(OSMMapViewVM.class);

        locationOverlay.enableMyLocation(osmMapViewVM.getMyLocationProvider());

        // setting listener for moving map -> lose following location
        mapView.setOnTouchListener((view1, motionEvent) -> {
            requireActivity().onTouchEvent(motionEvent);
            if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                osmMapViewVM.setFollowingLocation(false);
                osmMapViewVM.setCenterPoint(mapView.getMapCenter());
                osmMapViewVM.setZoom(mapView.getZoomLevelDouble());
            }
            return false;
        });

        /*
        SETTING OBSERVERS
         */
        // tile sources selection
        osmMapViewVM.getSelectedTileSource().observe(requireActivity(), selectedTileSource -> {
            if (selectedTileSource != null) {
                mapView.setTileSource(selectedTileSource);
            }
            mapView.invalidate();
        });

        // overlays selection
        osmMapViewVM.getSelectedOverlays().observe(requireActivity(), newSelectedOverlays -> {
            selectedOverlays = newSelectedOverlays;
            reloadOverlays();
        });

        // 'My locations' data
        osmMapViewVM.myPinLocationsLiveData().observe(requireActivity(), myPinLocationEntities -> {
            myPinLocationsMarkers.clear();
            myPinLocationsMarkers.addAll(myPinLocationEntities.stream().map(pinLocationEntity -> {
                final Marker marker = new Marker(mapView);
                marker.setPosition(pinLocationEntity.getPosition());
                marker.setTitle(pinLocationEntity.getName());
                marker.setInfoWindow(myPinLocationIW);
                marker.setRelatedObject(pinLocationEntity);
                return marker;
            }).collect(Collectors.toList()));
            reloadOverlays();
        });

        // showing current location
        osmMapViewVM.isLocationShown().observe(requireActivity(), show -> {
            showMyCurrentLocation = show;
            reloadOverlays();
        });

        // showing 'My locations' overlay
        osmMapViewVM.isMyLocationsOverlayShown().observe(requireActivity(), show -> {
            showMyPinLocationMarkers = show;
            myPinLocationsMarkers.forEach(OverlayWithIW::closeInfoWindow);
            reloadOverlays();
        });

        // map center point + zoom
        osmMapViewVM.getCenterPoint().observe(requireActivity(), geoPoint -> {
            if(!mapView.getMapCenter().equals(geoPoint))
                mapView.getController().setCenter(geoPoint);
        });
        osmMapViewVM.getZoom().observe(requireActivity(), zoom -> {
            if(mapView.getZoomLevelDouble() != zoom)
                mapView.getController().setZoom(zoom);
        });

        // setting mapview control
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setMultiTouchControls(true);

        // following location
        locationObserver = location -> mapView.getController().animateTo(location);
        osmMapViewVM.isLocationFollowed().observe(requireActivity(), follow -> {
            if(follow) {
                locationOverlay.enableFollowLocation();
                osmMapViewVM.getLastPosition().observe(requireActivity(), locationObserver);
            } else {
                locationOverlay.disableFollowLocation();
                osmMapViewVM.getLastPosition().removeObserver(locationObserver);
            }
        });

        // adding target marker if set in VM
        if(osmMapViewVM.getTargetMarkerGeoPoint().isPresent()) {
            targetMarker = new Marker(mapView);
            targetMarker.setPosition(osmMapViewVM.getTargetMarkerGeoPoint().get());
            targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            targetMarker.setPanToView(false);
            targetMarker.setInfoWindow(null);
            // TODO: update icon of target marker
        }
    }

    private void reloadOverlays() {
        mapView.getOverlays().clear();
        mapView.getOverlays().addAll(selectedOverlays);

        // adding overlays with locations
        if(showMyPinLocationMarkers)
            mapView.getOverlays().addAll(myPinLocationsMarkers);
        if(targetMarker != null)
            mapView.getOverlays().add(targetMarker);
        if(showMyCurrentLocation)
            mapView.getOverlays().add(locationOverlay);

        // adding copyright overlays
        mapView.getOverlays().add(mapCopyright);
        mapView.getOverlays().add(overlayCopyright);
        mapView.invalidate();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // saving state on detach
        osmMapViewVM.setZoom(mapView.getZoomLevelDouble());
        osmMapViewVM.setCenterPoint(mapView.getMapCenter());
    }
}


