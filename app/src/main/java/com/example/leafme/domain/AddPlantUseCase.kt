package com.example.leafme.domain



import com.example.leafme.database.AppRepository
import com.example.leafme.data.Plant
import com.example.leafme.retrofit.CreatePlantRequest
import com.example.leafme.retrofit.RetrofitClient
import android.util.Log

class AddPlantUseCase(private val repository: AppRepository) {
    suspend operator fun invoke(name: String, userId: Int) {
        try {
            // 1. Najpierw dodajemy roślinę do lokalnej bazy danych
            val localPlant = Plant(name = name, userId = userId)
            repository.insertPlant(localPlant)

            // 2. Próbujemy dodać roślinę na serwer
            val request = CreatePlantRequest(name = name)
            val response = RetrofitClient.plantService.createPlant(request)

            if (response.isSuccessful) {
                // Jeśli dodanie na serwer się powiodło, pobieramy identyfikator
                // i aktualizujemy lokalną bazę danych
                val serverPlant = response.body()
                if (serverPlant != null) {
                    Log.d("AddPlantUseCase", "Roślina dodana na serwerze z ID: ${serverPlant.id}")
                }
            } else {
                Log.e("AddPlantUseCase", "Błąd podczas dodawania rośliny na serwer: ${response.errorBody()?.string()}")
                // Synchronizacja nastąpi później, podczas wywoływania syncPlantsWithServer
            }
        } catch (e: Exception) {
            Log.e("AddPlantUseCase", "Wyjątek podczas dodawania rośliny: ${e.message}")
            // Roślina została dodana lokalnie, synchronizacja nastąpi później
        }
    }
}

