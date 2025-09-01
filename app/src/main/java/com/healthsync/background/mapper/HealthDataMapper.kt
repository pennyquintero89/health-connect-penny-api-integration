package com.healthsync.background.mapper

import com.healthsync.background.model.dto.DailyReportDTO
import com.healthsync.background.model.DailyReportData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object HealthDataMapper {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    fun toDto(dailyReportData: DailyReportData, date: LocalDate): DailyReportDTO {
        return DailyReportDTO(
            date = date.format(dateFormatter),
            steps = dailyReportData.steps,
            stand = dailyReportData.stand,
            distance = dailyReportData.distance,
            activeMinutes = dailyReportData.activeMinutes,
            activeCalories = dailyReportData.activeCalories,
            totalCalories = dailyReportData.totalCalories,
            basalMetabolicRate = dailyReportData.basalMetabolicRate,
            averageHeartRate = dailyReportData.averageHeartRate,
            averageRestingHeartRate = dailyReportData.averageRestingHeartRate,
            timeZone = dailyReportData.timeZone,
        )
    }
}