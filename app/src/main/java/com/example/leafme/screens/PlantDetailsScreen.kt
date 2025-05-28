
package com.example.leafme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.leafme.database.AppRepository
import com.example.leafme.data.Measurement
import com.example.leafme.data.Plant
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
// Prosty wykres liniowy (tylko poglądowy, nie produkcyjny!)
// Wymaga: implementation "androidx.compose.foundation:foundation:1.4.3"
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.math.abs


fun sampleEvery5Minutes(measurements: List<Measurement>): List<Measurement> {
    // Sortujemy po czasie
    return measurements
        .sortedBy { it.timeStamp }
        .groupBy { it.timeStamp / 300 } // 300 sekund = 5 minut
        .map { (_, group) ->
            // Możesz wybrać .last(), .first() lub średnią z grupy
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



@Composable
fun PlantDetailsScreen(
    plantId: Int,
    repository: AppRepository,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var plant by remember { mutableStateOf<Plant?>(null) }
    var measurements by remember { mutableStateOf<List<Measurement>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
   // val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(plantId) {
        isLoading = true
        plant = repository.getPlantById(plantId)
        measurements = repository.getMeasurementsForPlant(plantId)
        isLoading = false
    }

    val lastMeasurement = measurements.firstOrNull()
    val lastWatering = lastMeasurement?.let {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            .format(Date(it.timeStamp.toLong() * 1000))
    } ?: "Nigdy"

    // Filtruj pomiary z ostatniej godziny
    val now = System.currentTimeMillis() / 1000
    val thirtyMinutesAgo = now - 1800 // 30 minut
    val last30MinMeasurements = measurements.filter { it.timeStamp >= thirtyMinutesAgo }
    val chartMeasurementsRaw = if (last30MinMeasurements.isNotEmpty()) last30MinMeasurements else measurements
    val chartMeasurements = sampleEvery5Minutes(chartMeasurementsRaw)
    val filtered = measurements
        .filter { it.timeStamp >= thirtyMinutesAgo }
        .groupBy { (it.timeStamp - thirtyMinutesAgo) / (5 * 60) } // grupuj co 5 minut
        .map { (_, group) -> group.last() } // wybierz ostatni pomiar z każdej grupy


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (plant == null) {
            Text("Nie znaleziono rośliny.")
        } else {
            Text(plant!!.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ostatnie podlewanie: $lastWatering")
            Spacer(modifier = Modifier.height(8.dp))
            if (lastMeasurement != null) {
                Text("Wilgotność gleby: %.1f%%".format(lastMeasurement.moisture))
                Text("Temperatura: %.1f°C".format(lastMeasurement.temperature))
            } else {
                Text("Brak danych o wilgotności i temperaturze")
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                if (last30MinMeasurements.isNotEmpty()) "Wilgotność (ostatnie 30 minut):"
                else "Wilgotność (wszystkie pomiary):"
            )
            LineChart(
                data = chartMeasurements.map { it.timeStamp.toLong() to it.moisture },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                //color = MaterialTheme.colorScheme.primary,
                yLabel = "[%]",
                //xLabel = "Czas"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                if (last30MinMeasurements.isNotEmpty()) "Temperatura (ostatnie 30 minut):"
                else "Temperatura (wszystkie pomiary):"
            )
            LineChart(
                data = chartMeasurements.map { it.timeStamp.toLong() to it.moisture },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                //color = MaterialTheme.colorScheme.tertiary,
                yLabel = "[°C]",
                //xLabel = "Czas"
            )

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
