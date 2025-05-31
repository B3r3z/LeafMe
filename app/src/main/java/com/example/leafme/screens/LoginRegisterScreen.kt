package com.example.leafme.screens


import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.leafme.auth.AuthManager
import android.util.Log

@Composable
fun LoginRegisterScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit,
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    fun clearFields() {
        email = ""
        password = ""
        message = null
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

        message?.let {
            Text(it, color = if (it.startsWith("Błąd")) MaterialTheme.colorScheme.error else LocalContentColor.current)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                isLoading = true
                message = null
                coroutineScope.launch {
                    if (isLoginMode) {
                        val (success, msg, loggedInUserId) = authManager.login(email, password)
                        isLoading = false
                        if (success && loggedInUserId > 0) {
                            Log.d("LoginRegisterScreen", "Logowanie udane, userId: $loggedInUserId")
                            onLoginSuccess()
                        } else {
                            message = msg
                            Log.e("LoginRegisterScreen", "Błąd logowania: $msg")
                        }
                    } else {
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

