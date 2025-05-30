package com.example.leafme.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.leafme.data.User
import com.example.leafme.data.Plant
import com.example.leafme.data.Measurement

@Database(entities = [User::class, Plant::class, Measurement::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun plantDao(): PlantDao
    abstract fun measurementDao(): MeasurementDao

    companion object {
        const val DATABASE_NAME = "leafme_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    //.fallbackToDestructiveMigration() // Odkomentuj, jeśli chcesz, aby baza danych była czyszczona przy zmianie schematu
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}