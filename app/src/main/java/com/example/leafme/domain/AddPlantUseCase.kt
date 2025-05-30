package com.example.leafme.domain

import com.example.leafme.database.AppRepository
import com.example.leafme.data.Plant
import com.example.leafme.retrofit.CreatePlantRequest
import com.example.leafme.retrofit.RetrofitClient
import android.util.Log

class AddPlantUseCase(private val repository: AppRepository) {
    suspend operator fun invoke(name: String, userId: Int, plantId: Int? = null) {
        try {
            // Dodaj szczegółowe logowanie dla diagnostyki
            Log.d("AddPlantUseCase", "Dodawanie rośliny: name=$name, userId=$userId, plantId=$plantId")

            // Najpierw dodaj roślinę lokalnie
            val localPlant = if (plantId != null) {
                Plant(id = plantId, name = name, userId = userId)
            } else {
                Plant(name = name, userId = userId)
            }
            repository.insertPlant(localPlant)
            Log.d("AddPlantUseCase", "Roślina dodana lokalnie: id=${localPlant.id}, name=${localPlant.name}")

            // Następnie dodaj roślinę na serwer
            // Serwer pobiera userId z tokenu JWT, więc nie musimy go przesyłać
            val request = CreatePlantRequest(name = name, plantId = plantId)
            val response = RetrofitClient.plantService.createPlant(request)

            if (response.isSuccessful) {
                val serverPlant = response.body()
                if (serverPlant != null) {
                    Log.d("AddPlantUseCase", "Roślina dodana na serwerze z ID: ${serverPlant.id}")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Nieznany błąd"
                Log.e("AddPlantUseCase", "Błąd podczas dodawania rośliny na serwer: $errorBody, kod: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("AddPlantUseCase", "Wyjątek podczas dodawania rośliny: ${e.message}", e)
        }
    }
}

