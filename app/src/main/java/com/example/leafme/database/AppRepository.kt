package com.example.leafme.data // Lub inna odpowiednia paczka, np. com.example.leafme.repository

import com.example.leafme.database.UserDao
import com.example.leafme.database.PlantDao
import com.example.leafme.database.MeasurementDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    // Przykładowe metody dla Plant
    suspend fun insertPlant(plant: Plant) {
        withContext(Dispatchers.IO) {
            plantDao.insert(plant)
        }
    }

    suspend fun getPlantById(plantId: Int): Plant? { // Zmieniono typ plantId na Int
        return withContext(Dispatchers.IO) {
            plantDao.getPlantById(plantId)
        }
    }

    suspend fun getPlantsByUserId(userId: Int): List<Plant> { // Zmieniono typ userId na Int
        return withContext(Dispatchers.IO) {
            plantDao.getPlantsByUserId(userId)
        }
    }

    // Przykładowe metody dla Measurement
    suspend fun insertMeasurement(measurement: Measurement) {
        withContext(Dispatchers.IO) {
            measurementDao.insert(measurement)
        }
    }

    suspend fun getMeasurementsForPlant(plantId: Int): List<Measurement> {
        return withContext(Dispatchers.IO) {
            measurementDao.getMeasurementsForPlantSorted(plantId)
        }
    }

    //ToDo API implementacja
}