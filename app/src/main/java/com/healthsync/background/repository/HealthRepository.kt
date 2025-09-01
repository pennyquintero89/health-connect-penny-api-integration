package com.healthsync.background.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.healthsync.background.model.DailyReportData
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

    suspend fun getTodayHealthData(date: LocalDate): DailyReportData = withContext(Dispatchers.IO) {
        Log.d(TAG, "Fetching health data for $date")

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)

        val totalSteps = getTotalStepsCount(timeRangeFilter)
        val totalStandingHours = getTotalStandingHours(timeRangeFilter)
        val totalDistance = getTotalDistance(timeRangeFilter)
        val totalActiveMinutes = getTotalActiveMinutes(timeRangeFilter)
        val totalActiveCalories = getTotalActiveCaloriesBurned(timeRangeFilter)
        val totalCalories = getTotalCaloriesBurned(timeRangeFilter)
        val currentBMR = getBasalMetabolicRate(timeRangeFilter)
        val avgHeartRate = getAverageHeartRate(timeRangeFilter)
        val avgRestingHeartRate = getAverageRestingHeartRate(timeRangeFilter)
        val currentTimezone = "+0100"


        Log.d(TAG, "Health data fetched: steps=$totalSteps, calories=$totalCalories, activeMinutes=$totalActiveMinutes")

        DailyReportData(
            steps = totalSteps,
            stand = totalStandingHours,
            distance = totalDistance,
            activeMinutes = totalActiveMinutes,
            activeCalories = totalActiveCalories,
            totalCalories = totalCalories,
            basalMetabolicRate = currentBMR,
            averageHeartRate = avgHeartRate,
            averageRestingHeartRate = avgRestingHeartRate,
            timeZone = currentTimezone
        )
    }

    private suspend fun getTotalStepsCount(timeRangeFilter: TimeRangeFilter): Int {
         val response =
                healthConnectClient.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = timeRangeFilter,
                    )
                )
            val total = response[StepsRecord.COUNT_TOTAL]
            Log.d(TAG, "Steps count: $total")

        return total?.toInt() ?: 0
    }

    private fun getTotalStandingHours(timeRangeFilter: TimeRangeFilter): Int {
        // TODO: Not available in Health Connect: We need to implement ourselves this logic
        return 0
    }

    private suspend fun getTotalDistance(timeRangeFilter: TimeRangeFilter): Double {
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = timeRangeFilter,
                )
            )
        val total = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters
        Log.d(TAG, "Total distance: $total meters")
        return total?: 0.0
    }

    private suspend fun getTotalActiveMinutes(timeRangeFilter: TimeRangeFilter): Int {
        // TODO: "Calculate and add non-sessions active minutes as well"

        val response = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                timeRangeFilter = timeRangeFilter,
            )
        )
        val duration = response[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()
        Log.d(TAG, "Total active minutes: $duration")

        return duration?.toInt() ?: 0
    }

    private suspend fun getTotalCaloriesBurned(timeRangeFilter: TimeRangeFilter): Double {
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = timeRangeFilter,
                )
            )
        val total = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
        Log.d(TAG, "Total kcal burned: $total")

        return total?: 0.0
    }

    private suspend fun getTotalActiveCaloriesBurned(timeRangeFilter: TimeRangeFilter): Double {
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = timeRangeFilter,
                )
            )
        val total = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
        Log.d(TAG, "Total kcal burned from exercise: $total")

        return total?: 0.0
    }

    private suspend fun getBasalMetabolicRate(timeRangeFilter: TimeRangeFilter): Double {
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL),
                    timeRangeFilter = timeRangeFilter,
                )
            )
        val total = response[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]?.inKilocalories
        Log.d(TAG, "Current user's Basal Metabolic Rate: $total")

        return total?: 0.0
    }

    private suspend fun getAverageHeartRate(timeRangeFilter: TimeRangeFilter): Int {
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(HeartRateRecord.BPM_AVG),
                    timeRangeFilter = timeRangeFilter,
                )
            )
        val total = response[HeartRateRecord.BPM_AVG]
        Log.d(TAG, "Average Heart Rate: $total")

        return total?.toInt() ?: 0
    }
    private suspend fun getAverageRestingHeartRate(timeRangeFilter: TimeRangeFilter): Int {
        val response =
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(RestingHeartRateRecord.BPM_AVG),
                    timeRangeFilter = timeRangeFilter,
                )
            )
        val total = response[RestingHeartRateRecord.BPM_AVG]
        Log.d(TAG, "Average Resting Heart Rate: $total")

        return total?.toInt() ?: 0
    }
}