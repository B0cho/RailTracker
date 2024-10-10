package com.b0cho.railtracker;

import android.content.Context;
import android.graphics.Canvas;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

/**
 * Extends CopyrightOverlay to handle LicensedOverlays
 */
public class CopyrightOverlay extends org.osmdroid.views.overlay.CopyrightOverlay {
    public CopyrightOverlay(Context context) {
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
