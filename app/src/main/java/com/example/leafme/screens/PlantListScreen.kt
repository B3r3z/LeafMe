package com.example.leafme.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.leafme.database.AppRepository
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import com.example.leafme.auth.AuthManager
import com.example.leafme.LeafMeDestinations
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.leafme.util.TokenExpiredException
import android.util.Log

@Composable
fun PlantListScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var plants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var plantMeasurements by remember { mutableStateOf<Map<Int, List<Measurement>>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshPlants() {
        isRefreshing = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val syncedPlants = repository.syncPlantsWithServer(userId)
                plants = syncedPlants
                val newMeasurementsMap = mutableMapOf<Int, List<Measurement>>()
                for (plant in syncedPlants) {
                    val measurements = repository.syncMeasurementsWithServer(plant.id)
                    newMeasurementsMap[plant.id] = measurements
                }
                plantMeasurements = newMeasurementsMap
            } catch (e: TokenExpiredException) {
                Log.e("PlantListScreen", "Token wygasł podczas odświeżania.", e)
                authManager.logout() // To zainicjuje globalne przekierowanie
                errorMessage = "Sesja wygasła. Zaloguj się ponownie."
            } catch (e: Exception) {
                Log.e("PlantListScreen", "Błąd podczas synchronizacji: ${e.message}", e)
                errorMessage = "Błąd podczas synchronizacji: ${e.message}"
            } finally {
                isRefreshing = false
                isLoading = false
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId > 0) {
            try {
                plants = repository.syncPlantsWithServer(userId)
                val measurementsMap = mutableMapOf<Int, List<Measurement>>()
                plants.forEach { plant ->
                    measurementsMap[plant.id] = repository.syncMeasurementsWithServer(plant.id)
                }
                plantMeasurements = measurementsMap
            } catch (e: TokenExpiredException) {
                Log.e("PlantListScreen", "Token wygasł podczas LaunchedEffect.", e)
                authManager.logout() // To zainicjuje globalne przekierowanie
                errorMessage = "Sesja wygasła. Zaloguj się ponownie."
            } catch (e: Exception) {
                Log.e("PlantListScreen", "Błąd w LaunchedEffect: ${e.message}", e)
                errorMessage = "Nie można załadować danych: ${e.message}"
            }
        }
        isLoading = false
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = { refreshPlants() },
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // Najpierw wyczyść lokalne dane specyficzne dla użytkownika
                                    repository.clearAllLocalPlants()
                                    Log.d("PlantListScreen", "Lokalne dane roślin wyczyszczone przed wylogowaniem.")
                                } catch (e: Exception) {
                                    Log.e("PlantListScreen", "Błąd podczas czyszczenia lokalnych danych roślin: ${e.message}", e)
                                    // Można tu wyświetlić komunikat o błędzie, ale wylogowanie i tak powinno nastąpić
                                } finally {
                                    // Następnie wykonaj operację wylogowania w AuthManager
                                    authManager.logout() // To zmieni isLoggedInState, co powinno wywołać reaktywną nawigację
                                    // Jawna nawigacja nie jest już potrzebna, ponieważ MainActivity
                                    // reaguje na zmianę isLoggedInState.
                                }
                            }
                        }
                    ) {
                        Text("Wyloguj się")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Text("Ładowanie roślin...")
                } else if (plants.isEmpty()) {
                    Text("Brak roślin. Dodaj pierwszą roślinę!")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(plants) { plant ->
                            PlantCard(
                                plant = plant,
                                measurements = plantMeasurements[plant.id] ?: emptyList(),
                                onDelete = {
                                    coroutineScope.launch {
                                        try {
                                            val deleted = repository.deletePlant(plant.id)
                                            if (deleted) {
                                                refreshPlants()
                                            } else {
                                                errorMessage = "Nie udało się usunąć rośliny."
                                            }
                                        } catch (e: TokenExpiredException) {
                                            Log.e("PlantListScreen", "Token wygasł podczas usuwania rośliny.", e)
                                            authManager.logout() // To zainicjuje globalne przekierowanie
                                            errorMessage = "Sesja wygasła. Zaloguj się ponownie."
                                        } catch (e: Exception) {
                                            Log.e("PlantListScreen", "Błąd podczas usuwania rośliny: ${e.message}", e)
                                            errorMessage = "Błąd usuwania: ${e.message}"
                                        }
                                    }
                                },
                                onWaterPlant = {
                                    var success = false
                                    var error: String? = null
                                    try {
                                        success = repository.waterPlant(plant.id)
                                    } catch (e: TokenExpiredException) {
                                        Log.e("PlantListScreen", "Token wygasł podczas podlewania.", e)
                                        authManager.logout() // To zainicjuje globalne przekierowanie
                                        error = "Sesja wygasła. Zaloguj się ponownie."
                                    } catch (e: Exception) {
                                        Log.e("PlantListScreen", "Błąd podczas podlewania: ${e.message}", e)
                                        error = "Błąd podlewania: ${e.message}"
                                    }
                                    Pair(success, error)
                                },
                                onClick = {
                                    navController.navigate(LeafMeDestinations.PlantDetails.name + "/${plant.id}")
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantCard(
    plant: Plant,
    measurements: List<Measurement>,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onWaterPlant: suspend () -> Pair<Boolean, String?>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isWatering by remember { mutableStateOf(false) }
    var waterMsg by remember { mutableStateOf<String?>(null) }

    val lastMeasurement = measurements.firstOrNull()
    val lastWatering = lastMeasurement?.let {
        java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(it.timeStamp.toLong() * 1000))
    } ?: "Nigdy"

    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth()
            .clickable { onClick() }
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = plant.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onDelete,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Usuń")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Ostatnie podlewanie: $lastWatering")

            Spacer(modifier = Modifier.height(16.dp))

            if (lastMeasurement != null) {
                Text("Wilgotność gleby: %.1f%%".format(lastMeasurement.moisture))
                Text("Temperatura: %.1f°C".format(lastMeasurement.temperature))
            } else {
                Text("Brak danych o wilgotności i temperaturze")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isWatering = true
                    waterMsg = null
                    coroutineScope.launch {
                        val (success, error) = onWaterPlant()
                        isWatering = false
                        if (success) {
                            waterMsg = "Podlano!"
                        } else {
                            waterMsg = error ?: "Błąd podlewania"
                        }
                    }
                },
                enabled = !isWatering
            ) {
                if (isWatering) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                }
                Text("Podlej teraz")
            }

            if (waterMsg != null) {
                Text(
                    text = waterMsg!!,
                    color = if (waterMsg == "Podlano!") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

