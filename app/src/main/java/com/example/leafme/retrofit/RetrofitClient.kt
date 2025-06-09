package com.example.leafme.retrofit

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Interceptor do dodawania tokenu JWT do nagłówków żądań
 */

class AuthInterceptor(private var token: String = "") : Interceptor {
    fun setToken(newToken: String) {
        token = newToken
    }

    override fun intercept(chain: Interceptor.Chain) = chain.proceed(
        chain.request().let { originalRequest ->
            // Dodajemy token tylko jeśli nie jest pusty
            if (token.isNotEmpty()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
        }
    )
}

object RetrofitClient {
    // Base URL dla API
    private const val BASE_URL = "http://10.160.35.80:8000"  // Zmień na właściwy adres serwera

    // Instancja AuthInterceptor do zarządzania tokenem JWT
    private val authInterceptor = AuthInterceptor()

    // OkHttpClient z interceptorem
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    // Funkcja budująca i zwracająca instancję Retrofit
    private fun getClient(): Retrofit {
        // Tworzymy instancję Moshi do parsowania JSON
        // KotlinJsonAdapterFactory jest używany do automatycznego generowania adapterów dla klas danych
        val moshi = Moshi.Builder()
            .build()

        // Tworzymy instancję Retrofit z BASE_URL i konwerterem Moshi
        // używając wzorca budowniczego
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    // Funkcja do aktualizacji tokenu JWT
    fun setAuthToken(token: String) {
        authInterceptor.setToken(token)
    }

    // Obiekty lazy do utworzenia i przechowywania serwisów API
    val authService: AuthApiService by lazy {
        getClient().create(AuthApiService::class.java)
    }

    val plantService: PlantApiService by lazy {
        getClient().create(PlantApiService::class.java)
    }

    val userService: UserApiService by lazy {
        getClient().create(UserApiService::class.java)
    }
}

