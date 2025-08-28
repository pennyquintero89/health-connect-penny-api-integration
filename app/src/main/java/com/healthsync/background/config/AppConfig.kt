package com.healthsync.background.config

object AppConfig {
    // true = debug mode (run sync)
    // false = prod mode (once a day)
    var testMode: Boolean = true
    var backendReady = false
}