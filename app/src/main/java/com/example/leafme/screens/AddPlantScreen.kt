package com.example.leafme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.leafme.R
import com.example.leafme.data.AppRepository
import com.example.leafme.domain.AddPlantUseCase

@Composable
fun AddPlantScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    modifier: Modifier = Modifier
) {
    var plantName by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val viewModel = remember { AddPlantViewModel(AddPlantUseCase(repository)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.add_plant_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = plantName,
            onValueChange = { newValue ->
                plantName = newValue
                isNameError = false // Resetuj błąd przy każdej zmianie
            },
            label = { Text(stringResource(R.string.plant_name_label)) },
            isError = isNameError,
            supportingText = {
                if (isNameError) {
                    Text(
                        text = stringResource(R.string.plant_name_error),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            //TODO: POLE Z plantD, ID TRZEBA ZAPISAC DO BAZY, NIE MOZE BYC RANDOMOWE,
            // bo potem nie bedzie mozna PODPIAC POD SERWER
            // OGARNAC, ZEBY LADNIE SIE ZAPISYWALO I WYSWIETLALO
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        Button(onClick = {
            if (plantName.isNotBlank()) {
                viewModel.addPlant(plantName, userId) {
                    navController.popBackStack()
                }
            } else {
                isNameError = true
            }
        }) {
            Text("Save Plant (Placeholder)")
        }
    }
}