package com.b0cho.railtracker;

import android.content.Context;
import android.graphics.Color;

import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.views.overlay.Overlay;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.FragmentScoped;
import dagger.hilt.android.scopes.ViewModelScoped;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;

@Module
@InstallIn(SingletonComponent.class)
public class OSM_OverlaysModule {
    @Provides
    @Singleton
    public MapTileProviderBase provideMapTileProviderBasic(@ApplicationContext Context context) {
        return new MapTileProviderBasic(context);
    }

    @Provides
    @Singleton
    @IntoMap
    @StringKey("OpenRailwayMap")
    public Overlay provideORM_TilesOverlay (@ApplicationContext Context context, MapTileProviderBase tileProvider) {
        tileProvider.setTileSource(new XYTileSource(
                "OpenRailwayMap",
                1,
                16,
                256,
                ".png",
                new String[]{
                        "https://a.tiles.openrailwaymap.org/standard/",
                        "https://b.tiles.openrailwaymap.org/standard/",
                        "https://c.tiles.openrailwaymap.org/standard/"},
                "OpenRailwayMap: © OpenStreetMap contributors"));
        final LicensedTilesOverlay licensedOverlay = new LicensedTilesOverlay(tileProvider, context);
        licensedOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        return licensedOverlay;
    }
}
