package com.healthsync.background.repository

import android.util.Log
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

private const val TAG = "HealthRepository"

@Singleton
class HealthRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient
) {

    suspend fun getTodayHealthData(date: LocalDate): HealthData = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching health data for $date")

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)

        val totalSteps = getStepsCount(timeRangeFilter)
        val totalCalories = getCaloriesBurned(timeRangeFilter)
        val totalActiveMinutes = getActiveMinutes(timeRangeFilter)

        Log.d(TAG, "Health data fetched: steps=$totalSteps, calories=$totalCalories, activeMinutes=$totalActiveMinutes")

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
            val total = response.records.sumOf { it.count }
            Log.d(TAG, "Steps count: $total from ${response.records.size} records")
            total
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps: ${e.message}", e)
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
            val total = response.records.sumOf { it.energy.inKilocalories }
            Log.d(TAG, "Calories burned: $total from ${response.records.size} records")
            total
        } catch (e: Exception) {
            Log.e(TAG, "Error reading calories burned: ${e.message}", e)
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
            val total = response.records.sumOf {
                java.time.Duration.between(it.startTime, it.endTime).toMinutes()
            }
            Log.d(TAG, "Active minutes: $total from ${response.records.size} records")
            total
        } catch (e: Exception) {
            Log.e(TAG, "Error reading active minutes: ${e.message}", e)
            0L
        }
    }
}