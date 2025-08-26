package com.healthsync.background.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.healthsync.background.config.AppConfig
import com.healthsync.background.config.WorkScheduler
import com.healthsync.background.scheduler.DailyScheduler
import com.healthsync.background.scheduler.TestScheduler
import com.healthsync.background.worker.HealthDataWorker
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleHealthDataSync()
        finish()
    }
    private fun scheduleHealthDataSync() {
        val scheduler: WorkScheduler = if (AppConfig.testMode) TestScheduler() else DailyScheduler()
        scheduler.scheduleWork(this)
    }
}