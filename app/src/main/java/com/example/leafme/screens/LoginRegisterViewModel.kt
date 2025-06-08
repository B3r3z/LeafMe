package com.example.leafme.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafme.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel dla ekranu logowania i rejestracji
 */
class LoginRegisterViewModel(
    private val authManager: AuthManager
) : ViewModel() {

    // Stan UI dla ekranu logowania/rejestracji
    private val _uiState = MutableStateFlow(LoginRegisterUiState())
    val uiState: StateFlow<LoginRegisterUiState> = _uiState.asStateFlow()

    /**
     * Przełącza tryb między logowaniem a rejestracją
     */
    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            email = "",
            password = "",
            message = null
        )
    }

    /**
     * Aktualizuje wprowadzony email
     */
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    /**
     * Aktualizuje wprowadzone hasło
     */
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    /**
     * Wykonuje logowanie lub rejestrację w zależności od aktualnego trybu
     */
    fun submit() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                message = "Wprowadź email i hasło"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, message = null)

        viewModelScope.launch {
            if (_uiState.value.isLoginMode) {
                val (success, msg, loggedInUserId) = authManager.login(email, password)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = if (success) null else msg
                )

                if (success && loggedInUserId > 0) {
                    Log.d("LoginRegisterViewModel", "Logowanie udane, userId: $loggedInUserId")
                    // Logika nawigacji jest obsługiwana przez komponent UI i AuthManager (reactive state)
                } else {
                    Log.e("LoginRegisterViewModel", "Błąd logowania: $msg")
                }
            } else {
                val (success, msg) = authManager.register(email, password)

                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = msg,
                        isLoginMode = true,
                        email = "",
                        password = ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = msg
                    )
                }
            }
        }
    }
}

/**
 * Klasa reprezentująca stan UI dla ekranu logowania/rejestracji
 */
data class LoginRegisterUiState(
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val message: String? = null
)
