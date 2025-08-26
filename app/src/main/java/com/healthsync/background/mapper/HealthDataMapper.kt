package com.healthsync.background.mapper

import com.healthsync.background.model.HealthData
import com.healthsync.background.dto.HealthDataDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HealthDataMapper {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toDto(healthData: HealthData, date: LocalDate): HealthDataDto {
        return HealthDataDto(
            totalSteps = healthData.totalSteps,
            totalActiveMinutes = healthData.totalActiveMinutes,
            totalCaloriesBurned = healthData.totalCaloriesBurned,
            date = date.format(dateFormatter)
        )
    }
}