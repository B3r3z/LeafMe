package com.example.leafme.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.leafme.data.User

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: Int): User?
}