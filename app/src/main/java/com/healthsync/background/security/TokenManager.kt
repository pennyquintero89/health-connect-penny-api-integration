package com.healthsync.background.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.healthsync.background.dto.LoginRequest
import com.healthsync.background.dto.TokenResponse
import com.healthsync.background.network.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getAccessToken(): String? = encryptedPrefs.getString("access_token", null)

    private fun getRefreshToken(): String? = encryptedPrefs.getString("refresh_token", null)

    private fun saveTokens(tokenResponse: TokenResponse) {
        encryptedPrefs.edit {
            putString("access_token", tokenResponse.accessToken)
                .putString("refresh_token", tokenResponse.refreshToken)
        }
    }

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val loginRequest = LoginRequest(username, password)
            val tokenResponse = apiService.login(loginRequest)
            saveTokens(tokenResponse)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        try {
            val refreshToken = getRefreshToken() ?: return@withContext false
            val tokenResponse = apiService.refreshToken(refreshToken)
            saveTokens(tokenResponse)
            true
        } catch (e: Exception) {
            clearTokens()
            false
        }
    }

    private fun clearTokens() {
        encryptedPrefs.edit {
            remove("access_token")
                .remove("refresh_token")
        }
    }
}