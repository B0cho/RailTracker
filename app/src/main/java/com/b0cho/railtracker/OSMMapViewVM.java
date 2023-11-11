package com.b0cho.railtracker;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.android.gms.location.LocationCallback;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class OSMMapViewVM extends AndroidViewModel {
    public final ILocationProvider locationProvider;
    public final LocationCallback singleUpdateCallback;
    public final LocationCallback manualUpdatesCallback;

    private final HashMap<Integer, ITileSource> tileSourcesMap;
    private final HashMap<Integer, Pair<String, Overlay>> overlaysMap;

    private final MutableLiveData<HashMap<Integer, Pair<String, Overlay>>> selectedOverlaysHashMap;
    private final MutableLiveData<Pair<Integer, ITileSource>> selectedTileSourcePair;
    private final MutableLiveData<Optional<PinLocationEntity>> selectedPinLocationIWObject;

    private final MutableLiveData<IGeoPoint> centerPoint;
    private final MutableLiveData<Double> zoom;
    private final MutableLiveData<Boolean> followingLocation;
    private final MutableLiveData<Boolean> showCurrentLocation;
    private final MutableLiveData<Boolean> showMyPinLocations;
    private final MutableLiveData<IGeoPoint> lastPosition;
    private GeoPoint targetMarkerGeopoint;

    // TODO:
    private final MutableLiveData<List<PinLocationEntity>> myPinLocationsLiveData;

    @Inject
    public OSMMapViewVM(
            @NonNull Application application,
            Map<String, ITileSource> tileSources,
            Map<String, Overlay> overlaysSources,
            @NonNull ILocationProvider locationProvider) {
        super(application);

        // init
        tileSourcesMap = new HashMap<>();
        overlaysMap = new HashMap<>();
        selectedOverlaysHashMap = new MutableLiveData<>();
        selectedTileSourcePair = new MutableLiveData<>();
        selectedPinLocationIWObject = new MutableLiveData<>(Optional.empty());
        this.locationProvider = locationProvider;
        lastPosition = new MutableLiveData<>();
        myPinLocationsLiveData = new MutableLiveData<>();
        // initial settings
        centerPoint = new MutableLiveData<>(new GeoPoint(52.0, 19.5));
        zoom = new MutableLiveData<>(7.0);
        followingLocation = new MutableLiveData<>(false);
        showCurrentLocation  = new MutableLiveData<>(false);
        showMyPinLocations = new MutableLiveData<>(false);
        targetMarkerGeopoint = null;

        // creating hashmap of offered sources for views + setting initial value
        int key = 0;
        for (Map.Entry<String, ITileSource> entry :
                tileSources.entrySet()) {
            tileSourcesMap.put(key, entry.getValue());
            key++;
        }
        setTileSourceSelection(0);

        // creating hashmap of offered overlays for views
        selectedOverlaysHashMap.setValue(new HashMap<>());
        key = 0;
        for (Map.Entry<String, Overlay> entry :
                overlaysSources.entrySet()) {
            overlaysMap.put(key, new Pair<>(entry.getKey(), entry.getValue()));
            key++;
        }
        updateOverlaysSelection(0, true);

        // setting callaback for location update (to differentiate calls)
        singleUpdateCallback = locationProvider.getUpdateConsumersCallback();
        manualUpdatesCallback = locationProvider.getUpdateConsumersCallback();
    }

    /**
     * @return HashMap of pairs id - title, that can be used to create menu of offered tile sources
     */
    @NonNull
    public HashMap<Integer, String> offeredSourcesMenuInput() {
        HashMap<Integer, String> hashMap = new HashMap<>();
        tileSourcesMap.forEach((key, source) -> hashMap.put(key, source.name()));
        return hashMap;
    }

    /**
     * @return HashMap of pairs id - title, that can be used to create menu of offered overlays
     */
    @NonNull
    public HashMap<Integer, String> offeredOverlaysMenuInput() {
        HashMap<Integer, String> hashMap = new HashMap<>();
        overlaysMap.forEach((key, overlayPair) -> hashMap.put(key, overlayPair.first));
        return hashMap;
    }

    /**
     * @return Set of keys of available overlays
     */
    public Set<Integer> getOverlaysKeys() {
        return overlaysMap.keySet();
    }

    /**
     * @return LiveData of Set of MenuItem ids of selected overlays
     */
    @NonNull
    public LiveData<Set<Integer>> getSelectedOverlaysIds() {
        return Transformations.map(selectedOverlaysHashMap, HashMap::keySet);
    }

    /**
     * Updates a selection of overlays, that should be used by observers/consumers.
     * @param itemId ID of menuItem, that was selected from overlays menu. Overlay connected with given itemId will be added or removed from overlays selection.
     * @param checked If True, overlay of given ID will be added to selected overlays. Otherwise, it will be removed from selection
     */
    public void updateOverlaysSelection(int itemId, boolean checked) {
        HashMap<Integer, Pair<String, Overlay>> currentSelection = Objects.requireNonNull(selectedOverlaysHashMap.getValue());
        if(checked)
            currentSelection.put(itemId, overlaysMap.get(itemId));
        else
            currentSelection.remove(itemId);
        selectedOverlaysHashMap.setValue(currentSelection);
    }

    /**
     * @return LiveData of list of selected overlays for mapview
     */
    @NonNull
    public LiveData<ArrayList<Overlay>> getSelectedOverlays() {
        return Transformations.map(selectedOverlaysHashMap, pairs -> {
            ArrayList<Overlay> array = new ArrayList<>();
            for (Pair<String, Overlay> pair:
                    pairs.values()){
                array.add(pair.second);
            }
            return array;
        });
    }

    /**
     * Sets visibility of 'My locations' overlay     *
     * @param showOverlay - true, if overlay with 'My locations' should be shown
     */
    public void setShowMyPinLocations(boolean showOverlay) {
        showMyPinLocations.setValue(showOverlay);
    }

    /**
     * @return LiveData of flag if 'My locations' overlay should be shown
     */
    @NonNull
    public LiveData<Boolean> isMyLocationsOverlayShown() {
        return showMyPinLocations;
    }

    /**
     * @return LiveData ITileSource of selected tile source
     */
    @NonNull
    public LiveData<ITileSource> getSelectedTileSource() {
        return Transformations.map(selectedTileSourcePair, source -> source.second);
    }

    /**
     * @return LiveData of MenuItem id of selected tile source
     */
    @NonNull
    public LiveData<Integer> getSelectedTileSourceId() {
        return Transformations.map(selectedTileSourcePair, source -> source.first);
    }

    /**
     * Sets selection of tile source, that should be used by observers/consumers
     * @param itemId ID of menuItem, that was selected from sources menu
     */
    public void setTileSourceSelection(int itemId) {
        selectedTileSourcePair.setValue(new Pair<>(itemId, tileSourcesMap.get(itemId)));
    }

    /**
     * @return LiveData of saved center point of map view
     */
    @NonNull
    public LiveData<IGeoPoint> getCenterPoint() {
        return centerPoint;
    }

    /**
     * Sets center point of connected mapview
     *
     * @param center Center point, to that map should be moved
     */
    public void setCenterPoint(IGeoPoint center) {
        centerPoint.setValue(center);
    }

    /**
     * @return LiveData of saved zoom of map view
     */
    @NonNull
    public LiveData<Double> getZoom() {
        return zoom;
    }

    /**
     * Sets zoom of connected map view
     *
     * @param zoom - value of zoom to be applied
     */
    public void setZoom(Double zoom) {
        this.zoom.setValue(zoom);
    }

    /**
     * LiveData with flag if location should be followed.
     * @return True, if location should be followed. Otherwise false.
     */
    @NonNull
    public LiveData<Boolean> isLocationFollowed() {
        return followingLocation;
    }

    /**
     * Sets if location should be followed by observers (e.g. MapView)
     * @param follow True, if location should be followed. False if otherwise
     */
    public void setFollowingLocation(boolean follow) {
        followingLocation.setValue(follow);
    }

    /**
     * Sets visibility of current location     *
     * @param showLocation - true, if current locations should be shown
     */
    public void setShowCurrentLocation(boolean showLocation) {
        showCurrentLocation.setValue(showLocation);
    }

    /**
     * @return LiveData of flag if location should be shown on e.g. MapView
     */
    @NonNull
    public LiveData<Boolean> isLocationShown() {
        return showCurrentLocation;
    }

    /**
     * @return LiveData of last position of location (e.g. drawn on MapView)
     */
    @NonNull
    public LiveData<IGeoPoint> getLastPosition() {
        return lastPosition;
    }

    /**
     * @param relatedObject to be set as currently selected related Object
     */
    public void setSelectedPinLocationIWObject(final PinLocationEntity relatedObject) {
        selectedPinLocationIWObject.setValue(Optional.ofNullable(relatedObject));
    }

    /**
     * @return LiveData of list with loaded 'my locations
     */
    public LiveData<List<PinLocationEntity>> myPinLocationsLiveData() {
        return myPinLocationsLiveData;
    }

    /**
     * @return IMyLocationProvider, that can be used to feed MapView
     */
    public IMyLocationProvider getMyLocationProvider() {
        return (IMyLocationProvider) locationProvider;
    }

    /**
     * Adds GeoPoint, that can be used by VM users to add static Marker (e.g Target Marker) during e.g. OnCreate
     * Value is NOT observable
     * @param geoPoint to be used to create Marker. Pass null, to remove saved geopoint.
     */
    public void setTargetMarker(@Nullable GeoPoint geoPoint) {
        targetMarkerGeopoint = geoPoint;
    }

    /**
     * Returns GeoPoint, that can be used by VM users to add static Marker (e.g Target Marker) during e.g. OnCreate
     * Value is NOT observable
     * @return GeoPoint wrapped by Optional. Empty, if geopoint was not set or removed.
     */
    public Optional<GeoPoint> getTargetMarkerGeoPoint() {
        return Optional.ofNullable(targetMarkerGeopoint);
    }
}