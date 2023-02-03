package com.b0cho.railtracker;

import android.content.Context;

import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.views.overlay.TilesOverlay;

/**
 * Extension of TilesOverlay, providing copyright notice of tile source
 */
public class LicensedTilesOverlay extends TilesOverlay {
    public LicensedTilesOverlay(MapTileProviderBase aTileProvider, Context aContext) {
        super(aTileProvider, aContext);
    }

    public String getCopyrightNotice() {
        return mTileProvider.getTileSource().getCopyrightNotice();
    }
}
