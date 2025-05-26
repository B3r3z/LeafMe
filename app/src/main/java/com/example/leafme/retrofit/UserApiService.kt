package com.example.leafme.retrofit

import retrofit2.Response
import retrofit2.http.GET

/**
 * Interfejs API do operacji na danych użytkownika.
 */
interface UserApiService {
    @GET("/api/me")
    suspend fun getUserInfo(): Response<UserResponse>
}
