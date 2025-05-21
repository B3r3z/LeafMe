package com.example.leafme.database

import androidx.room.Dao

@Dao
interface PlantDao {
    @androidx.room.Insert
    suspend fun insert(plant: com.example.leafme.data.Plant)

    @androidx.room.Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getPlantById(plantId: Int): com.example.leafme.data.Plant?

    @androidx.room.Query("SELECT * FROM plants WHERE userId = :userId")
    suspend fun getPlantsByUserId(userId: Int): List<com.example.leafme.data.Plant>
}