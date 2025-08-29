package com.healthsync.background.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthsync.background.R
import com.healthsync.background.config.AppConfig
import com.healthsync.background.config.DeviceIdManager
import com.healthsync.background.dto.TokenRequest
import com.healthsync.background.mapper.HealthDataMapper
import com.healthsync.background.network.ApiService
import com.healthsync.background.repository.HealthRepository
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
    private val deviceIdManager: DeviceIdManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("HealthDataWorker", "Sync Starting...")

            val today = LocalDate.now()
            val healthData = healthRepository.getTodayHealthData(today)
            val healthDataDto = HealthDataMapper.toDto(healthData, today)

            if (!AppConfig.backendReady) {return@withContext Result.success()}

            val deviceId = deviceIdManager.getOrCreateDeviceId(applicationContext)

            val tokenResponse = apiService.getClientToken(TokenRequest(deviceId))
            val accessToken = tokenResponse.accessToken


            apiService.uploadHealthData("Bearer $accessToken", healthDataDto)
            Log.d("HealthDataWorker", "Sync Completed.")
            showNotification(applicationContext, "Health Data Sync Completed.")

            Result.success()

        } catch (e: retrofit2.HttpException) {
            Log.e("HealthDataWorker", "HTTP Error: ${e.code()}")
            if (e.code() == 401) {
                return@withContext Result.retry()
            }
            Result.retry()
        } catch (e: Exception) {
            Log.e("HealthDataWorker", "Error: ${e.message}")
            Result.retry()
        }

    }

    fun showNotification(context: Context, message: String) {
        val channelId = "health_sync_channel"
        val channelName = "Health Sync Notifications"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("HealthSync")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(1, notification)
    }
}

