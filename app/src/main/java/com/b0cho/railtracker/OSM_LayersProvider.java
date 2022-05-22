package com.b0cho.railtracker;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.ArrayList;

public class OSM_LayersProvider {
    public final static ArrayList<ITileSource> offeredSources;
    public final ArrayList<Pair<String, Overlay>> offeredOverlays = new ArrayList<>();

    static {
        // initialization of offeredSources
        offeredSources = new ArrayList<>();
        // TODO: Add tile offeredSources below
        offeredSources.add(TileSourceFactory.DEFAULT_TILE_SOURCE);
        offeredSources.add(new XYTileSource("Öpnvkarte", 1, 20, 256, ".png",
                new String[] {"https://tile.memomaps.de/tilegen/"}));

    }
    public OSM_LayersProvider(@NonNull Context context) {
        // TODO: Add offered overlays
        offeredOverlays.add(new Pair<>("OpenRailwayMap", createORM_TilesOverlay(context)));
    }

    /**
     * @param context - Context of app
     * @return An instance of TilesOverlay for OpenRailwayMap
     */
    @NonNull
    private static TilesOverlay createORM_TilesOverlay(Context context) {
        final MapTileProviderBasic tileProvider = new MapTileProviderBasic(context);
        tileProvider.setTileSource(new XYTileSource(
                "OpenRailwayMap",
                1,
                16,
                256,
                ".png",
                new String[]{
                        "http://a.tiles.openrailwaymap.org/standard/",
                        "http://b.tiles.openrailwaymap.org/standard/",
                        "http://c.tiles.openrailwaymap.org/standard/"}));
        final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, context);
        tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        return tilesOverlay;
    }
}
