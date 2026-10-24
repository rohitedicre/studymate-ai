package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class AuthUserState(
    val userId: Long = -1L,
    val username: String = "",
    val email: String = "",
    val isLoggedIn: Boolean = false
)

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("studymate_session_prefs", Context.MODE_PRIVATE)

    private val _userState = MutableStateFlow(loadUserState())
    val userState: StateFlow<AuthUserState> = _userState.asStateFlow()

    private fun loadUserState(): AuthUserState {
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        val username = prefs.getString(KEY_USERNAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && userId != -1L
        return AuthUserState(
            userId = userId,
            username = username,
            email = email,
            isLoggedIn = isLoggedIn
        )
    }

    fun saveUserSession(userId: Long, username: String, email: String) {
        prefs.edit()
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_EMAIL, email)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
        _userState.value = AuthUserState(
            userId = userId,
            username = username,
            email = email,
            isLoggedIn = true
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
        _userState.value = AuthUserState(
            userId = -1L,
            username = "",
            email = "",
            isLoggedIn = false
        )
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    companion object {
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_CUSTOM_API_KEY = "key_custom_api_key"

        fun generateSalt(): String {
            val random = SecureRandom()
            val salt = ByteArray(16)
            random.nextBytes(salt)
            return Base64.getEncoder().encodeToString(salt)
        }

        fun hashPassword(password: String, salt: String): String {
            val combined = "$password:$salt"
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(combined.toByteArray(Charsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
}
