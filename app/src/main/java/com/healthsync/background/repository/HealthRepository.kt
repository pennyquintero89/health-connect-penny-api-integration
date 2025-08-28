package com.healthsync.background.repository

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthsync.background.model.HealthData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) {

    suspend fun getTodayHealthData(date: LocalDate): HealthData = withContext(Dispatchers.IO) {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)

        val totalSteps = getStepsCount(timeRangeFilter)
        val totalCalories = getCaloriesBurned(timeRangeFilter)
        val totalActiveMinutes = getActiveMinutes(timeRangeFilter)

        HealthData(
            totalSteps = totalSteps,
            totalActiveMinutes = totalActiveMinutes,
            totalCaloriesBurned = totalCalories
        )
    }

    private suspend fun getStepsCount(timeRangeFilter: TimeRangeFilter): Long {
        return try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            0L
        }
    }

    private suspend fun getCaloriesBurned(timeRangeFilter: TimeRangeFilter): Double {
        return try {
            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.energy.inKilocalories }
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun getActiveMinutes(timeRangeFilter: TimeRangeFilter): Long {
        return try {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = timeRangeFilter
            )
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).toMinutes()
            }
        } catch (e: Exception) {
            0L
        }
    }
}