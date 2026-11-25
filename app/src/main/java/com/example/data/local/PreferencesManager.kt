package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notif_vault_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val MONITORED_PACKAGES = stringSetPreferencesKey("monitored_packages")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")
        val IGNORE_SENSITIVE_OTP = booleanPreferencesKey("ignore_sensitive_otp")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val monitoredPackages: Flow<Set<String>> = preferencesFlow
        .map { it[Keys.MONITORED_PACKAGES] ?: emptySet() }

    val excludedPackages: Flow<Set<String>> = preferencesFlow
        .map { it[Keys.EXCLUDED_PACKAGES] ?: emptySet() }

    val ignoreSensitiveOtp: Flow<Boolean> = preferencesFlow
        .map { it[Keys.IGNORE_SENSITIVE_OTP] ?: true }

    val themeMode: Flow<ThemeMode> = preferencesFlow
        .map { ThemeMode.fromKey(it[Keys.THEME_MODE]) }

    suspend fun setPackageMonitored(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val currentMonitored = prefs[Keys.MONITORED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            val currentExcluded = prefs[Keys.EXCLUDED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (enabled) {
                currentMonitored.add(packageName)
                currentExcluded.remove(packageName)
            } else {
                currentMonitored.remove(packageName)
            }
            prefs[Keys.MONITORED_PACKAGES] = currentMonitored
            prefs[Keys.EXCLUDED_PACKAGES] = currentExcluded
        }
    }

    suspend fun setPackageExcluded(packageName: String, excluded: Boolean) {
        context.dataStore.edit { prefs ->
            val currentMonitored = prefs[Keys.MONITORED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            val currentExcluded = prefs[Keys.EXCLUDED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (excluded) {
                currentExcluded.add(packageName)
                currentMonitored.remove(packageName)
            } else {
                currentExcluded.remove(packageName)
            }
            prefs[Keys.MONITORED_PACKAGES] = currentMonitored
            prefs[Keys.EXCLUDED_PACKAGES] = currentExcluded
        }
    }

    suspend fun setIgnoreSensitiveOtp(ignore: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IGNORE_SENSITIVE_OTP] = ignore
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.storageKey
        }
    }

    suspend fun shouldCapturePackage(packageName: String): Boolean {
        val prefs = preferencesFlow.first()
        val monitored = prefs[Keys.MONITORED_PACKAGES] ?: emptySet()
        val excluded = prefs[Keys.EXCLUDED_PACKAGES] ?: emptySet()
        return packageName in monitored && packageName !in excluded
    }

    suspend fun isSensitiveOtpIgnored(): Boolean {
        return ignoreSensitiveOtp.first()
    }
}
