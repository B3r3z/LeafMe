package com.example.leafme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.example.leafme.ui.theme.LeafMeTheme

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
                val currentUserId = authManager.getUserId()

                val startDestination = if (isLoggedIn) {
                    LeafMeDestinations.PlantList.name
                } else {
                    LeafMeDestinations.LoginRegister.name
                }

                LeafMeApp(
                    repository = repository,
                    userId = currentUserId,
                    startDestination = startDestination,
                    authManager = authManager
                )
            }
        }
    }
}
