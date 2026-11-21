package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mali_prefs")

data class UserPreferences(
    val hasSeededInitial: Boolean = false,
    val isDemoMode: Boolean = true,
    val hideBalances: Boolean = false,
    val primaryCurrency: String = "MRU",
    val currencyConversion: Boolean = false,
    val languageCode: String = "ar",
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val pinLockEnabled: Boolean = false,
    val pinHash: String = "",
    val biometricEnabled: Boolean = false
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val HAS_SEEDED = booleanPreferencesKey("has_seeded_initial")
        val IS_DEMO = booleanPreferencesKey("is_demo_mode")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val PRIMARY_CURRENCY = stringPreferencesKey("primary_currency")
        val CURRENCY_CONVERSION = booleanPreferencesKey("currency_conversion")
        val LANGUAGE = stringPreferencesKey("language_code")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            hasSeededInitial = prefs[Keys.HAS_SEEDED] ?: false,
            isDemoMode = prefs[Keys.IS_DEMO] ?: true,
            hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
            primaryCurrency = prefs[Keys.PRIMARY_CURRENCY] ?: "MRU",
            currencyConversion = prefs[Keys.CURRENCY_CONVERSION] ?: false,
            languageCode = prefs[Keys.LANGUAGE] ?: "ar",
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            pinLockEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false
        )
    }

    suspend fun markSeeded(isDemo: Boolean) {
        context.dataStore.edit {
            it[Keys.HAS_SEEDED] = true
            it[Keys.IS_DEMO] = isDemo
        }
    }

    suspend fun setDemoMode(isDemo: Boolean) {
        context.dataStore.edit { it[Keys.IS_DEMO] = isDemo }
    }

    suspend fun setHideBalances(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_BALANCES] = hide }
    }

    suspend fun setPrimaryCurrency(currency: String) {
        context.dataStore.edit { it[Keys.PRIMARY_CURRENCY] = currency }
    }

    suspend fun setCurrencyConversion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CURRENCY_CONVERSION] = enabled }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = dark }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun enablePinLock(rawPin: String) {
        val hash = hashPin(rawPin)
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = true
            it[Keys.PIN_HASH] = hash
        }
    }

    suspend fun disablePinLock() {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    fun verifyPin(rawPin: String, storedHash: String): Boolean {
        return storedHash.isNotEmpty() && hashPin(rawPin) == storedHash
    }

    private fun hashPin(rawPin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(("mali_pin_salt_v1:" + rawPin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
