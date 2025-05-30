package com.example.leafme.screens

// app/src/main/java/com/example/leafme/screens/AddPlantViewModel.kt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafme.domain.AddPlantUseCase
import kotlinx.coroutines.launch
import com.example.leafme.util.TokenExpiredException

class AddPlantViewModel(private val addPlantUseCase: AddPlantUseCase) : ViewModel() {
    fun addPlant(name: String, userId: Int, plantId: Int? = null, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                addPlantUseCase(name, userId, plantId)
                onSuccess()
            } catch (e: TokenExpiredException) {
                Log.w("AddPlantViewModel", "Token wygasł, rzucam dalej do UI.", e)
                throw e
            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "Błąd podczas dodawania rośliny", e)
                throw e
            }
        }
    }
}

