package com.b0cho.railtracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.Menu
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import kotlin.streams.toList

@AndroidEntryPoint
class LocationPickerActivity : OSMMapViewActivity() {
    companion object {
        // KEYS FOR INTENT EXTRAS
        const val PICKED_GEOPOINT = "PICKED_GEOPOINT"
    }

    private lateinit var mCurrentAimedPosition: IGeoPoint
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)
        val toolbar = findViewById<Toolbar>(R.id.locationPickerToolbar)
        setSupportActionBar(toolbar)

        if(savedInstanceState == null) {
            intent.getParcelableExtra(
                MAPVIEW_STATE,
                LocationPickerDTO::class.java
            )?.apply {
                osmMapViewVM.setTargetMarker(currentGeopoint)
                locationName?.let { toolbar.title = it }
            }
        }
        osmMapViewVM.centerPoint.observe(this) { currentAimedPosition ->
            mCurrentAimedPosition = currentAimedPosition
        }

        val myLocationButton: FloatingActionButton = findViewById(R.id.myLocationActionButton2)
        myLocationButton.setOnClickListener { super.moveToCurrentLocation() }

        val confirmPositionButton: Button = findViewById(R.id.confirmPositionButton)
        confirmPositionButton.setOnClickListener {
            setResult(RESULT_OK, Intent(intent).apply { putExtra(PICKED_GEOPOINT, mCurrentAimedPosition as Parcelable) })
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val settingsMenuButton = super.onCreateOptionsMenu(menu)
        if (!settingsMenuButton) {
            return false
        }
        menu?.findItem(R.id.settingsMenuButton)?.isVisible = false
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        setResult(RESULT_CANCELED)
        return true
    }

    class ResultContract : ActivityResultContract<LocationPickerDTO?, GeoPoint?>() {
        override fun createIntent(context: Context, input: LocationPickerDTO?): Intent {
            return Intent(context, LocationPickerActivity::class.java)
                .putExtra(MAPVIEW_STATE, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): GeoPoint? {
            return if (resultCode == RESULT_OK) {
                intent?.getParcelableExtra(PICKED_GEOPOINT, GeoPoint::class.java)
            } else {
                null
            }
        }
    }
}

open class LocationPickerDTO(
    center: GeoPoint,
    mapZoom: Double,
    mapShowingCurrPosition: Boolean,
    mapTileSourceKey: Int,
    mapSelectedOverlays: List<Int>,
    isMyLocationsOverlayShown: Boolean,
    val currentGeopoint: GeoPoint? = null,
    val locationName: String? = null,
) : OSMMapViewActivity.MapViewDTO(center, mapZoom, mapShowingCurrPosition, mapTileSourceKey, ArrayList(mapSelectedOverlays), isMyLocationsOverlayShown) {
    constructor(mapViewDTO: OSMMapViewActivity.MapViewDTO,
                currentGeopoint: GeoPoint? = null,
                locationName: String? = null,
    ) : this(
        mapViewDTO.center,
        mapViewDTO.zoom,
        mapViewDTO.showCurrentPosition,
        mapViewDTO.selectedTileSourceKey,
        mapViewDTO.selectedOverlaysKeys,
        mapViewDTO.showMyLocationsOverlay,
        currentGeopoint,
        locationName,
    )

    protected constructor(`in`: Parcel) : this(
        `in`.readParcelable(GeoPoint::class.java.classLoader, GeoPoint::class.java)!!, // center
        `in`.readDouble(),
        `in`.readByte() == 1.toByte(),
        `in`.readInt(),
        `in`.readArrayList(ArrayList::class.java.classLoader, Integer::class.java)!!.stream().map { it.toInt() }.toList(),
        `in`.readByte() == 1.toByte(),
        `in`.readParcelable(GeoPoint::class.java.classLoader, GeoPoint::class.java), // currentGeopoint
        `in`.readString(), // locationName
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeParcelable(currentGeopoint, flags)
        dest.writeString(locationName)
    }

    companion object CREATOR: Parcelable.Creator<LocationPickerDTO?> {
        override fun createFromParcel(p0: Parcel) = LocationPickerDTO(p0)
        override fun newArray(p0: Int): Array<LocationPickerDTO?> = newArray(p0)
    }
}
