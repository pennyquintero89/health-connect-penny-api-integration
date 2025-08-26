package com.healthsync.background.model

data class HealthData(
    val totalSteps: Long,
    val totalActiveMinutes: Long,
    val totalCaloriesBurned: Double
)