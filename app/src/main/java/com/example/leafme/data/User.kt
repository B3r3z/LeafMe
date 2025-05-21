package com.example.leafme.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Int = 0,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String
)
