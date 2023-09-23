package com.b0cho.railtracker

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationPickerActivity : OSMMapViewActivity() {
    companion object {
        // KEYS FOR INTENT EXTRAS

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        val toolbar = findViewById<Toolbar>(R.id.mapViewToolbar)
        setSupportActionBar(toolbar)

        val myLocationButton: FloatingActionButton = findViewById(R.id.myLocationActionButton2)
        myLocationButton.setOnClickListener{ super.moveToCurrentLocation() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val settingsMenuButton = super.onCreateOptionsMenu(menu)
        if(!settingsMenuButton)
            return false
        menu?.findItem(R.id.settingsMenuButton)?.isVisible = false
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}