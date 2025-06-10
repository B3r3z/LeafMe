package com.example.leafme.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: Int,
    val email: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String
)
