package com.example.leafme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.leafme.ui.theme.LeafMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeafMeTheme {
                val repository = (application as LeafMeApplication).repository
                // Na razie używamy userId = 1 jako placeholder.
                // W przyszłości ten ID będzie pochodził z danych zalogowanego użytkownika.
                val userId = 1
                LeafMeApp(repository = repository, userId = userId)
            }
        }
    }
}

