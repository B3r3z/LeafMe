package com.example.leafme.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.leafme.data.Plant

@Dao
interface PlantDao {
    @Insert
    suspend fun insert(plant: Plant)

    @Query("SELECT * FROM plants WHERE plantId = :plantId")
    suspend fun getPlantById(plantId: Int): Plant?

    @Query("SELECT * FROM plants WHERE userId = :userId")
    suspend fun getPlantsByUserId(userId: Int): List<Plant>

    @Query("DELETE FROM plants WHERE plantId = :plantId")
    suspend fun deletePlant(plantId: Int)

    @Query("DELETE FROM plants")
    suspend fun clearAllPlants()
}
