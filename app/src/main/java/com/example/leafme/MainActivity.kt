package com.example.leafme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.example.leafme.auth.AuthManager
import com.example.leafme.ui.theme.LeafMeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LeafMeTheme {
                val repository = (application as LeafMeApplication).repository
                val context = this
                val authManager = remember { AuthManager(context, repository) }
                // Inicjalizacja tokenu przy starcie
                authManager.initializeToken()
                var userId = authManager.getUserId()

                // Użyj LaunchedEffect do wywołania suspendowanych funkcji
                androidx.compose.runtime.LaunchedEffect(authManager.isLoggedIn()) {
                    if (authManager.isLoggedIn() && userId == 0) {
                        authManager.refreshUserInfo()
                    } else if (authManager.isLoggedIn() && userId > 0) {
                        authManager.refreshUserInfo()
                    }
                }

                // Pobierz userId po ewentualnym odświeżeniu
                userId = authManager.getUserId()
                val startDestination = if (authManager.isLoggedIn()) {
                    LeafMeDestinations.PlantList.name
                } else {
                    LeafMeDestinations.LoginRegister.name
                }
                LeafMeApp(
                    repository = repository,
                    userId = userId,
                    startDestination = startDestination,
                    authManager = authManager
                )
            }
        }
    }
}
