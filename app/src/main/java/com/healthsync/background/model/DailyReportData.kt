package com.healthsync.background.model

data class DailyReportData(
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