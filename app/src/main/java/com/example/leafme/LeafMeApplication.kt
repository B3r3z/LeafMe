package com.example.leafme

import android.app.Application
import com.example.leafme.data.AppRepository
import com.example.leafme.database.AppDatabase

class LeafMeApplication : Application() {

    // Użycie 'lazy' zapewnia, że baza danych i repozytorium są tworzone tylko wtedy,
    // gdy są po raz pierwszy potrzebne, i tylko raz podczas życia aplikacji.
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        AppRepository(
            userDao = database.userDao(),
            plantDao = database.plantDao(),
            measurementDao = database.measurementDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Możesz tutaj dodać inną logikę inicjalizacyjną, jeśli jest potrzebna.
        // Inicjalizacja repozytorium i bazy danych odbywa się teraz leniwie (lazy).
    }
}