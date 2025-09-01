package com.healthsync.background.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyReportDTO(
    val date: String,
    val steps: Int,
    val stand: Int,
    val distance: Double,
    val activeMinutes: Int,
    val activeCalories: Double,
    val totalCalories: Double,
    val basalMetabolicRate: Double,
    val averageHeartRate: Int,
    val averageRestingHeartRate: Int,
    val timeZone: String
)