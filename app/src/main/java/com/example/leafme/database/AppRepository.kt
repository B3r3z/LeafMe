package com.example.leafme.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import com.example.leafme.data.User
import com.example.leafme.retrofit.RetrofitClient
import com.example.leafme.retrofit.CreatePlantRequest

class AppRepository(
    private val userDao: UserDao,
    private val plantDao: PlantDao,
    private val measurementDao: MeasurementDao
) {

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

            // Niezależnie od odpowiedzi serwera, usuwamy roślinę lokalnie
            withContext(Dispatchers.IO) {
                plantDao.deletePlant(plantId)
            }

            // Zwracamy true jeśli serwer potwierdził usunięcie
            response.isSuccessful
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
     * Synchronizuje rośliny z serwerem
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

                // Identyfikatory roślin na serwerze
                val serverPlantIds = serverPlants.map { it.id }.toSet()

                // Identyfikatory roślin lokalnie
                val localPlantIds = localPlants.map { it.id }.toSet()

                // Rośliny, które są na serwerze, ale nie ma ich lokalnie - dodaj lokalnie
                val plantsToAddLocally = serverPlants.filter { it.id !in localPlantIds }
                Log.d("AppRepository", "Liczba roślin do dodania lokalnie: ${plantsToAddLocally.size}")

                // Rośliny, które są lokalnie, ale nie ma ich na serwerze - dodaj na serwer
                val plantsToAddToServer = localPlants.filter { it.id !in serverPlantIds }
                Log.d("AppRepository", "Liczba roślin do dodania na serwer: ${plantsToAddToServer.size}")

                // Dodaj rośliny do lokalnej bazy danych
                withContext(Dispatchers.IO) {
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

                    // Jeśli mamy rośliny z serwera, ale nie mamy żadnych lokalnie po synchronizacji,
                    // dodajmy je wszystkie bezpośrednio
                    if (localPlants.isEmpty() && serverPlants.isNotEmpty()) {
                        Log.d("AppRepository", "Brak lokalnych roślin, dodaję wszystkie rośliny z serwera")
                        for (apiPlant in serverPlants) {
                            try {
                                val dbPlant = mapApiPlantToDbPlant(apiPlant, userId)
                                Log.d("AppRepository", "Dodawanie wszystkich roślin z serwera: id=${dbPlant.id}, name=${dbPlant.name}, userId=${dbPlant.userId}")
                                plantDao.insert(dbPlant)
                            } catch (e: Exception) {
                                Log.e("AppRepository", "Błąd podczas dodawania wszystkich roślin: ${e.message}")
                            }
                        }
                    }
                }

                // Dodaj rośliny na serwer
                for (plant in plantsToAddToServer) {
                    try {
                        val createRequest = CreatePlantRequest(
                            name = plant.name,
                            plantId = plant.id
                            // Usuwam userId, gdyż serwer i tak pobiera go z tokenu JWT
                        )
                        val createResponse = RetrofitClient.plantService.createPlant(createRequest)

                        if (!createResponse.isSuccessful) {
                            Log.e("AppRepository", "Błąd podczas dodawania rośliny na serwer: ${createResponse.errorBody()?.string()}")
                        } else {
                            Log.d("AppRepository", "Roślina dodana na serwer: ${plant.name}")
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Wyjątek podczas dodawania rośliny na serwer: ${e.message}")
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
                Log.e("AppRepository", "Błąd podczas pobierania roślin z serwera: ${response.errorBody()?.string()}")
                withContext(Dispatchers.IO) {
                    plantDao.getPlantsByUserId(userId)
                }
            }
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
                val serverMeasurements = response.body() ?: emptyList()
                Log.d("AppRepository", "Pobrano z serwera pomiarów: ${serverMeasurements.size}")

                val localMeasurements = withContext(Dispatchers.IO) {
                    measurementDao.getMeasurementsForPlantSorted(plantId)
                }
                Log.d("AppRepository", "Liczba pomiarów w bazie lokalnej: ${localMeasurements.size}")

                val localTimestamps = localMeasurements.map { it.timeStamp }.toSet()

                withContext(Dispatchers.IO) {
                    for (apiMeasurement in serverMeasurements) {
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
                Log.e("AppRepository", "Błąd podczas pobierania pomiarów z serwera: ${response.errorBody()?.string()}")
                val localMeasurements = withContext(Dispatchers.IO) {
                    measurementDao.getMeasurementsForPlantSorted(plantId)
                }
                Log.d("AppRepository", "Liczba pomiarów w bazie lokalnej (błąd serwera): ${localMeasurements.size}")
                localMeasurements
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Wyjątek podczas synchronizacji pomiarów: ${e.message}")
            val localMeasurements = withContext(Dispatchers.IO) {
                measurementDao.getMeasurementsForPlantSorted(plantId)
            }
            Log.d("AppRepository", "Liczba pomiarów w bazie lokalnej (wyjątek): ${localMeasurements.size}")
            localMeasurements
        }
    }

    // w pliku `app/src/main/java/com/example/leafme/database/AppRepository.kt`
    suspend fun waterPlant(plantId: Int): Boolean {
        return try {
            val response = RetrofitClient.plantService.waterPlant(plantId, com.example.leafme.retrofit.WaterRequest())
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

}

