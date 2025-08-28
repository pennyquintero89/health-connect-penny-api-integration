package com.healthsync.background.dto

import kotlinx.serialization.Serializable

@Serializable
data class TokenRequest(
    val deviceId: String
)