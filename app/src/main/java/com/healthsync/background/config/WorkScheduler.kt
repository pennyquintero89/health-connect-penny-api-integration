package com.healthsync.background.config

import android.content.Context

interface WorkScheduler {
    fun scheduleWork(context: Context)
}