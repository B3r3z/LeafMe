package com.example.leafme.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.leafme.retrofit.LoginRequest
import com.example.leafme.retrofit.RegisterRequest
import com.example.leafme.retrofit.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Klasa do zarządzania autentykacją użytkownika
 */
class AuthManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val TAG = "AuthManager"
    }

    /**
     * Rejestruje nowego użytkownika
     * @return Para (sukces, wiadomość)
     */
    suspend fun register(email: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(email = email, password = password)
            val response = RetrofitClient.authService.register(request)

            if (response.isSuccessful) {
                return@withContext Pair(true, "Rejestracja udana!")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Nieznany błąd"
                Log.e(TAG, "Błąd rejestracji: $errorMsg")
                return@withContext Pair(false, "Błąd rejestracji: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas rejestracji", e)
            return@withContext Pair(false, "Błąd sieci: ${e.message}")
        }
    }

    /**
     * Loguje użytkownika
     * @return Para (sukces, wiadomość)
     */
    suspend fun login(email: String, password: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email = email, password = password)
            val response = RetrofitClient.authService.login(request)

            if (response.isSuccessful) {
                val token = response.body()?.accessToken ?: return@withContext Pair(false, "Brak tokenu w odpowiedzi")

                // Zapisz token w SharedPreferences
                saveToken(token)

                // Ustaw token w kliencie Retrofit
                RetrofitClient.setAuthToken(token)

                // Pobierz i zapisz ID użytkownika
                fetchAndSaveUserId()

                return@withContext Pair(true, "Logowanie udane!")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Nieznany błąd"
                Log.e(TAG, "Błąd logowania: $errorMsg")
                return@withContext Pair(false, "Błąd logowania: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas logowania", e)
            return@withContext Pair(false, "Błąd sieci: ${e.message}")
        }
    }

    /**
     * Wylogowuje użytkownika
     */
    fun logout() {
        // Usuń token z SharedPreferences
        saveToken("")
        // Usuń ID użytkownika
        saveUserId(0)
        // Usuń token z klienta Retrofit
        RetrofitClient.setAuthToken("")
    }

    /**
     * Zapisuje token JWT w SharedPreferences
     */
    private fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Pobiera zapisany token JWT
     */
    fun getToken(): String {
        return sharedPreferences.getString(KEY_TOKEN, "") ?: ""
    }

    /**
     * Sprawdza, czy użytkownik jest zalogowany
     */
    fun isLoggedIn(): Boolean {
        val token = getToken()
        return token.isNotEmpty()
    }

    /**
     * Inicjalizuje token w kliencie Retrofit przy starcie aplikacji
     */
    fun initializeToken() {
        val token = getToken()
        if (token.isNotEmpty()) {
            RetrofitClient.setAuthToken(token)
        }
    }

    /**
     * Pobiera ID użytkownika z serwera i zapisuje je lokalnie
     */
    private suspend fun fetchAndSaveUserId() = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.userService.getUserInfo()
            if (response.isSuccessful) {
                val userId = response.body()?.id ?: 0
                saveUserId(userId)
                Log.d(TAG, "Pobrano ID użytkownika: $userId")
            } else {
                Log.e(TAG, "Błąd podczas pobierania ID użytkownika: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas pobierania ID użytkownika", e)
        }
    }

    /**
     * Zapisuje ID użytkownika w SharedPreferences
     */
    private fun saveUserId(userId: Int) {
        sharedPreferences.edit().putInt(KEY_USER_ID, userId).apply()
    }

    /**
     * Pobiera zapisane ID użytkownika
     * @return ID użytkownika lub 0 jeśli nie jest zalogowany
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, 0)
    }

    /**
     * Pobiera dane użytkownika z serwera
     * Używaj tej metody, gdy potrzebujesz ponownie pobrać dane użytkownika
     */
    suspend fun refreshUserInfo(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.userService.getUserInfo()
            if (response.isSuccessful) {
                val userId = response.body()?.id ?: 0
                saveUserId(userId)
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas odświeżania informacji o użytkowniku", e)
            return@withContext false
        }
    }
}
