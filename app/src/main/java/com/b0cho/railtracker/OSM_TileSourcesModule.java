package com.b0cho.railtracker;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;
import dagger.multibindings.IntoMap;
import dagger.multibindings.StringKey;

@Module
@InstallIn(ViewModelComponent.class)
final class OSM_TileSourcesModule {
    @Provides
    @IntoMap
    @StringKey("MAPNIK")
    public static ITileSource provideMapnik() {
        return TileSourceFactory.DEFAULT_TILE_SOURCE;
    }

    @Provides
    @IntoMap
    @StringKey("Öpnvkarte")
    public static ITileSource provideOpnvkarte() {
        return new XYTileSource(
                "Öpnvkarte",
                1,
                20,
                256,
                ".png",
                new String[] {"https://tile.memomaps.de/tilegen/"});
    }
}
