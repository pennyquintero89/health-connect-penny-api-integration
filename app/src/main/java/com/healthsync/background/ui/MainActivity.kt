package com.healthsync.background.ui

import android.app.Activity
import android.os.Bundle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.healthsync.background.worker.HealthDataWorker
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleHealthDataSync()
        finish()
    }

    private fun scheduleHealthDataSync() {
        val now = LocalDateTime.now()
        val targetTime = now.toLocalDate().atTime(LocalTime.of(23, 57))
        val nextTargetTime = if (now.isAfter(targetTime)) {
            targetTime.plusDays(1)
        } else {
            targetTime
        }

        val initialDelay = ChronoUnit.MINUTES.between(now, nextTargetTime)

        val workRequest = PeriodicWorkRequestBuilder<HealthDataWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "health_data_sync",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}