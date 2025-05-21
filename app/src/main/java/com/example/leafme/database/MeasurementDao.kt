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
    @Query("SELECT * FROM measurements WHERE plantId = :plantId")
    suspend fun getMeasurementsForPlantSorted(plantId: Int): List<Measurement>
}