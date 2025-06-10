package com.example.leafme.domain

import com.example.leafme.database.AppRepository
import com.example.leafme.data.Plant
import com.example.leafme.retrofit.CreatePlantRequest
import com.example.leafme.retrofit.RetrofitClient
import android.util.Log
import com.example.leafme.util.TokenExpiredException
import com.example.leafme.auth.AuthManager

class AddPlantUseCase(private val repository: AppRepository, private val authManager: AuthManager?) {
    suspend operator fun invoke(name: String, userId: Int, providedPlantId: Int? = null): Plant {
        Log.d("AddPlantUseCase", "Rozpoczęcie dodawania rośliny: name=$name, userId=$userId, providedPlantId=$providedPlantId")

        try {
            if (providedPlantId == null) {
                // Użytkownik nie podał ID, więc najpierw tworzymy na serwerze, aby uzyskać ID
                Log.d("AddPlantUseCase", "Brak ID od użytkownika, tworzenie na serwerze...")
                val serverRequest = CreatePlantRequest(name = name, plantId = null) // plantId jest null
                val serverResponse = RetrofitClient.plantService.createPlant(serverRequest)

                if (serverResponse.isSuccessful) {
                    val serverPlantResponse = serverResponse.body()
                    if (serverPlantResponse != null && serverPlantResponse.id != 0) {
                        Log.d("AddPlantUseCase", "Roślina utworzona na serwerze z ID: ${serverPlantResponse.id}")
                        // Użyj ID z serwera do utworzenia i zapisania lokalnej rośliny
                        val newPlant = Plant(id = serverPlantResponse.id, name = name, userId = userId)
                        repository.insertPlant(newPlant)
                        Log.d("AddPlantUseCase", "Roślina zapisana lokalnie z ID serwera: ${newPlant.id}")
                        return newPlant
                    } else {
                        Log.e("AddPlantUseCase", "Serwer nie zwrócił poprawnego ID dla nowej rośliny. Odpowiedź: $serverPlantResponse")
                        throw Exception("Serwer nie zwrócił poprawnego ID dla nowej rośliny.")
                    }
                } else {
                    val errorBody = serverResponse.errorBody()?.string() ?: "Nieznany błąd serwera"
                    Log.e("AddPlantUseCase", "Błąd serwera (bez ID od użytkownika): $errorBody, kod: ${serverResponse.code()}")
                    if (errorBody.contains("Token has expired")) {
                        authManager?.logout()
                        throw TokenExpiredException(errorBody)
                    }
                    throw Exception("Błąd serwera podczas tworzenia rośliny (bez ID): $errorBody")
                }
            } else {
                // Użytkownik podał ID, więc najpierw próbujemy utworzyć na serwerze z tym ID
                Log.d("AddPlantUseCase", "Użytkownik podał ID: $providedPlantId, próba utworzenia na serwerze...")
                val serverRequest = CreatePlantRequest(name = name, plantId = providedPlantId)
                val serverResponse = RetrofitClient.plantService.createPlant(serverRequest)

                if (serverResponse.isSuccessful) {
                    val serverPlantResponse = serverResponse.body()
                    // Sprawdź, czy serwer potwierdził ID (powinno być takie samo jak providedPlantId)
                    if (serverPlantResponse != null && serverPlantResponse.id == providedPlantId) {
                        Log.d("AddPlantUseCase", "Roślina utworzona/potwierdzona na serwerze z ID: $providedPlantId")
                        // Zapisz lokalnie z podanym ID
                        val newPlant = Plant(id = providedPlantId, name = name, userId = userId)
                        repository.insertPlant(newPlant)
                        Log.d("AddPlantUseCase", "Roślina zapisana lokalnie z podanym ID: ${newPlant.id}")
                        return newPlant
                    } else {
                        Log.e("AddPlantUseCase", "Niespójność ID między żądaniem a odpowiedzią serwera. Oczekiwano ${providedPlantId}, otrzymano ${serverPlantResponse?.id}")
                        throw Exception("Niespójność ID z serwerem.")
                    }
                } else {
                    val errorBody = serverResponse.errorBody()?.string() ?: "Nieznany błąd serwera"
                    Log.e("AddPlantUseCase", "Błąd serwera (z ID od użytkownika $providedPlantId): $errorBody, kod: ${serverResponse.code()}")
                    if (errorBody.contains("Token has expired")) {
                        authManager?.logout()
                        throw TokenExpiredException(errorBody)
                    }
                    // Jeśli serwer zwrócił 409 (Plant ID already exists), nie zapisuj lokalnie i rzuć błąd
                    if (serverResponse.code() == 409) {
                        throw Exception("Roślina o podanym ID ($providedPlantId) już istnieje na serwerze.")
                    }
                    throw Exception("Błąd serwera podczas tworzenia rośliny (z ID $providedPlantId): $errorBody")
                }
            }
        } catch (e: TokenExpiredException) {
            Log.w("AddPlantUseCase", "Token wygasł podczas operacji dodawania rośliny.", e)
            throw e
        } catch (e: Exception) {
            Log.e("AddPlantUseCase", "Wyjątek podczas dodawania rośliny: ${e.message}", e)
            // Tutaj nie zapisujemy lokalnie, jeśli operacja na serwerze się nie powiodła lub wystąpił inny błąd
            throw e
        }
    }
}

