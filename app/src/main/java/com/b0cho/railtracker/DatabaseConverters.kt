package com.b0cho.railtracker

import android.net.Uri
import android.util.Log
import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.b0cho.railtracker.App.logTAG
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint
import java.sql.Timestamp

@ProvidedTypeConverter
class DatabaseConverters(private val dbGsonBuilder: GsonBuilder) {
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

    @TypeConverter
    fun fromUriList(value: List<Uri>): String {
        val json = dbGsonBuilder.create().toJson(value, object : TypeToken<List<Uri>>(){}.type)
        Log.d(logTAG, ("conversion to JSON: $value $json"))
        return json
    }

    @TypeConverter
    fun toUriList(value: String): List<Uri> {
        val list: List<Uri> = dbGsonBuilder.create().fromJson(value, object : TypeToken<List<Uri>>(){}.type)
        Log.d(logTAG, "conversion from JSON: $value $list")
        return list
    }

    @TypeConverter
    fun fromUri(value: Uri?): String? = value?.toString()

    @TypeConverter
    fun toUri(value: String?): Uri? = if(value.isNullOrEmpty()) null else Uri.parse(value)
}