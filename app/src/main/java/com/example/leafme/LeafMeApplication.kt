package com.example.leafme

import android.app.Application
import com.example.leafme.database.AppRepository
import com.example.leafme.database.AppDatabase
import com.example.leafme.auth.AuthManager // Dodaj import

class LeafMeApplication : Application() {

    // Użycie 'lazy' zapewnia, że baza danych i repozytorium są tworzone tylko wtedy,
    // gdy są po raz pierwszy potrzebne, i tylko raz podczas życia aplikacji.
    val database by lazy { AppDatabase.getDatabase(this) }

    // Tworzenie AppRepository bez AuthManager na początku
    val repository by lazy {
        AppRepository(
            userDao = database.userDao(),
            plantDao = database.plantDao(),
            measurementDao = database.measurementDao()
            // AuthManager zostanie ustawiony później
        )
    }

    // Tworzenie AuthManager z referencją do repository
    val authManager by lazy { AuthManager(applicationContext, repository) }

    override fun onCreate() {
        super.onCreate()
        // Po utworzeniu obu instancji, wstrzyknij authManager do repository
        repository.setAuthManager(authManager)
        // Inicjalizacja tokenu (i stanu isLoggedIn) przy starcie aplikacji
        authManager.initializeToken()
    }
}

