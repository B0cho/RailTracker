package com.b0cho.railtracker

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.osmdroid.util.GeoPoint
import java.sql.Timestamp

@Parcelize
@Entity(tableName = "my_locations")
data class MyLocation(
    @PrimaryKey(autoGenerate = true) val locationId: Int = 0,
    val name: String,
    val position: GeoPoint,
    val notes: String?,
    val timeCreated: Timestamp = Timestamp(System.currentTimeMillis()),

    // TODO: add columns naming 'column_name', add column for uri of marker photo
) : Parcelable
