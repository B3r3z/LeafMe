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
                // Przekazujemy repozytorium do AuthManager, aby mógł zapisywać użytkownika w lokalnej bazie danych
                val authManager = remember { AuthManager(context, repository) }
                val coroutineScope = rememberCoroutineScope()

                // Inicjalizacja tokenu przy starcie
                authManager.initializeToken()

                // Pobieranie aktualnego ID użytkownika
                val userId = authManager.getUserId()

                // Jeśli użytkownik jest zalogowany, ale nie mamy jego ID, pobieramy je
                if (authManager.isLoggedIn() && userId == 0) {
                    coroutineScope.launch {
                        authManager.refreshUserInfo()
                    }
                } else if (authManager.isLoggedIn() && userId > 0) {
                    // Jeśli użytkownik jest zalogowany, upewnijmy się, że mamy go w lokalnej bazie danych
                    coroutineScope.launch {
                        authManager.refreshUserInfo()
                    }
                }

                val startDestination = if (authManager.isLoggedIn()) {
                    LeafMeDestinations.PlantList.name
                } else {
                    LeafMeDestinations.LoginRegister.name
                }
                LeafMeApp(repository = repository, userId = userId, startDestination = startDestination, authManager = authManager)
            }
        }
    }
}

