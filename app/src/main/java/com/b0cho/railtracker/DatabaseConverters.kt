package com.b0cho.railtracker

import androidx.room.TypeConverter
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.GeoPoint
import java.sql.Timestamp

class DatabaseConverters {
    @TypeConverter
    fun fromTimestamp(value: Timestamp?): Long? {
        return value?.nanos?.toLong()
    }

    @TypeConverter
    fun toTimestamp(value: Long?): Timestamp? {
        return value?.let { Timestamp(it) }
    }

    @TypeConverter
    fun fromGeoPoint(value: GeoPoint): String {
        return value.toDoubleString()
    }

    @TypeConverter
    fun toGeoPoint(value: String): GeoPoint {
        return GeoPoint.fromDoubleString(value, ',')
    }
}