package com.example.leafme.domain

// app/src/main/java/com/example/leafme/domain/AddPlantUseCase.kt

import com.example.leafme.data.AppRepository
import com.example.leafme.data.Plant

class AddPlantUseCase(private val repository: AppRepository) {
    suspend operator fun invoke(name: String, userId: Int) {
        repository.insertPlant(Plant(name = name, userId = userId))
    }
}