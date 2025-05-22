package com.example.leafme.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.leafme.R
import com.example.leafme.data.AppRepository

@Composable
fun AddPlantScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.add_plant_screen_title))
        // TODO: Zaimplementuj formularz dodawania rośliny
        Button(onClick = {
            // Logika zapisu (np. repository.addPlant(...))
            navController.popBackStack() // Wróć do poprzedniego ekranu
        }) {
            Text("Save Plant (Placeholder)")
        }
    }
}