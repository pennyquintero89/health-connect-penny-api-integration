package com.healthsync.background.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.healthsync.background.config.WorkScheduler
import com.healthsync.background.worker.HealthDataWorker
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class DailyScheduler : WorkScheduler {
    override fun scheduleWork(context: Context) {
        val now = LocalDateTime.now()
        val targetTime = now.toLocalDate().atTime(LocalTime.of(23, 57))
        val nextTargetTime = if (now.isAfter(targetTime)) targetTime.plusDays(1) else targetTime
        val initialDelay = ChronoUnit.MINUTES.between(now, nextTargetTime)

        val workRequest = PeriodicWorkRequestBuilder<HealthDataWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "health_data_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}