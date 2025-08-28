package com.healthsync.background.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.healthsync.background.config.AppConfig
import com.healthsync.background.config.WorkScheduler
import com.healthsync.background.scheduler.DailyScheduler
import com.healthsync.background.scheduler.TestScheduler
import dagger.hilt.android.AndroidEntryPoint

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