package com.example.leafme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.leafme.database.AppRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import java.text.SimpleDateFormat
import java.util.*


private val timeRanges = listOf(15, 30, 60, 120) // minuty
@Composable
fun PlantDetailsScreen(
    plantId: Int,
    repository: AppRepository,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: PlantDetailsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PlantDetailsViewModel(repository) as T
        }
    })
) {
    var selectedRange by remember { mutableStateOf(30) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    LaunchedEffect(plantId, selectedRange) {
        viewModel.loadPlantDetails(plantId, selectedRange)
    }

    val plant = uiState.plant
    val lastMeasurement = uiState.lastMeasurement

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(64.dp))
            CircularProgressIndicator()
        } else if (plant == null) {
            Text("Nie znaleziono rośliny.", style = MaterialTheme.typography.titleMedium)
        } else {
            Text(
                plant.name,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Karta z ostatnim podlewaniem
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪴", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ostatnie podlewanie",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.lastWateringTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Dwa kwadraty z danymi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wilgotność
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("💧", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lastMeasurement != null) {
                            Text(
                                "%.1f%%".format(lastMeasurement.moisture),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text("Wilgotność", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("--", style = MaterialTheme.typography.headlineMedium)
                            Text("Wilgotność", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                // Temperatura
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌡️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lastMeasurement != null) {
                            Text(
                                "%.1f°C".format(lastMeasurement.temperature),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text("Temperatura", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            Text("--", style = MaterialTheme.typography.headlineMedium)
                            Text("Temperatura", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            // Chipy wyboru zakresu czasu (wstaw przed wykresami)
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp)
            ) {
                timeRanges.forEach { minutes ->
                    AssistChip(
                        onClick = { selectedRange = minutes },
                        label = { Text("$minutes min") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedRange == minutes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (selectedRange == minutes) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wykres wilgotności
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Wilgotność (ostatnie 30 minut):", style = MaterialTheme.typography.titleMedium)
                    LineChart(
                        data = uiState.chartMeasurements.map { it.timeStamp.toLong() to it.moisture },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        yLabel = "[%]",
                    )
                }
            }

            // Wykres temperatury
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Temperatura (ostatnie 30 minut):", style = MaterialTheme.typography.titleMedium)
                    LineChart(
                        data = uiState.chartMeasurements.map { it.timeStamp.toLong() to it.temperature },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        yLabel = "[°C]",
                    )
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
    yLabel: String = "Wilgotność [%]"
) {
    val minY = data.minOfOrNull { it.second } ?: 0f
    val maxY = data.maxOfOrNull { it.second } ?: 100f
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(modifier = modifier) {
        // Oś Y - podpis
        Text(yLabel, style = MaterialTheme.typography.bodySmall)
        Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padding = 40f
                val width = size.width - padding
                val height = size.height - padding

                // Oś Y
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, 0f),
                    end = Offset(padding, height),
                    strokeWidth = 2f
                )
                // Oś X
                drawLine(
                    color = Color.Gray,
                    start = Offset(padding, height),
                    end = Offset(width, height),
                    strokeWidth = 2f
                )

                // Punkty i linia
                val points = data.mapIndexed { idx, (ts, value) ->
                    val x = padding + idx * (width - padding) / (data.size - 1)
                    val y = height - ((value - minY) / (maxY - minY + 0.01f)) * (height - 20f)
                    Offset(x, y)
                }
                // Linia wykresu
                val path = Path().apply {
                    if (points.isNotEmpty()) moveTo(points[0].x, points[0].y)
                    for (pt in points.drop(1)) lineTo(pt.x, pt.y)
                }
                drawPath(path, color = Color(0xFF4CAF50), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                // Punkty
                points.forEach {
                    drawCircle(Color(0xFF388E3C), center = it, radius = 6f)
                }

                // Podpisy osi X (czas)
                val labelEvery = (data.size / 6).coerceAtLeast(1) // pokazuj maks. 6 podpisów
                data.forEachIndexed { idx, (ts, _) ->
                    if (idx % labelEvery == 0) {
                        val x = padding + idx * (width - padding) / (data.size - 1)
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(
                                timeFormat.format(Date(ts * 1000)),
                                x - 30,
                                height + 30,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.DKGRAY
                                    textSize = 28f
                                }
                            )
                        }
                    }
                }

                // Podpisy osi Y (min, max)
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "%.0f".format(maxY),
                        0f,
                        30f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 28f
                        }
                    )
                    drawText(
                        "%.0f".format(minY),
                        0f,
                        height,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 28f
                        }
                    )
                }
            }
        }
        // Oś X - podpis
        Text("Czas (ostatnie 30 minut, co 5 min)", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}
