package com.example.leafme.screens

// app/src/main/java/com/example/leafme/screens/AddPlantViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafme.domain.AddPlantUseCase
import kotlinx.coroutines.launch

class AddPlantViewModel(private val addPlantUseCase: AddPlantUseCase) : ViewModel() {
    fun addPlant(name: String, userId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            addPlantUseCase(name, userId)
            onSuccess()
        }
    }
}