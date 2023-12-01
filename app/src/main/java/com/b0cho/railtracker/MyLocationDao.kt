package com.b0cho.railtracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MyLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pinLocation: MyLocation): Long

    @Query("SELECT * FROM my_locations")
    fun getAll(): List<MyLocation>


}
