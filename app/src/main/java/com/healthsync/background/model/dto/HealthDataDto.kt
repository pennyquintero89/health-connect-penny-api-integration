package com.healthsync.background.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthDataDto(
    val totalSteps: Long,
    val totalActiveMinutes: Long,
    val totalCaloriesBurned: Double,
    val date: String
)