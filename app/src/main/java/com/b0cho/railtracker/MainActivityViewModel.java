package com.b0cho.railtracker;

import android.app.Application;

import androidx.annotation.NonNull;
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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MainActivityViewModel extends AndroidViewModel {
    public final ILocationProvider locationProvider;
    public final LocationCallback manualUpdatesCallback;
    public final LocationCallback singleUpdateCallback;

    private final HashMap<Integer, ITileSource> keyedTileSources = new HashMap<>();
    private final HashMap<Integer, Pair<String, Overlay>> keyedOverlays = new HashMap<>();

    private final MutableLiveData<Pair<Integer, ITileSource>> selectedTileSourcePair = new MutableLiveData<>();
    private final MutableLiveData<HashMap<Integer, Pair<String, Overlay>>> selectedOverlaysHashMap = new MutableLiveData<>();
    private final MutableLiveData<IGeoPoint> lastPosition = new MutableLiveData<>();
    private final MutableLiveData<IGeoPoint> centerPoint = new MutableLiveData<>(new GeoPoint(52.0, 19.5));
    private final MutableLiveData<Double> zoom = new MutableLiveData<>(7.0);
    private final MutableLiveData<Boolean> followingLocation = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showingLocation = new MutableLiveData<>(false);

    @Inject
    public MainActivityViewModel(
            @NonNull Application application,
            Map<String, ITileSource> tileSources,
            Map<String, Overlay> overlaysSources,
            @NonNull ILocationProvider locationProvider) {
        super(application);
        this.locationProvider = locationProvider;

        // creating hashmap of offered sources for views + setting initial value
        int key = 0;
        for (Map.Entry<String, ITileSource> entry :
                tileSources.entrySet()) {
            keyedTileSources.put(key, entry.getValue());
            key++;
        }
        setTileSourceSelection(0);

        // creating hashmap of offered overlays for views
        selectedOverlaysHashMap.setValue(new HashMap<>());
        key = 0;
        for (Map.Entry<String, Overlay> entry :
                overlaysSources.entrySet()) {
            keyedOverlays.put(key, new Pair<>(entry.getKey(), entry.getValue()));
            key++;
        }
        updateOverlaysSelection(0, true);

        // setting callaback for location update (to differentiate calls)
        manualUpdatesCallback = locationProvider.getUpdateConsumersCallback();
        singleUpdateCallback = locationProvider.getUpdateConsumersCallback();
    }

    /**
     * @return LiveData of saved center point of map view
     */
    @NonNull
    public LiveData<IGeoPoint> getCenterPoint() {
        return centerPoint;
    }

    /**
     * @return LiveData of saved zoom of map view
     */
    @NonNull
    public LiveData<Double> getZoom() {
        return zoom;
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
     * @return LiveData of Set of MenuItem ids of selected overlays
     */
    @NonNull
    public LiveData<Set<Integer>> getSelectedOverlaysIds() {
        return Transformations.map(selectedOverlaysHashMap, HashMap::keySet);
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
     * LiveData with flag if location should be followed.
     * @return True, if location should be followed. Otherwise false.
     */
    @NonNull
    public LiveData<Boolean> isLocationFollowed() {
        return followingLocation;
    }

    /**
     * @return IMyLocationProvider, that can be used to feed MapView
     */
    public IMyLocationProvider getMyLocationProvider() {
        return (IMyLocationProvider) locationProvider;
    }

    /**
     * @return HashMap of pairs id - title, that can be used to create menu of offered tile sources
     */
    @NonNull
    public HashMap<Integer, String> offeredSourcesMenuInput() {
        HashMap<Integer, String> hashMap = new HashMap<>();
        keyedTileSources.forEach((key, source) -> hashMap.put(key, source.name()));
        return hashMap;
    }

    /**
     * @return HashMap of pairs id - title, that can be used to create menu of offered overlays
     */
    @NonNull
    public HashMap<Integer, String> offeredOverlaysMenuInput() {
        HashMap<Integer, String> hashMap = new HashMap<>();
        keyedOverlays.forEach((key, overlayPair) -> hashMap.put(key, overlayPair.first));
        return hashMap;
    }

    /**
     * Sets if location should be followed by observers (e.g. MapView)
     * @param follow True, if location should be followed. False if otherwise
     */
    public void setFollowingLocation(boolean follow) {
        followingLocation.setValue(follow);
    }

    /**
     * Sets selection of tile source, that should be used by observers/consumers
     * @param itemId ID of menuItem, that was selected from sources menu
     */
    public void setTileSourceSelection(int itemId) {
        selectedTileSourcePair.setValue(new Pair<>(itemId, keyedTileSources.get(itemId)));
    }

    /**
     * Updates a selection of overlays, that should be used by observers/consumers.
     * @param itemId ID of menuItem, that was selected from overlays menu. Overlay connected with given itemId will be added or removed from overlays selection.
     * @param checked If True, overlay of given ID will be added to selected overlays. Otherwise, it will be removed from selection
     */
    public void updateOverlaysSelection(int itemId, boolean checked) {
        HashMap<Integer, Pair<String, Overlay>> currentSelection = Objects.requireNonNull(selectedOverlaysHashMap.getValue());
        if(checked)
            currentSelection.put(itemId, keyedOverlays.get(itemId));
        else
            currentSelection.remove(itemId);
        selectedOverlaysHashMap.setValue(currentSelection);
    }

    /**
     * @return LiveData of last position of location (e.g. drawn on MapView)
     */
    @NonNull
    public LiveData<IGeoPoint> getLastPosition() {
        return lastPosition;
    }

    /**
     * @return LiveData of flag if location should be shown on e.g. MapView
     */
    @NonNull
    public MutableLiveData<Boolean> isLocationShown() {
        return showingLocation;
    }

    public void setShowingLocation(boolean showLocation) {
        showingLocation.setValue(showLocation);
    }

    public void setZoom(Double zoom) {
        this.zoom.setValue(zoom);
    }

    public void setCenterPoint(IGeoPoint center) {
        centerPoint.setValue(center);
    }
}
