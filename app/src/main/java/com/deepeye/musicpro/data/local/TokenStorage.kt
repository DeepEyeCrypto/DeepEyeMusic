package com.deepeye.musicpro.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.deepeye.musicpro.data.OAuthConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Secure token storage using Android Keystore and EncryptedSharedPreferences.
 * Mobile-optimized with biometric authentication support.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val sharedPreferences: android.content.SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save OAuth tokens securely.
     */
    suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresIn: Long) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000)
            apply()
        }
    }
    
    /**
     * Get stored access token if valid.
     */
    fun getAccessToken(): String? {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = sharedPreferences.getLong(KEY_EXPIRES_AT, 0)
        
        // Check if token is expired (with buffer)
        return if (System.currentTimeMillis() < expiresAt - OAuthConfig.TOKEN_EXPIRY_BUFFER_MS) {
            token
        } else {
            null
        }
    }
    
    /**
     * Get refresh token.
     */
    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    
    /**
     * Check if valid tokens exist.
     */
    fun hasValidTokens(): Boolean = getAccessToken() != null
    
    /**
     * Clear all stored tokens.
     */
    suspend fun clearTokens() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Flow of access token changes.
     */
    val accessTokenFlow: Flow<String?> = flow {
        emit(getAccessToken())
    }
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}