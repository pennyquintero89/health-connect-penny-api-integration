package com.healthsync.background.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthsync.background.repository.HealthRepository
import com.healthsync.background.network.ApiService
import com.healthsync.background.security.TokenManager
import com.healthsync.background.mapper.HealthDataMapper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@HiltWorker
class HealthDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val healthRepository: HealthRepository,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("HealthDataWorker", "Worker ejecutándose...")
            val today = LocalDate.now()
            val healthData = healthRepository.getTodayHealthData(today)
            val healthDataDto = HealthDataMapper.toDto(healthData, today)

            val token = tokenManager.getAccessToken() ?: return@withContext Result.retry()

            try {
                apiService.uploadHealthData("Bearer $token", healthDataDto)
                Result.success()
            } catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> {
                        val refreshed = tokenManager.refreshToken()
                        if (refreshed) {
                            val newToken = tokenManager.getAccessToken()!!
                            apiService.uploadHealthData("Bearer $newToken", healthDataDto)
                            Result.success()
                        } else {
                            Result.failure()
                        }
                    }
                    else -> Result.retry()
                }
            }
        } catch (e: Exception) {
            Result.retry()
        }

    }

}