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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun PlantListScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    var plants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    //var measurementsMap by remember { mutableStateOf<Map<Int, List<Measurement>>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    // W PlantListScreen.kt
    var plantMeasurements by remember { mutableStateOf<Map<Int, List<Measurement>>>(emptyMap()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    fun refreshPlants() {
        isRefreshing = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val syncedPlants = repository.syncPlantsWithServer(userId)
                plants = syncedPlants
                // Synchronizuj pomiary dla każdej rośliny
                val newMeasurementsMap = mutableMapOf<Int, List<Measurement>>()
                for (plant in syncedPlants) {
                    val measurements = repository.syncMeasurementsWithServer(plant.id)
                    newMeasurementsMap[plant.id] = measurements
                }
                // KLUCZOWA ZMIANA:
                plantMeasurements = newMeasurementsMap
            } catch (e: Exception) {
                errorMessage = "Błąd podczas synchronizacji: ${e.message}"
            } finally {
                isRefreshing = false
                isLoading = false
            }
        }
    }


    LaunchedEffect(userId) {
        if (userId > 0) {
            plants = repository.syncPlantsWithServer(userId)
            val measurementsMap = mutableMapOf<Int, List<Measurement>>()
            plants.forEach { plant ->
                measurementsMap[plant.id] = repository.syncMeasurementsWithServer(plant.id)
            }
            plantMeasurements = measurementsMap
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
                            authManager.logout()
                            navController.navigate(LeafMeDestinations.LoginRegister.name) {
                                popUpTo(LeafMeDestinations.PlantList.name) { inclusive = true }
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
                                repository = repository,
                                onDelete = {
                                    coroutineScope.launch {
                                        repository.deletePlant(plant.id)
                                        refreshPlants()
                                    }
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

// app/src/main/java/com/example/leafme/screens/PlantListScreen.kt

// Dodaj importy:


@Composable
fun PlantCard(
    plant: Plant,
    measurements: List<Measurement>,
    repository: AppRepository,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    //var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var isWatering by remember { mutableStateOf(false) }
    var waterMsg by remember { mutableStateOf<String?>(null) }
/*
    LaunchedEffect(plant) {
        measurements = repository.syncMeasurementsWithServer(plant.id)
    }
*/
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

            // Przycisk "Podlej teraz"
            Button(
                onClick = {
                    isWatering = true
                    waterMsg = null
                    coroutineScope.launch {
                        val success = repository.waterPlant(plant.id)
                        isWatering = false
                        waterMsg = if (success) "Podlano!" else "Błąd podlewania"
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

