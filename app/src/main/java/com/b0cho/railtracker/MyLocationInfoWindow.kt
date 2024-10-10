package com.b0cho.railtracker

import android.view.View
import android.widget.Button
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

/**
 * MyLocationInfoWindow is extension of MarkerInfoWindow, but handling some listeners for:
 * - clicking on opened info window,
 * - long clicking on opened info window
 * - closing the info window with button - must implement button with id = R.id.bubble_close
 *
 */
open class MyLocationInfoWindow(
    layoutResInfo: Int,
    mapView: MapView
) : MarkerInfoWindow(layoutResInfo, mapView) {
    init {
        mView.setOnTouchListener(null) // override default behaviour from parent class
        mView.setOnLongClickListener(null)
        mView.findViewById<Button>(R.id.bubble_close)?.setOnClickListener { this.close() }
    }

    override fun onOpen(item: Any) {
        super.onOpen(item)
        if(item is Marker) {
            relatedObject = item.relatedObject
        }
    }

    override fun onClose() {
        super.onClose()
        relatedObject = null
    }

    /**
     * Sets new OnClickListener for displayed info window, removing previously set (if any).
     * @param newListener that is set and receives parameters:
     *** @param view of the info window
     *** @param relatedLocation to which displayed info window is related
     * @return reference to info window
     */
    fun setOnBubbleClickListener(newListener: (view: View, relatedLocation: MyLocation) -> Unit): MyLocationInfoWindow {
        mView.setOnClickListener { newListener(it, relatedObject as MyLocation) }
        return this
    }

    /**
     * Sets new OnLongClickListener for displayed info window, removing previously set (if any).
     * @param newListener that is set and receives parameters:
     *** @param view of the info window
     *** @param relatedLocation to which displayed info window is related
     * newListener should return true if action is consumed.
     * @return reference to info window
     */
    fun setOnBubbleLongClickListener(newListener: (view: View, relatedLocation: MyLocation) -> Boolean): MyLocationInfoWindow {
        mView.setOnLongClickListener {
                newListener(it, relatedObject as MyLocation)
        }
        return this
    }
}