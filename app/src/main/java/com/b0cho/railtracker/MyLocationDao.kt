package com.b0cho.railtracker

import androidx.lifecycle.LiveData
import androidx.room.*
import io.reactivex.rxjava3.core.Completable

@Dao
interface MyLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pinLocation: MyLocation): Long

    @Query("SELECT * FROM my_locations")
    fun getAll(): LiveData<List<MyLocation>>

    @Delete
    fun delete(location: MyLocation): Completable
}
