package com.healthsync.background.network

import com.healthsync.background.dto.HealthDataDto
import com.healthsync.background.dto.LoginRequest
import com.healthsync.background.dto.TokenResponse
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body refreshToken: String): TokenResponse

    @POST("health/data")
    suspend fun uploadHealthData(
        @Header("Authorization") authorization: String,
        @Body healthData: HealthDataDto
    )
}