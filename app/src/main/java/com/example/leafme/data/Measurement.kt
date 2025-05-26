package com.example.leafme.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity(tableName = "measurements",
    foreignKeys = [ForeignKey(entity = Plant::class,
        parentColumns = ["plantId"],
        childColumns = ["plantId"],
        onDelete = ForeignKey.CASCADE)],
    indices = [androidx.room.Index("plantId")]
)
data class Measurement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantId: Int,
    @Json(name = "ts") val timeStamp: Int,
    val moisture: Float,
    val temperature: Float
)

