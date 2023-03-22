package com.b0cho.railtracker

import android.view.View
import android.widget.Button
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow

/**
 * PinLocationInfoWindow is extension of MarkerInfoWindow, but handling some listeners for:
 * - change of info window visibility change (on open + on close),
 * - clicking on opened info window,
 * - long clicking on opened info window,
 *
 * It also enable closing the info window with button - must implement button with id = R.id.bubble_close.
 *
 */
open class PinLocationInfoWindow(layoutResInfo: Int,
                                 mapView: MapView,
                                 private val onWindowShowListener: OnVisibilityChangeListener? = null,
                                 onBubbleClickListener: View.OnClickListener? = null,
                                 onBubbleLongClickListener: View.OnLongClickListener? = null) : MarkerInfoWindow(layoutResInfo, mapView) {
    init {
        mView.setOnTouchListener(null) // override default behaviour from parent class
        mView.setOnClickListener(onBubbleClickListener)
        mView.setOnLongClickListener(onBubbleLongClickListener)
        mView.findViewById<Button>(R.id.bubble_close)?.setOnClickListener { this.close() }
    }

    override fun onOpen(item: Any) {
        super.onOpen(item)
        if(item is Marker)
            onWindowShowListener?.onOpen(item.relatedObject)
    }

    override fun onClose() {
        super.onClose()
        onWindowShowListener?.onClose()
    }

    /**
     * Dedicated listener for info window open/close listeners
     */
    abstract class OnVisibilityChangeListener {
        /**
         * Listener triggered when info window is getting shown.
         * @Any is an related object that is connected to selected Marker (if any).
         */
        abstract fun onOpen(item: Any?)

        /**
         * Listener triggered when info window is getting closed.
         */
        abstract fun onClose()
    }
}