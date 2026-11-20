package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CurrencyOption
import com.example.data.model.ThemeMode
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "spendwise_settings")

data class AppPreferencesState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val currencyCode: String = CurrencyOption.BDT.code,
    val language: AppLanguage = AppLanguage.BN,
    val balanceAlertLimit: Double = 2000.0
)

class SettingsPreferences(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val LANGUAGE = stringPreferencesKey("app_language")
        val BALANCE_ALERT_LIMIT = doublePreferencesKey("balance_alert_limit")
    }

    val preferencesFlow: Flow<AppPreferencesState> = dataStore.data.map { prefs ->
        AppPreferencesState(
            themeMode = ThemeMode.fromString(prefs[Keys.THEME_MODE]),
            biometricEnabled = prefs[Keys.BIOMETRIC_ENABLED] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            currencyCode = CurrencyOption.fromCode(prefs[Keys.CURRENCY_CODE] ?: CurrencyOption.BDT.code).code,
            language = AppLanguage.fromString(prefs[Keys.LANGUAGE]),
            balanceAlertLimit = prefs[Keys.BALANCE_ALERT_LIMIT] ?: 2000.0
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENCY_CODE] = CurrencyOption.fromCode(code).code
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.name
        }
    }

    suspend fun setBalanceAlertLimit(limit: Double) {
        dataStore.edit { prefs ->
            prefs[Keys.BALANCE_ALERT_LIMIT] = limit.coerceAtLeast(0.0)
        }
    }
}
