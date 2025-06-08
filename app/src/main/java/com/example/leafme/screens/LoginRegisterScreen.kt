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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginRegisterScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit,
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    viewModel: LoginRegisterViewModel = viewModel { LoginRegisterViewModel(authManager) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (uiState.isLoginMode) "Logowanie" else "Rejestracja",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { viewModel.updateEmail(it) },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        uiState.message?.let {
            Text(it, color = if (it.startsWith("Błąd")) MaterialTheme.colorScheme.error else LocalContentColor.current)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = { viewModel.submit() },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoginMode) "Zaloguj się" else "Zarejestruj się")
        }

        TextButton(
            onClick = { viewModel.toggleMode() }
        ) {
            Text(if (uiState.isLoginMode) "Nie masz konta? Zarejestruj się" else "Masz już konto? Zaloguj się")
        }
    }
}

