package com.example.leafme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.leafme.ui.theme.LeafMeTheme
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LeafMeApplication
        val repository = app.repository
        val authManager = app.authManager

        setContent {
            LeafMeTheme {
                val isLoggedIn by authManager.isLoggedInState.collectAsState()
                val userId by authManager.currentUserIdState.collectAsState()

                Log.d("MainActivity", "Stan isLoggedIn: $isLoggedIn, Stan userId: $userId")

                val startDestination = if (isLoggedIn && userId > 0) {
                    LeafMeDestinations.PlantList.name
                } else {
                    LeafMeDestinations.LoginRegister.name
                }
                Log.d("MainActivity", "StartDestination: $startDestination")

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
