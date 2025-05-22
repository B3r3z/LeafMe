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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.leafme.data.AppRepository
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import com.example.leafme.database.MeasurementDao
import com.example.leafme.database.PlantDao
import com.example.leafme.database.UserDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlantListScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    modifier: Modifier = Modifier // Dodano parametr modifier
) {
    var plants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // TODO: Rozważ dodanie obsługi wylogowania użytkownika, np. przycisk w UI,
    // który nawigowałby z powrotem do ekranu logowania.

    LaunchedEffect(userId) { // Zmieniono z Unit na userId, aby ponownie ładować dane, jeśli userId się zmieni
        // TODO: Upewnij się, że `userId` jest prawidłowy przed próbą pobrania danych.
        // Jeśli `userId` może być np. -1 dla niezalogowanego użytkownika, dodaj odpowiednią obsługę.
        plants = repository.getPlantsByUserId(userId)
        isLoading = false
    }

    Column(
        modifier = modifier // Zastosowano przekazany modifier
            .fillMaxSize()
            .padding(16.dp) // Ten padding jest wewnętrzny dla PlantListScreen, zachowaj go lub dostosuj
    ) {
        Text(
            text = "Plant Care App",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Text("Loading plants...")
        } else if (plants.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("No plants added yet")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // TODO: Przed nawigacją do "addPlant", upewnij się, że użytkownik jest zalogowany
                        // i przekaż odpowiednie dane (np. userId) do ekranu dodawania rośliny, jeśli to konieczne.
                        navController.navigate("addPlant")
                    }
                ) {
                    Text("Add Your First Plant")
                }
            }
        } else {
            LazyColumn {
                items(plants) { plant ->
                    PlantCard(
                        plant = plant,
                        repository = repository,
                        onWaterNow = { /* TODO: Implement API call to water plant, upewnij się, że użytkownik jest autoryzowany */ },
                        onDelete = { /* TODO: Implement plant deletion, upewnij się, że użytkownik jest autoryzowany do usunięcia tej rośliny */ },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PlantCard(
    plant: Plant,
    repository: AppRepository,
    onWaterNow: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }

    LaunchedEffect(plant) {
        measurements = repository.getMeasurementsForPlant(plant.plantId)
    }

    val lastWatering = measurements.firstOrNull()?.let {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(it.timeStamp.toLong() * 1000))
    } ?: "Never"

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
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Last watered: $lastWatering")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Moisture and temperature indicators
                measurements.firstOrNull()?.let { lastMeasurement ->
                    Column {
                        Text("Moisture: %.1f%%".format(lastMeasurement.moisture))
                        Text("Temperature: %.1f°C".format(lastMeasurement.temperature))
                    }
                }

                Button(onClick = onWaterNow) {
                    Text("Water Now")
                }
            }
        }
    }
}


@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun PlantListScreenPreview() {
    MaterialTheme {
        // Mock DAOs to pass to the mock repository
        val mockUserDao = object : UserDao {
            override suspend fun insert(user: com.example.leafme.data.User) {}
            override suspend fun getUserById(userId: Int): com.example.leafme.data.User? = null
        }
        val mockPlantDao = object : PlantDao {
            override suspend fun insert(plant: com.example.leafme.data.Plant) {}
            override suspend fun getPlantById(plantId: Int): com.example.leafme.data.Plant? = null
            override suspend fun getPlantsByUserId(userId: Int): List<com.example.leafme.data.Plant> {
                return listOf(
                    Plant(plantId = 1, name = "Mleczyk", userId = 1),
                    Plant(plantId = 2, name = "Bazylia", userId = 1)
                )
            }
        }
        val mockMeasurementDao = object : MeasurementDao {
            override suspend fun insert(measurement: Measurement) {}
            override suspend fun getMeasurementsForPlantSorted(plantId: Int): List<Measurement> {
                return listOf(
                    Measurement(
                        plantId = plantId,
                        timeStamp = (System.currentTimeMillis() / 1000).toInt(),
                        moisture = 65.5f,
                        temperature = 22.3f
                    )
                )
            }
        }

        // Create an instance of AppRepository with mock DAOs
        val mockRepository = AppRepository(mockUserDao, mockPlantDao, mockMeasurementDao)

        PlantListScreen(
            navController = rememberNavController(),
            repository = mockRepository,
            userId = 1 // TOD O: W podglądzie używamy userId = 1. W rzeczywistej aplikacji
            // ten ID będzie pochodził z zalogowanego użytkownika.
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PlantCardPreview() {
    MaterialTheme {
        val mockPlant = Plant(plantId = 1, name = "Mleczyk", userId = 1)
        val mockMeasurement = Measurement(
            plantId = 1,
            timeStamp = (System.currentTimeMillis() / 1000).toInt(),
            moisture = 65.5f,
            temperature = 22.3f
        )

        // Mock DAOs for PlantCardPreview
        val mockUserDao = object : UserDao {
            override suspend fun insert(user: com.example.leafme.data.User) {}
            override suspend fun getUserById(userId: Int): com.example.leafme.data.User? = null
        }
        val mockPlantDao = object : PlantDao {
            override suspend fun insert(plant: com.example.leafme.data.Plant) {}
            override suspend fun getPlantById(plantId: Int): com.example.leafme.data.Plant? = null
            override suspend fun getPlantsByUserId(userId: Int): List<com.example.leafme.data.Plant> = emptyList()
        }
        val mockMeasurementDao = object : MeasurementDao {
            override suspend fun insert(measurement: Measurement) {}
            override suspend fun getMeasurementsForPlantSorted(plantId: Int) = listOf(mockMeasurement)
        }

        PlantCard(
            plant = mockPlant,
            repository = AppRepository(mockUserDao, mockPlantDao, mockMeasurementDao),
            onWaterNow = {},
            onDelete = {}
        )
    }
}