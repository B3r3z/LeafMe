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
import com.example.leafme.LeafMeDestinations

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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

                isLoading = true
                errorMessage = null
                Log.d("AddPlantScreen", "Dodawanie rośliny: $plantName, userId: $userId")

                val plantId = plantIdText.toIntOrNull()
                if (plantIdText.isNotBlank() && plantId == null) {
                    isPlantIdError = true
                    isLoading = false
                    errorMessage = "Nieprawidłowe ID rośliny."
                    return@Button
                }
                coroutineScope.launch {
                    try {
                        viewModel.addPlant(plantName, userId, plantId) {
                            coroutineScope.launch {
                                try {
                                    Log.d("AddPlantScreen", "Rozpoczynam synchronizację roślin po dodaniu")
                                    repository.syncPlantsWithServer(userId)
                                    Log.d("AddPlantScreen", "Synchronizacja zakończona")
                                    navController.popBackStack()
                                } catch (e: TokenExpiredException) {
                                    Log.e("AddPlantScreen", "Token wygasł podczas synchronizacji po dodaniu.", e)
                                    authManager.logout() // To zainicjuje globalne przekierowanie
                                    errorMessage = "Sesja wygasła. Zaloguj się ponownie."
                                } catch (e: Exception) {
                                    Log.e("AddPlantScreen", "Błąd podczas synchronizacji po dodaniu: ${e.message}", e)
                                    errorMessage = "Błąd synchronizacji: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    } catch (e: TokenExpiredException) {
                        Log.e("AddPlantScreen", "Token wygasł podczas dodawania rośliny.", e)
                        authManager.logout() // To zainicjuje globalne przekierowanie
                        errorMessage = "Sesja wygasła. Zaloguj się ponownie."
                        isLoading = false
                    } catch (e: Exception) {
                        Log.e("AddPlantScreen", "Błąd podczas dodawania rośliny: ${e.message}", e)
                        errorMessage = "Błąd dodawania rośliny: ${e.message}"
                        isLoading = false
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
