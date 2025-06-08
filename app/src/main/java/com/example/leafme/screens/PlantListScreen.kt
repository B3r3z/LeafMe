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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlantListScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    viewModel: PlantListViewModel = viewModel { PlantListViewModel(repository, authManager) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        if (userId > 0) {
            viewModel.loadData()
        }
    }

    SwipeRefresh(
        state = rememberSwipeRefreshState(uiState.isRefreshing),
        onRefresh = { viewModel.loadData() },
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
                        onClick = { viewModel.logout() }
                    ) {
                        Text("Wyloguj się")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    Text("Ładowanie roślin...")
                } else if (uiState.plants.isEmpty()) {
                    Text("Brak roślin. Dodaj pierwszą roślinę!")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.plants) { plant ->
                            PlantCard(
                                plant = plant,
                                measurements = uiState.plantMeasurements[plant.id] ?: emptyList(),
                                onDelete = { viewModel.deletePlant(plant.id) },
                                onWaterPlant = { onComplete ->
                                    viewModel.waterPlant(plant.id, onComplete)
                                },
                                onClick = {
                                    navController.navigate(LeafMeDestinations.PlantDetails.name + "/${plant.id}")
                                },
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }
                }

                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
    onWaterPlant: ((Boolean, String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var isWatering by remember { mutableStateOf(false) }
    var waterMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val lastMeasurement = measurements.firstOrNull()
    val lastWatering = lastMeasurement?.let {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(it.timeStamp.toLong() * 1000))
    } ?: "Nigdy"

    Card(
        modifier = modifier.fillMaxWidth()
            .clickable { onClick() }
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
                    onWaterPlant { success, message ->
                        isWatering = false
                        waterMsg = if (success) "Podlano!" else message
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

