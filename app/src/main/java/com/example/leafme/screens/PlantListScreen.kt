package com.example.leafme.screens

import androidx.compose.foundation.layout.Arrangement
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun PlantListScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    var plants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Funkcja do odświeżania listy roślin
    fun refreshPlants() {
        coroutineScope.launch {
            plants = repository.syncPlantsWithServer(userId)
        }
    }

    // Pobierz listę roślin przy uruchomieniu ekranu
    LaunchedEffect(userId) {
        if (userId > 0) {
            // Zamiast tylko pobierać lokalne rośliny, synchronizujemy je z serwerem
            plants = repository.syncPlantsWithServer(userId)
        }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Przycisk wylogowania
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                authManager.logout()
                navController.navigate(LeafMeDestinations.LoginRegister.name) {
                    popUpTo(0) { inclusive = true }
                }
            }) {
                Text("Wyloguj się")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reszta zawartości ekranu
        if (isLoading) {
            Text("Wczytywanie roślin...")
        } else if (plants.isEmpty()) {
            Text("Nie masz jeszcze żadnych roślin. Dodaj swoją pierwszą roślinę!")
        } else {
            // Lista roślin
            LazyColumn {
                items(plants) { plant ->
                    PlantCard(
                        plant = plant,
                        repository = repository,
                        onDelete = {
                            coroutineScope.launch {
                                // Usunięcie rośliny
                                val success = repository.deletePlant(plant.id)
                                if (success) {
                                    // Odśwież całą listę po usunięciu zamiast tylko lokalnego filtrowania
                                    refreshPlants()
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// app/src/main/java/com/example/leafme/screens/PlantListScreen.kt

@Composable
fun PlantCard(
    plant: Plant,
    repository: AppRepository,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Pobierz pomiary przy pierwszym renderowaniu i kiedy zmienia się roślina
    LaunchedEffect(plant) {
        measurements = repository.syncMeasurementsWithServer(plant.id)
    }

    val lastMeasurement = measurements.firstOrNull()
    val lastWatering = lastMeasurement?.let {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(it.timeStamp.toLong() * 1000))
    } ?: "Nigdy"

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
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

            // Nowe: wyświetlanie wilgotności i temperatury
            if (lastMeasurement != null) {
                Text("Wilgotność gleby: %.1f%%".format(lastMeasurement.moisture))
                Text("Temperatura: %.1f°C".format(lastMeasurement.temperature))
            } else {
                Text("Brak danych o wilgotności i temperaturze")
            }
        }
    }
}

