package com.example.leafme.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import com.example.leafme.data.User
import com.example.leafme.retrofit.RetrofitClient
import com.example.leafme.retrofit.CreatePlantRequest
import com.example.leafme.util.TokenExpiredException
import com.example.leafme.auth.AuthManager

class AppRepository(
    private val userDao: UserDao,
    private val plantDao: PlantDao,
    private val measurementDao: MeasurementDao,
    private var authManager: AuthManager? = null
) {

    // Metoda do ustawiania AuthManager po utworzeniu AppRepository
    fun setAuthManager(manager: AuthManager) {
        this.authManager = manager
    }

    // Przykładowe metody dla User
    suspend fun insertUser(user: User) {
        withContext(Dispatchers.IO) {
            userDao.insert(user)
        }
    }

    suspend fun getUserById(userId: Int): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }

    // Metody dla Plant
    /**
     * Dodaje roślinę do lokalnej bazy danych
     * @param plant Roślina do dodania
     */
    suspend fun insertPlant(plant: Plant) {
        withContext(Dispatchers.IO) {
            plantDao.insert(plant)
        }
    }

    /**
     * Pobiera roślinę o podanym ID
     * @param plantId ID rośliny
     * @return Roślina lub null jeśli nie znaleziono
     */
    suspend fun getPlantById(plantId: Int): Plant? {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantById(plantId)
        }
    }

    suspend fun getPlantsByUserId(userId: Int): List<Plant> {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantsByUserId(userId)
        }
    }

    /**
     * Usuwa roślinę lokalnie i na serwerze
     * @param plantId ID rośliny do usunięcia
     * @return true jeśli usunięcie powiodło się, false w przeciwnym razie
     */
    suspend fun deletePlant(plantId: Int): Boolean {
        return try {
            // Najpierw próbujemy usunąć roślinę z serwera
            val response = RetrofitClient.plantService.deletePlant(plantId)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("AppRepository", "Błąd podczas usuwania rośliny na serwerze: $errorBody, kod: ${response.code()}")
                if (errorBody?.contains("Token has expired") == true) {
                    authManager?.logout()
                    throw TokenExpiredException()
                }
            }

            // Niezależnie od odpowiedzi serwera, usuwamy roślinę lokalnie
            withContext(Dispatchers.IO) {
                plantDao.deletePlant(plantId)
            }

            // Zwracamy true jeśli serwer potwierdził usunięcie
            response.isSuccessful
        } catch (e: TokenExpiredException) {
            throw e // Ponownie rzuć, aby UI mogło obsłużyć
        } catch (e: Exception) {
            // W przypadku błędu sieciowego, usuwamy tylko lokalnie
            Log.e("AppRepository", "Błąd podczas usuwania rośliny: ${e.message}")
            withContext(Dispatchers.IO) {
                try {
                    plantDao.deletePlant(plantId)
                    true // Usunięcie lokalne powiodło się
                } catch (dbException: Exception) {
                    Log.e("AppRepository", "Błąd podczas lokalnego usuwania rośliny: ${dbException.message}")
                    false // Usunięcie lokalne nie powiodło się
                }
            }
        }
    }

    /**
     * Usuwa wszystkie rośliny z lokalnej bazy danych.
     */
    suspend fun clearAllLocalPlants() {
        withContext(Dispatchers.IO) {
            plantDao.clearAllPlants()
            // Opcjonalnie można też wyczyścić pomiary, jeśli są przechowywane globalnie
            // measurementDao.clearAllMeasurements() // Jeśli taka metoda istnieje
        }
    }

    // Przykładowe metody dla Measurement
    suspend fun insertMeasurement(measurement: Measurement) {
        withContext(Dispatchers.IO) {
            measurementDao.insert(measurement)
        }
    }

    /**
     * Pobiera pomiary dla danej rośliny
     * @param plantId ID rośliny
     * @return Lista pomiarów posortowana po czasie
     */
    suspend fun getMeasurementsForPlant(plantId: Int): List<Measurement> {
        return withContext(Dispatchers.IO) {
            measurementDao.getMeasurementsForPlantSorted(plantId)
        }
    }

    /**
     * Pobiera listę roślin dla danego użytkownika
     * @param userId ID użytkownika
     * @return Lista roślin należących do użytkownika
     */
    suspend fun getPlantsForUser(userId: Int): List<Plant> {
        return withContext(Dispatchers.IO) {
            plantDao.getPlantsByUserId(userId)
        }
    }

    /**
     * Konwertuje roślinę z formatu API do formatu lokalnej bazy danych
     */
    private fun mapApiPlantToDbPlant(apiPlant: com.example.leafme.retrofit.Plant, userId: Int): Plant {
        return Plant(
            id = apiPlant.id,
            name = apiPlant.name,
            userId = apiPlant.userId ?: userId // Używaj userId z API, a jeśli brak to użyj lokalnego userId
        )
    }

    /**
     * Synchronizuje rośliny z serwerem, traktując serwer jako główne źródło prawdy
     * @param userId ID użytkownika, którego rośliny mają zostać zsynchronizowane
     * @return Lista roślin po synchronizacji
     */
    suspend fun syncPlantsWithServer(userId: Int): List<Plant> {
        return try {
            // Pobierz rośliny z serwera
            val response = RetrofitClient.plantService.getUserPlants()
            Log.d("AppRepository", "Odpowiedź serwera (getUserPlants): ${response.isSuccessful}, kod: ${response.code()}")

            if (response.isSuccessful) {
                val serverPlants = response.body() ?: emptyList()
                Log.d("AppRepository", "Liczba roślin z serwera: ${serverPlants.size}")

                // Wypisz szczegóły każdej rośliny dla diagnostyki
                serverPlants.forEach { plant ->
                    Log.d("AppRepository", "Roślina z serwera: id=${plant.id}, name=${plant.name}, userId=${plant.userId}")
                }

                // Pobierz rośliny z lokalnej bazy danych
                val localPlants = withContext(Dispatchers.IO) {
                    val plants = plantDao.getPlantsByUserId(userId)
                    Log.d("AppRepository", "Liczba lokalnych roślin: ${plants.size}")
                    plants
                }

                // Identyfikatory roślin na serwerze i lokalnie
                val serverPlantIds = serverPlants.map { it.id }.toSet()
                val localPlantIds = localPlants.map { it.id }.toSet()

                // 1. Dodaj lokalnie rośliny, które są na serwerze, a nie ma ich lokalnie
                val plantsToAddLocally = serverPlants.filter { it.id !in localPlantIds }
                Log.d("AppRepository", "Liczba roślin do dodania lokalnie: ${plantsToAddLocally.size}")

                // 2. Usuń lokalnie rośliny, których nie ma już na serwerze
                val plantsToRemoveLocally = localPlants.filter { it.id !in serverPlantIds }
                Log.d("AppRepository", "Liczba roślin do usunięcia lokalnie: ${plantsToRemoveLocally.size}")

                withContext(Dispatchers.IO) {
                    // Dodaj brakujące rośliny lokalnie
                    for (apiPlant in plantsToAddLocally) {
                        try {
                            // Konwertuj z formatu API do formatu bazy danych
                            val dbPlant = mapApiPlantToDbPlant(apiPlant, userId)
                            Log.d("AppRepository", "Dodawanie rośliny lokalnie: id=${dbPlant.id}, name=${dbPlant.name}, userId=${dbPlant.userId}")
                            plantDao.insert(dbPlant)
                        } catch (e: Exception) {
                            Log.e("AppRepository", "Błąd podczas dodawania rośliny lokalnie: ${e.message}")
                        }
                    }

                    // Usuń lokalnie rośliny, których nie ma na serwerze
                    for (plantToRemove in plantsToRemoveLocally) {
                        try {
                            Log.d("AppRepository", "Usuwanie rośliny lokalnie: id=${plantToRemove.id}, name=${plantToRemove.name}")
                            plantDao.deletePlant(plantToRemove.id)
                        } catch (e: Exception) {
                            Log.e("AppRepository", "Błąd podczas usuwania rośliny lokalnie: ${e.message}")
                        }
                    }
                }

                // Zwróć zaktualizowaną listę roślin
                withContext(Dispatchers.IO) {
                    val plants = plantDao.getPlantsByUserId(userId)
                    Log.d("AppRepository", "Liczba roślin po synchronizacji: ${plants.size}")
                    plants
                }
            } else {
                // W przypadku błędu, zwróć tylko lokalne rośliny
                val errorBody = response.errorBody()?.string()
                Log.e("AppRepository", "Błąd podczas pobierania roślin z serwera: $errorBody")
                if (errorBody?.contains("Token has expired") == true) {
                    authManager?.logout()
                    throw TokenExpiredException()
                }
                withContext(Dispatchers.IO) {
                    plantDao.getPlantsByUserId(userId)
                }
            }
        } catch (e: TokenExpiredException) {
            throw e // Ponownie rzuć, aby UI mogło obsłużyć
        } catch (e: Exception) {
            // W przypadku wyjątku, zwróć tylko lokalne rośliny
            Log.e("AppRepository", "Wyjątek podczas synchronizacji roślin: ${e.message}")
            withContext(Dispatchers.IO) {
                plantDao.getPlantsByUserId(userId)
            }
        }
    }

    /**
     * Konwertuje pomiar z formatu API do formatu lokalnej bazy danych
     */
    private fun mapApiMeasurementToDbMeasurement(
        apiMeasurement: com.example.leafme.retrofit.Measurement,
        plantId: Int
    ): Measurement {
        return Measurement(
            id = 0, // Room wygeneruje ID
            plantId = plantId,
            timeStamp = apiMeasurement.ts.toInt(),
            moisture = apiMeasurement.moisture,
            temperature = apiMeasurement.temperature
        )
    }

    /**
     * Synchronizuje pomiary dla danej rośliny z serwerem
     * @param plantId ID rośliny, której pomiary mają zostać zsynchronizowane
     * @return Lista pomiarów po synchronizacji
     */
    suspend fun syncMeasurementsWithServer(plantId: Int): List<Measurement> {
        Log.d("AppRepository", "Wywołano syncMeasurementsWithServer dla plantId=$plantId")
        return try {
            val response = RetrofitClient.plantService.getMeasurements(plantId)
            Log.d("AppRepository", "Odpowiedź serwera (getMeasurements): ${response.isSuccessful}, kod: ${response.code()}")

            if (response.isSuccessful) {
                val serverMeasurementsRaw = response.body() ?: emptyList()
                Log.d("AppRepository", "Pobrano z serwera pomiarów (raw): ${serverMeasurementsRaw.size}")

                // Odfiltruj duplikaty z odpowiedzi serwera na podstawie timestamp
                val serverMeasurements = serverMeasurementsRaw
                    .distinctBy { it.ts } // Usuwa duplikaty timestampów z odpowiedzi serwera
                Log.d("AppRepository", "Pobrano z serwera pomiarów (po deduplikacji): ${serverMeasurements.size}")

                val localMeasurements = withContext(Dispatchers.IO) {
                    measurementDao.getMeasurementsForPlantSorted(plantId)
                }
                Log.d("AppRepository", "Liczba pomiarów w bazie lokalnej: ${localMeasurements.size}")

                val localTimestamps = localMeasurements.map { it.timeStamp }.toSet()

                withContext(Dispatchers.IO) {
                    for (apiMeasurement in serverMeasurements) { // Iteruj po odfiltrowanej liście
                        if (apiMeasurement.ts.toInt() !in localTimestamps) {
                            try {
                                val dbMeasurement = mapApiMeasurementToDbMeasurement(apiMeasurement, plantId)
                                measurementDao.insert(dbMeasurement)
                                Log.d("AppRepository", "Dodano pomiar: ts=${apiMeasurement.ts}, moisture=${apiMeasurement.moisture}, temp=${apiMeasurement.temperature}")
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Błąd podczas dodawania pomiaru: ${e.message}")
                            }
                        }
                    }
                }

                val updatedMeasurements = withContext(Dispatchers.IO) {
                    measurementDao.getMeasurementsForPlantSorted(plantId)
                }
                Log.d("AppRepository", "Liczba pomiarów po synchronizacji: ${updatedMeasurements.size}")
                updatedMeasurements
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AppRepository", "Błąd podczas pobierania pomiarów z serwera: $errorBody")
                if (errorBody?.contains("Token has expired") == true) {
                    authManager?.logout()
                    throw TokenExpiredException()
                }
                val localMeasurements = withContext(Dispatchers.IO) {
                    measurementDao.getMeasurementsForPlantSorted(plantId)
                }
                Log.d("AppRepository", "Liczba pomiarów w bazie lokalnej (błąd serwera): ${localMeasurements.size}")
                localMeasurements
            }
        } catch (e: TokenExpiredException) {
            throw e // Ponownie rzuć, aby UI mogło obsłużyć
        } catch (e: Exception) {
            Log.e("AppRepository", "Wyjątek podczas synchronizacji pomiarów: ${e.message}")
            val localMeasurements = withContext(Dispatchers.IO) {
                measurementDao.getMeasurementsForPlantSorted(plantId)
            }
            Log.d("AppRepository", "Liczba pomiarów w bazie lokalnej (wyjątek): ${localMeasurements.size}")
            localMeasurements
        }
    }

    suspend fun waterPlant(plantId: Int): Boolean {
        return try {
            val response = RetrofitClient.plantService.waterPlant(plantId, com.example.leafme.retrofit.WaterRequest())
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e("AppRepository", "Błąd podczas podlewania rośliny na serwerze: $errorBody, kod: ${response.code()}")
                if (errorBody?.contains("Token has expired") == true) {
                    authManager?.logout()
                    throw TokenExpiredException()
                }
                return false // Wskazuje na niepowodzenie
            }
            response.isSuccessful
        } catch (e: TokenExpiredException) {
            throw e // Ponownie rzuć, aby UI mogło obsłużyć
        } catch (e: Exception) {
            Log.e("AppRepository", "Wyjątek podczas podlewania rośliny: ${e.message}")
            false
        }
    }
}

