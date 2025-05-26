package com.example.leafme.retrofit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Klasy odpowiedzi dla autoryzacji
@JsonClass(generateAdapter = true)
data class RegisterResponse(
    @Json(name = "msg")
    val msg: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "access_token")
    val accessToken: String
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    @Json(name = "msg")
    val msg: String? = null,
    @Json(name = "error")
    val error: String? = null
)

// Klasy odpowiedzi dla API użytkownika
@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id")
    val id: Int,
    @Json(name = "email")
    val email: String
)

// Klasy odpowiedzi dla zarządzania roślinami
@JsonClass(generateAdapter = true)
data class Plant(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String,
    @Json(name = "user_id")
    val userId: Int? = null
)

@JsonClass(generateAdapter = true)
data class CreatePlantResponse(
    @Json(name = "id")
    val id: Int,
    @Json(name = "name")
    val name: String,
    @Json(name = "user_id")
    val userId: Int
)

// Klasy odpowiedzi dla pomiarów
@JsonClass(generateAdapter = true)
data class Measurement(
    @Json(name = "ts")
    val ts: Long,
    @Json(name = "moisture")
    val moisture: Float,
    @Json(name = "temperature")
    val temperature: Float
)

@JsonClass(generateAdapter = true)
data class MeasurementsResponse(
    @Json(name = "measurements")
    val measurements: List<Measurement>
)

// Klasy odpowiedzi dla podlewania
@JsonClass(generateAdapter = true)
data class WaterResponse(
    @Json(name = "status")
    val status: String
)

// Klasy żądań (requests)
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "email")
    val email: String,
    @Json(name = "password")
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email")
    val email: String,
    @Json(name = "password")
    val password: String
)

@JsonClass(generateAdapter = true)
data class CreatePlantRequest(
    @Json(name = "name")
    val name: String,
    @Json(name = "plant_id")
    val plantId: Int? = null
)

@JsonClass(generateAdapter = true)
data class WaterRequest(
    @Json(name = "duration_ms")
    val durationMs: Int = 5000
)

// Nowa klasa dla odpowiedzi z identyfikatorami roślin
@JsonClass(generateAdapter = true)
data class PlantIdsResponse(
    @Json(name = "plant_ids")
    val plantIds: List<Int>
)

