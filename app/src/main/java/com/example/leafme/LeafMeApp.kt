package com.example.leafme

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.leafme.data.AppRepository
import com.example.leafme.screens.AddPlantScreen
import com.example.leafme.screens.LoginRegisterScreen
import com.example.leafme.screens.PlantListScreen

enum class LeafMeDestinations(@StringRes val title: Int) {
    PlantList(title = R.string.plant_list_screen_title),
    AddPlant(title = R.string.add_plant_screen_title),
    // TODO: Dodaj inne ekrany, np. EditPlant, PlantDetails
    LoginRegister(title = R.string.login_register_screen_title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafMeApp(
    repository: AppRepository,
    navController: NavHostController = rememberNavController(),
    userId: Int, // Przekazujemy userId
    startDestination: String
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreenName = backStackEntry?.destination?.route ?: LeafMeDestinations.PlantList.name
    val currentScreen = LeafMeDestinations.entries.find { it.name == currentScreenName } ?: LeafMeDestinations.PlantList

    Scaffold(
        topBar = {
            LeafMeTopAppBar(
                currentScreenTitle = stringResource(id = currentScreen.title),
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        },
        floatingActionButton = {
            if (currentScreen == LeafMeDestinations.PlantList) { // Pokaż FAB tylko na liście roślin
                FloatingActionButton(onClick = {
                    navController.navigate(LeafMeDestinations.AddPlant.name)
                }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_plant_fab_description))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LeafMeDestinations.PlantList.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(route = LeafMeDestinations.PlantList.name) {
                LoginRegisterScreen(
                    navController = navController,
                    onLoginSuccess = { userId ->
                        navController.navigate(LeafMeDestinations.PlantList.name) {
                            popUpTo(LeafMeDestinations.LoginRegister.name) { inclusive = true }
                        }
                        // Przekaż userId do dalszych ekranów jeśli trzeba
                    }
                )
            }
            composable(route = LeafMeDestinations.AddPlant.name) {
                AddPlantScreen(
                    navController = navController,
                    repository = repository,
                    userId = userId
                    // modifier = Modifier.padding(innerPadding)
                )
            }
            // TODO: Dodaj composable dla innych ekranów, np. edycji rośliny
            // composable(route = LeafMeDestinations.EditPlant.name + "/{plantId}") { ... }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafMeTopAppBar(
    currentScreenTitle: String,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(text = currentScreenTitle) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_description)
                    )
                }
            }
        }
    )
}