package com.b0cho.railtracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.osmdroid.util.GeoPoint
import java.sql.Timestamp

@Entity(tableName = "my_locations")
data class MyLocation(
    @PrimaryKey(autoGenerate = true) val locationId: Int = 0,
    val name: String,
    val position: GeoPoint,
    val notes: String?,
    val timeCreated: Timestamp = Timestamp(System.currentTimeMillis()),

    // TODO: add columns naming 'column_name', add column for uri of marker photo
)
