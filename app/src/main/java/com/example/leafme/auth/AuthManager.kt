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

    private val _currentUserId = MutableStateFlow(getUserIdFromPrefs())
    val currentUserIdState: StateFlow<Int> = _currentUserId.asStateFlow()

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val TAG = "AuthManager"
    }

    private fun isTokenPresentInPrefs(): Boolean {
        return sharedPreferences.getString(KEY_TOKEN, "")?.isNotEmpty() ?: false
    }

    private fun getUserIdFromPrefs(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, 0)
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
     * @return Triple (sukces, wiadomość, userId)
     */
    suspend fun login(email: String, password: String): Triple<Boolean, String, Int> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email = email, password = password)
            val response = RetrofitClient.authService.login(request)

            if (response.isSuccessful) {
                val token = response.body()?.accessToken ?: return@withContext Triple(false, "Brak tokenu w odpowiedzi", 0)

                saveToken(token)
                saveUserEmail(email)
                RetrofitClient.setAuthToken(token)

                val fetchedUserId = fetchAndSaveUserIdInternal()

                if (fetchedUserId > 0) {
                    if (repository != null) {
                        try {
                            val existingUser = repository.getUserById(fetchedUserId)
                            if (existingUser == null) {
                                Log.d(TAG, "Dodaję użytkownika do lokalnej bazy danych: id=$fetchedUserId, email=$email")
                                val user = User(userId = fetchedUserId, email = email, passwordHash = "")
                                repository.insertUser(user)
                                Log.d(TAG, "Użytkownik dodany do lokalnej bazy danych")
                            } else {
                                Log.d(TAG, "Użytkownik już istnieje w lokalnej bazie danych")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Błąd podczas dodawania użytkownika do lokalnej bazy danych: ${e.message}", e)
                        }
                    }
                    _isLoggedIn.value = true
                    return@withContext Triple(true, "Logowanie udane!", fetchedUserId)
                } else {
                    logout() // Pełne wylogowanie, jeśli nie udało się pobrać ID
                    return@withContext Triple(false, "Nie udało się pobrać ID użytkownika.", 0)
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Nieznany błąd"
                logout() // Pełne wylogowanie przy błędzie API logowania
                return@withContext Triple(false, "Błąd logowania: $errorMsg", 0)
            }
        } catch (e: Exception) {
            logout() // Pełne wylogowanie przy wyjątku sieciowym itp.
            return@withContext Triple(false, "Błąd sieci: ${e.message}", 0)
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
        // Usuń email użytkownika
        saveUserEmail("")
        // Zaktualizuj ID użytkownika
        saveUserIdInternal(0) // Użyj metody wewnętrznej do aktualizacji SharedPreferences i StateFlow
        // Zaktualizuj stan zalogowania
        _isLoggedIn.value = false
        Log.d(TAG, "Użytkownik wylogowany. Stany isLoggedIn i currentUserId zresetowane.")
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
     * oraz aktualizuje stan _isLoggedIn i _currentUserId.
     */
    fun initializeToken() {
        val token = getToken()
        if (token.isNotEmpty()) {
            RetrofitClient.setAuthToken(token)
            // _isLoggedIn i _currentUserId są już inicjalizowane z SharedPreferences
            Log.d(TAG, "Token zainicjalizowany. isLoggedIn: ${_isLoggedIn.value}, currentUserId: ${_currentUserId.value}")
        } else {
            _isLoggedIn.value = false
            _currentUserId.value = 0
            Log.d(TAG, "Brak tokenu przy inicjalizacji. Stany zresetowane.")
        }
    }

    /**
     * Pobiera ID użytkownika z serwera i zapisuje je lokalnie
     * @return ID użytkownika lub 0 jeśli nie udało się pobrać
     */
    private suspend fun fetchAndSaveUserIdInternal(): Int = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.userService.getUserInfo()
            if (response.isSuccessful) {
                val userId = response.body()?.id ?: 0
                if (userId > 0) {
                    saveUserIdInternal(userId)
                    return@withContext userId
                } else {
                    Log.e(TAG, "Serwer zwrócił nieprawidłowe ID użytkownika (0).")
                    return@withContext 0
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Błąd API podczas pobierania ID użytkownika: $errorBody")
                return@withContext 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wyjątek podczas pobierania ID użytkownika: ${e.message}", e)
            return@withContext 0
        }
    }

    /**
     * Zapisuje ID użytkownika w SharedPreferences i aktualizuje StateFlow
     */
    private fun saveUserIdInternal(userId: Int) {
        sharedPreferences.edit().putInt(KEY_USER_ID, userId).apply()
        _currentUserId.value = userId
        Log.d(TAG, "Zapisano userId: $userId do SharedPreferences i _currentUserId.")
    }

    /**
     * Pobiera zapisane ID użytkownika
     * @return ID użytkownika lub 0 jeśli nie jest zalogowany
     */
    fun getUserId(): Int {
        return _currentUserId.value // Lub sharedPreferences.getInt(KEY_USER_ID, 0)
    }

    /**
     * Pobiera dane użytkownika z serwera
     * Używaj tej metody, gdy potrzebujesz ponownie pobrać dane użytkownika
     */
    suspend fun refreshUserInfo(): Boolean = withContext(Dispatchers.IO) {
        if (!isTokenPresentInPrefs()) {
            logout()
            return@withContext false
        }
        val fetchedUserId = fetchAndSaveUserIdInternal()
        if (fetchedUserId > 0) {
            _isLoggedIn.value = true
            if (repository != null) {
                val email = getUserEmail()
                if (email.isNotEmpty()) {
                    try {
                        val existingUser = repository.getUserById(fetchedUserId)
                        if (existingUser == null) {
                            val user = User(userId = fetchedUserId, email = email, passwordHash = "")
                            repository.insertUser(user)
                            Log.d(TAG, "Użytkownik dodany do lokalnej bazy danych podczas odświeżania")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Błąd podczas aktualizacji użytkownika w lokalnej bazie danych: ${e.message}", e)
                    }
                }
            }
            return@withContext true
        } else {
            logout()
            return@withContext false
        }
    }
}
