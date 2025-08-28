package com.healthsync.background.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.healthsync.background.config.WorkScheduler
import com.healthsync.background.worker.HealthDataWorker
import java.util.concurrent.TimeUnit

class TestScheduler : WorkScheduler {
    override fun scheduleWork(context: Context) {
        val testWork = OneTimeWorkRequestBuilder<HealthDataWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "TestHealthDataWork",
            ExistingWorkPolicy.REPLACE,
            testWork
        )
    }
}