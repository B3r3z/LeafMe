package com.example.leafme.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafme.auth.AuthManager
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import com.example.leafme.database.AppRepository
import com.example.leafme.util.TokenExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel dla ekranu listy roślin
 */
class PlantListViewModel(
    private val repository: AppRepository,
    private val authManager: AuthManager
) : ViewModel() {

    // Stan ekranu
    private val _uiState = MutableStateFlow(PlantListUiState())
    val uiState: StateFlow<PlantListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Ładuje wszystkie dane (rośliny i ich pomiary)
     */
    fun loadData() {
        val userId = authManager.getUserId()
        if (userId <= 0) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val syncedPlants = repository.syncPlantsWithServer(userId)
                val newMeasurementsMap = mutableMapOf<Int, List<Measurement>>()

                for (plant in syncedPlants) {
                    val measurements = repository.syncMeasurementsWithServer(plant.id)
                    newMeasurementsMap[plant.id] = measurements
                }

                _uiState.value = _uiState.value.copy(
                    plants = syncedPlants,
                    plantMeasurements = newMeasurementsMap,
                    isLoading = false
                )
            } catch (e: TokenExpiredException) {
                Log.e("PlantListViewModel", "Token wygasł podczas odświeżania.", e)
                authManager.logout()
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Sesja wygasła. Zaloguj się ponownie.",
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("PlantListViewModel", "Błąd podczas synchronizacji: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Błąd podczas synchronizacji: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Usuwa roślinę
     */
    fun deletePlant(plantId: Int) {
        viewModelScope.launch {
            try {
                val deleted = repository.deletePlant(plantId)
                if (deleted) {
                    loadData() // Przeładuj dane po usunięciu
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Nie udało się usunąć rośliny."
                    )
                }
            } catch (e: TokenExpiredException) {
                Log.e("PlantListViewModel", "Token wygasł podczas usuwania rośliny.", e)
                authManager.logout()
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Sesja wygasła. Zaloguj się ponownie."
                )
            } catch (e: Exception) {
                Log.e("PlantListViewModel", "Błąd podczas usuwania rośliny: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Błąd usuwania: ${e.message}"
                )
            }
        }
    }

    /**
     * Podlewa roślinę
     */
    fun waterPlant(plantId: Int, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.waterPlant(plantId)
                onComplete(success, if (success) null else "Nie udało się podlać rośliny")
            } catch (e: TokenExpiredException) {
                Log.e("PlantListViewModel", "Token wygasł podczas podlewania.", e)
                authManager.logout()
                onComplete(false, "Sesja wygasła. Zaloguj się ponownie.")
            } catch (e: Exception) {
                Log.e("PlantListViewModel", "Błąd podczas podlewania: ${e.message}", e)
                onComplete(false, "Błąd podlewania: ${e.message}")
            }
        }
    }

    /**
     * Wylogowuje użytkownika
     */
    fun logout() {
        viewModelScope.launch {
            try {
                repository.clearAllLocalPlants()
                Log.d("PlantListViewModel", "Lokalne dane roślin wyczyszczone przed wylogowaniem.")
            } catch (e: Exception) {
                Log.e("PlantListViewModel", "Błąd podczas czyszczenia lokalnych danych: ${e.message}", e)
            } finally {
                authManager.logout()
            }
        }
    }
}

/**
 * Klasa reprezentująca stan UI dla ekranu listy roślin
 */
data class PlantListUiState(
    val plants: List<Plant> = emptyList(),
    val plantMeasurements: Map<Int, List<Measurement>> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
