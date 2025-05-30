package com.example.leafme.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.leafme.retrofit.LoginRequest
import com.example.leafme.retrofit.RegisterRequest
import com.example.leafme.retrofit.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import com.example.leafme.data.User
import com.example.leafme.database.AppRepository

/**
 * Klasa do zarządzania autentykacją użytkownika
 */
class AuthManager(private val context: Context, private val repository: AppRepository? = null) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(isTokenPresentInPrefs())
    val isLoggedInState: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val TAG = "AuthManager"
    }

    private fun isTokenPresentInPrefs(): Boolean {
        return sharedPreferences.getString(KEY_TOKEN, "")?.isNotEmpty() ?: false
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

                saveToken(token)
                saveUserEmail(email)
                RetrofitClient.setAuthToken(token)
                _isLoggedIn.value = true // Aktualizuj stan zalogowania

                val userId = fetchAndSaveUserId()

                if (repository != null && userId > 0) {
                    try {
                        val existingUser = repository.getUserById(userId)
                        if (existingUser == null) {
                            Log.d(TAG, "Dodaję użytkownika do lokalnej bazy danych: id=$userId, email=$email")
                            val user = User(userId = userId, email = email, passwordHash = "")
                            repository.insertUser(user)
                            Log.d(TAG, "Użytkownik dodany do lokalnej bazy danych")
                        } else {
                            Log.d(TAG, "Użytkownik już istnieje w lokalnej bazie danych")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd podczas dodawania użytkownika do lokalnej bazy danych: ${e.message}", e)
                    }
                }
                return@withContext Pair(true, "Logowanie udane!")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Nieznany błąd"
                Log.e(TAG, "Błąd logowania: $errorMsg")
                _isLoggedIn.value = false // Upewnij się, że stan jest false przy błędzie
                return@withContext Pair(false, "Błąd logowania: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas logowania", e)
            _isLoggedIn.value = false // Upewnij się, że stan jest false przy błędzie
            return@withContext Pair(false, "Błąd sieci: ${e.message}")
        }
    }

    /**
     * Wylogowuje użytkownika.
     * Ta metoda nie jest już funkcją suspend.
     * Czyszczenie danych lokalnych (np. roślin) powinno być obsługiwane osobno,
     * np. przed wywołaniem tej metody przy jawnym wylogowaniu.
     */
    fun logout() {
        // Usuń token z klienta Retrofit
        RetrofitClient.setAuthToken("")
        // Usuń token z SharedPreferences
        saveToken("")
        // Usuń ID użytkownika
        saveUserId(0)
        // Usuń email użytkownika
        saveUserEmail("")
        // Zaktualizuj stan zalogowania
        _isLoggedIn.value = false
        Log.d(TAG, "Użytkownik wylogowany. Stan isLoggedIn ustawiony na false.")
    }

    /**
     * Zapisuje token JWT w SharedPreferences
     */
    private fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
    }

    /**
     * Zapisuje email użytkownika w SharedPreferences
     */
    private fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    /**
     * Pobiera zapisany email użytkownika
     */
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }

    /**
     * Pobiera zapisany token JWT
     */
    fun getToken(): String {
        return sharedPreferences.getString(KEY_TOKEN, "") ?: ""
    }

    /**
     * Sprawdza, czy użytkownik jest zalogowany (na podstawie StateFlow).
     */
    fun isLoggedIn(): Boolean {
        return _isLoggedIn.value
    }

    /**
     * Inicjalizuje token w kliencie Retrofit przy starcie aplikacji
     * oraz aktualizuje stan _isLoggedIn.
     */
    fun initializeToken() {
        val token = getToken()
        if (token.isNotEmpty()) {
            RetrofitClient.setAuthToken(token)
            _isLoggedIn.value = true
            Log.d(TAG, "Token zainicjalizowany. Stan isLoggedIn ustawiony na true.")
        } else {
            _isLoggedIn.value = false
            Log.d(TAG, "Brak tokenu przy inicjalizacji. Stan isLoggedIn ustawiony na false.")
        }
    }

    /**
     * Pobiera ID użytkownika z serwera i zapisuje je lokalnie
     * @return ID użytkownika lub 0 jeśli nie udało się pobrać
     */
    private suspend fun fetchAndSaveUserId(): Int = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.userService.getUserInfo()
            if (response.isSuccessful) {
                val userId = response.body()?.id ?: 0
                saveUserId(userId)
                Log.d(TAG, "Pobrano ID użytkownika: $userId")
                return@withContext userId
            } else {
                Log.e(TAG, "Błąd podczas pobierania ID użytkownika: ${response.errorBody()?.string()}")
                // Jeśli pobranie ID użytkownika nie powiedzie się (np. z powodu wygaśnięcia tokenu zaraz po logowaniu),
                // powinniśmy to potraktować jako nieudane logowanie.
                logout() // Wywołaj logout, aby wyczyścić stan i token
                return@withContext 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas pobierania ID użytkownika", e)
            logout() // Wywołaj logout, aby wyczyścić stan i token
            return@withContext 0
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

                if (repository != null && userId > 0) {
                    val email = getUserEmail()
                    if (email.isNotEmpty()) {
                        try {
                            val existingUser = repository.getUserById(userId)
                            if (existingUser == null) {
                                val user = User(userId = userId, email = email, passwordHash = "")
                                repository.insertUser(user)
                                Log.d(TAG, "Użytkownik dodany do lokalnej bazy danych podczas odświeżania")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd podczas aktualizacji użytkownika w lokalnej bazie danych: ${e.message}", e)
                        }
                    }
                }
                _isLoggedIn.value = true // Upewnij się, że stan jest aktualny
                return@withContext true
            } else {
                // Jeśli odświeżenie informacji o użytkowniku nie powiedzie się z powodu tokenu
                if (response.errorBody()?.string()?.contains("Token has expired") == true) {
                    logout()
                } else {
                     _isLoggedIn.value = false // Jeśli inny błąd, ale nie możemy potwierdzić użytkownika
                }
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas odświeżania informacji o użytkowniku", e)
            // Rozważ wylogowanie, jeśli wyjątek oznacza problem z autentykacją
            // logout()
             _isLoggedIn.value = false // Jeśli wyjątek, nie możemy potwierdzić użytkownika
            return@withContext false
        }
    }
}
