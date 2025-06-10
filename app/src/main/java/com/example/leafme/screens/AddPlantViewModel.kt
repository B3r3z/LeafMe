package com.example.leafme.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafme.domain.AddPlantUseCase
import kotlinx.coroutines.launch
import com.example.leafme.util.TokenExpiredException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AddPlantViewModel(private val addPlantUseCase: AddPlantUseCase) : ViewModel() {
    // Stan dla obsługi błędów
    private val _errorState = MutableStateFlow<Exception?>(null)
    val errorState: StateFlow<Exception?> = _errorState.asStateFlow()

    // Stan dla obsługi statusu operacji
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun addPlant(name: String, userId: Int, plantId: Int? = null, onSuccess: () -> Unit) {
        _isLoading.value = true
        _errorState.value = null

        viewModelScope.launch {
            try {
                addPlantUseCase(name, userId, plantId)
                _isLoading.value = false
                onSuccess()
            } catch (e: TokenExpiredException) {
                Log.w("AddPlantViewModel", "Token wygasł podczas dodawania rośliny", e)
                _errorState.value = e
                _isLoading.value = false
            } catch (e: CancellationException) {
                // Nie przechwytuj wyjątków związanych z anulowaniem korutyny
                throw e
            } catch (e: Exception) {
                Log.e("AddPlantViewModel", "Błąd podczas dodawania rośliny", e)
                _errorState.value = e
                _isLoading.value = false
            }
        }
    }

    // Resetuje stan błędu
    fun clearError() {
        _errorState.value = null
    }
}

