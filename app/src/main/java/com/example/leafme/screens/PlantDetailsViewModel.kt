package com.example.leafme.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import com.example.leafme.database.AppRepository
import com.example.leafme.util.TokenExpiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel dla ekranu szczegółów rośliny
 */
class PlantDetailsViewModel(
    private val repository: AppRepository
) : ViewModel() {

    // Stan UI dla ekranu szczegółów
    private val _uiState = MutableStateFlow(PlantDetailsUiState())
    val uiState: StateFlow<PlantDetailsUiState> = _uiState.asStateFlow()

    /**
     * Ładuje dane rośliny i jej pomiary
     */
    fun loadPlantDetails(plantId: Int, rangeMinutes: Int = 15) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val plant = repository.getPlantById(plantId)
                val measurements = repository.syncMeasurementsWithServer(plantId)

                val now = System.currentTimeMillis() / 1000
                val fromTimestamp = now - (rangeMinutes * 60)
                val filtered = measurements.filter { it.timeStamp >= fromTimestamp }
                val chartMeasurementsRaw = if (filtered.isNotEmpty()) filtered else measurements
                val chartMeasurements = sampleEvery5Minutes(chartMeasurementsRaw)
                val lastMeasurement = measurements.firstOrNull()
                val lastWateringTime = lastMeasurement?.let {
                    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        .format(Date(it.timeStamp.toLong() * 1000))
                } ?: "Nigdy"

                _uiState.value = _uiState.value.copy(
                    plant = plant,
                    measurements = measurements,
                    chartMeasurements = chartMeasurements,
                    lastWateringTime = lastWateringTime,
                    lastMeasurement = lastMeasurement,
                    isLoading = false
                )
            } catch (e: TokenExpiredException) {
                Log.e("PlantDetailsViewModel", "Token wygasł podczas ładowania danych rośliny", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Sesja wygasła. Zaloguj się ponownie.",
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("PlantDetailsViewModel", "Błąd podczas ładowania danych rośliny: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Błąd podczas ładowania danych: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Próbkuje pomiary co 5 minut, aby zmniejszyć liczbę punktów na wykresie
     */
    private fun sampleEvery5Minutes(measurements: List<Measurement>): List<Measurement> {
        return measurements
            .sortedBy { it.timeStamp }
            .groupBy { it.timeStamp / 300 } // 300 sekund = 5 minut
            .map { (_, group) ->
                val avgMoisture = group.map { it.moisture }.average().toFloat()
                val avgTemp = group.map { it.temperature }.average().toFloat()
                val ts = group.last().timeStamp
                Measurement(
                    id = 0,
                    plantId = group.last().plantId,
                    timeStamp = ts,
                    moisture = avgMoisture,
                    temperature = avgTemp
                )
            }
    }
}

/**
 * Klasa reprezentująca stan UI dla ekranu szczegółów rośliny
 */
data class PlantDetailsUiState(
    val plant: Plant? = null,
    val measurements: List<Measurement> = emptyList(),
    val chartMeasurements: List<Measurement> = emptyList(),
    val lastMeasurement: Measurement? = null,
    val lastWateringTime: String = "Nigdy",
    val isRecent: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
