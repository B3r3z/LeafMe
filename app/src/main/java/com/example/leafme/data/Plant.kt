package com.example.leafme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import com.squareup.moshi.Json

@Entity(
    tableName = "plants",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices =[Index(value = ["userId"])]
)
data class Plant(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "plantId") // Nazwa kolumny w bazie danych
    @Json(name = "id") // Nazwa pola w JSON z serwera
    val id: Int = 0,

    val name: String,

    @ColumnInfo(name = "userId") // Nazwa kolumny w bazie danych
    @Json(name = "user_id") // Nazwa pola w JSON z serwera
    val userId: Int // Ta kolumna będzie kluczem obcym
)
