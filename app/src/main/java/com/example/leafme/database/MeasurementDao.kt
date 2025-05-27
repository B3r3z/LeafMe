package com.example.leafme.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.leafme.data.Measurement

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insert(measurement: Measurement)
    //@Query("SELECT * FROM measurements")
    //suspend fun getAllMeasurements(): List<Measurement>
    @Query("SELECT * FROM measurements WHERE plantId = :plantId AND timeStamp = :timeStamp LIMIT 1")
    suspend fun getMeasurementByPlantAndTimestamp(plantId: Int, timeStamp: Int): Measurement?

    @Query("SELECT * FROM measurements WHERE plantId = :plantId ORDER BY timeStamp DESC")
    suspend fun getMeasurementsForPlantSorted(plantId: Int): List<Measurement>
}