package com.healthsync.background.network

import com.healthsync.background.dto.HealthDataDto
import com.healthsync.background.dto.TokenRequest
import com.healthsync.background.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("auth/token")
    suspend fun getClientToken(@Body token: TokenRequest): TokenResponse

    @POST("health/data")
    suspend fun uploadHealthData(
        @Header("Authorization") authorization: String,
        @Body healthData: HealthDataDto
    )
}