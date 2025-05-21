package com.example.leafme.database

import androidx.room.Dao
import androidx.room.Query
import com.example.leafme.data.User

@Dao
interface UserDao {
    suspend fun insert(use: User)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Int): User?
}