package com.example.leafme.retrofit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Interfejs API do zarządzania roślinami i pomiarami.
 */
interface PlantApiService {
    // Pobieranie listy roślin użytkownika
    @GET("/api/plants")
    suspend fun getUserPlants(): Response<List<Plant>>

    // Tworzenie nowej rośliny
    @POST("/api/plants")
    suspend fun createPlant(@Body request: CreatePlantRequest): Response<CreatePlantResponse>

    // Usunięcie rośliny
    @DELETE("/api/plants/{plantId}")
    suspend fun deletePlant(@Path("plantId") plantId: Int): Response<Map<String, String>>

    // Pobieranie pomiarów dla konkretnej rośliny
    @GET("/api/measurements/{plantId}")
    suspend fun getMeasurements(@Path("plantId") plantId: Int): Response<List<Measurement>>

    // Podlewanie rośliny
    @POST("/api/plants/{plantId}/water")
    suspend fun waterPlant(
        @Path("plantId") plantId: Int,
        @Body request: WaterRequest
    ): Response<WaterResponse>

    // Pobieranie identyfikatorów roślin użytkownika
    @GET("/api/plants/ids")
    suspend fun getPlantIds(): Response<Map<String, List<Int>>>
}
