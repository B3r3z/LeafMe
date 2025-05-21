package com.example.leafme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "plants",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Plant(
    @PrimaryKey(autoGenerate = true) val plantId: Int = 0,
    val name: String,
    val userId: Int // Ta kolumna będzie kluczem obcym
)