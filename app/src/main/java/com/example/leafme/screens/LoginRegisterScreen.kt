package com.example.leafme.screens


import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.leafme.auth.AuthManager

@Composable
fun LoginRegisterScreen(
    navController: NavController,
    onLoginSuccess: (userId: Int) -> Unit,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    fun clearFields() {
        email = ""
        password = ""
        message = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Logowanie" else "Rejestracja",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (message.isNotEmpty()) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                isLoading = true
                message = ""
                coroutineScope.launch {
                    if (isLoginMode) {
                        // Logowanie
                        val (success, msg) = authManager.login(email, password)
                        isLoading = false
                        if (success) {
                            // Pobierz faktyczne ID użytkownika
                            val userId = authManager.getUserId()
                            onLoginSuccess(userId)
                        } else {
                            message = msg
                        }
                    } else {
                        // Rejestracja
                        val (success, msg) = authManager.register(email, password)
                        isLoading = false
                        message = msg
                        if (success) {
                            isLoginMode = true
                            clearFields()
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoginMode) "Zaloguj się" else "Zarejestruj się")
        }

        TextButton(
            onClick = {
                isLoginMode = !isLoginMode
                clearFields()
            }
        ) {
            Text(if (isLoginMode) "Nie masz konta? Zarejestruj się" else "Masz już konto? Zaloguj się")
        }
    }
}

