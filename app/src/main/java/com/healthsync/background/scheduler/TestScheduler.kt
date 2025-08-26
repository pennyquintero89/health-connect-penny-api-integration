package com.healthsync.background.scheduler

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.healthsync.background.config.WorkScheduler
import com.healthsync.background.worker.HealthDataWorker
import java.util.concurrent.TimeUnit

class TestScheduler : WorkScheduler {
    override fun scheduleWork(context: Context) {
        val testWork = OneTimeWorkRequestBuilder<HealthDataWorker>()
            .setInitialDelay(1, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(testWork)
    }
}