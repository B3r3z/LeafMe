package com.example.leafme.retrofit

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfejs API do obsługi autoryzacji.
 */
interface AuthApiService {
    @POST("/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
