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
import com.example.leafme.database.AppRepository
import com.example.leafme.domain.AddPlantUseCase
import kotlinx.coroutines.launch
import android.util.Log
import com.example.leafme.auth.AuthManager
import com.example.leafme.util.TokenExpiredException
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AddPlantScreen(
    navController: NavController,
    repository: AppRepository,
    userId: Int,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    var plantName by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val viewModel = remember { AddPlantViewModel(AddPlantUseCase(repository, authManager)) }
    var plantIdText by remember { mutableStateOf("") }
    var isPlantIdError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Obserwuj stan ładowania z ViewModel zamiast lokalnego stanu
    val isLoading by viewModel.isLoading.collectAsState()

    // Obserwuj stan błędu z ViewModel
    val error by viewModel.errorState.collectAsState()

    // Reaguj na zmiany stanu błędu
    LaunchedEffect(error) {
        error?.let { exception ->
            when (exception) {
                is TokenExpiredException -> {
                    Log.e("AddPlantScreen", "Token wygasł podczas dodawania rośliny.", exception)
                    authManager.logout()
                    errorMessage = "Sesja wygasła. Zaloguj się ponownie."
                }
                else -> {
                    Log.e("AddPlantScreen", "Błąd podczas dodawania rośliny: ${exception.message}", exception)
                    // Sprawdź, czy błąd dotyczy duplikatu ID
                    if (exception.message?.contains("już istnieje na serwerze") == true) {
                        isPlantIdError = true
                        errorMessage = exception.message
                    } else {
                        errorMessage = "Błąd dodawania rośliny: ${exception.message}"
                    }
                }
            }
        }
    }

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
                isNameError = false
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
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        OutlinedTextField(
            value = plantIdText,
            onValueChange = { newValue ->
                plantIdText = newValue
                isPlantIdError = false
                viewModel.clearError() // Wyczyść błąd przy zmianie tekstu
            },
            label = { Text("ID rośliny (opcjonalnie)") },
            isError = isPlantIdError,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        Button(
            onClick = {
                if (plantName.isBlank()) {
                    isNameError = true
                    return@Button
                }

                errorMessage = null
                Log.d("AddPlantScreen", "Dodawanie rośliny: $plantName, userId: $userId")

                val plantId = plantIdText.toIntOrNull()
                if (plantIdText.isNotBlank() && plantId == null) {
                    isPlantIdError = true
                    errorMessage = "Nieprawidłowe ID rośliny."
                    return@Button
                }

                viewModel.addPlant(plantName, userId, plantId) {
                    coroutineScope.launch {
                        try {
                            Log.d("AddPlantScreen", "Rozpoczynam synchronizację roślin po dodaniu")
                            repository.syncPlantsWithServer(userId)
                            Log.d("AddPlantScreen", "Synchronizacja zakończona")
                            navController.popBackStack()
                        } catch (e: TokenExpiredException) {
                            Log.e("AddPlantScreen", "Token wygasł podczas synchronizacji po dodaniu.", e)
                            authManager.logout()
                            errorMessage = "Sesja wygasła. Zaloguj się ponownie."
                        } catch (e: Exception) {
                            Log.e("AddPlantScreen", "Błąd podczas synchronizacji po dodaniu: ${e.message}", e)
                            errorMessage = "Błąd synchronizacji: ${e.message}"
                        }
                    }
                }
            },
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.save_plant_button))
        }

        if (isLoading) {
            Text(
                text = "Dodawanie rośliny...",
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
