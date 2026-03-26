package com.disastermesh.connect.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.disastermesh.connect.domain.model.BatteryMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_prefs")

/**
 * UserPreferencesManager — Stores user identity and app settings.
 * Backed by DataStore (encrypted if needed via EncryptedSharedPreferences upgrade).
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.userPreferencesDataStore

    companion object {
        val KEY_DEVICE_ID       = stringPreferencesKey("device_id")
        val KEY_DISPLAY_NAME    = stringPreferencesKey("display_name")
        val KEY_PHONE_NUMBER    = stringPreferencesKey("phone_number")
        val KEY_BATTERY_MODE    = stringPreferencesKey("battery_mode")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_NOTIFICATIONS   = booleanPreferencesKey("notifications_enabled")
        val KEY_LOCATION_SHARE  = booleanPreferencesKey("location_share_enabled")
        val KEY_AVATAR_COLOR    = intPreferencesKey("avatar_color")
    }

    // ─── Device ID (auto-generated, never changes) ───────
    val deviceIdFlow: Flow<String> = store.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: generateAndStoreDeviceId()
    }

    private suspend fun generateAndStoreDeviceId(): String {
        val newId = "DM-${UUID.randomUUID()}"
        store.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }

    suspend fun getDeviceId(): String {
        var id = ""
        store.edit { prefs ->
            id = prefs[KEY_DEVICE_ID] ?: run {
                val newId = "DM-${UUID.randomUUID()}"
                prefs[KEY_DEVICE_ID] = newId
                newId
            }
        }
        return id
    }

    // ─── Display Name ─────────────────────────────────────
    val displayNameFlow: Flow<String> = store.data.map { it[KEY_DISPLAY_NAME] ?: "" }

    suspend fun setDisplayName(name: String) {
        store.edit { it[KEY_DISPLAY_NAME] = name.trim() }
    }

    // ─── Phone Number (optional) ──────────────────────────
    val phoneNumberFlow: Flow<String?> = store.data.map { it[KEY_PHONE_NUMBER] }

    suspend fun setPhoneNumber(phone: String?) {
        store.edit {
            if (phone != null) it[KEY_PHONE_NUMBER] = phone
            else it.remove(KEY_PHONE_NUMBER)
        }
    }

    // ─── Battery Mode ─────────────────────────────────────
    val batteryModeFlow: Flow<BatteryMode> = store.data.map {
        BatteryMode.valueOf(it[KEY_BATTERY_MODE] ?: BatteryMode.NORMAL.name)
    }

    suspend fun setBatteryMode(mode: BatteryMode) {
        store.edit { it[KEY_BATTERY_MODE] = mode.name }
    }

    // ─── Onboarding ───────────────────────────────────────
    val isOnboardingDoneFlow: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    // ─── Notifications ────────────────────────────────────
    val notificationsEnabledFlow: Flow<Boolean> = store.data.map { it[KEY_NOTIFICATIONS] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    // ─── Location Sharing ─────────────────────────────────
    val locationShareEnabledFlow: Flow<Boolean> = store.data.map { it[KEY_LOCATION_SHARE] ?: false }

    suspend fun setLocationShareEnabled(enabled: Boolean) {
        store.edit { it[KEY_LOCATION_SHARE] = enabled }
    }

    // ─── Avatar Color ─────────────────────────────────────
    val avatarColorFlow: Flow<Int> = store.data.map { it[KEY_AVATAR_COLOR] ?: generateAvatarColor() }

    private fun generateAvatarColor(): Int {
        val colors = listOf(0xFFE53935, 0xFF8E24AA, 0xFF1E88E5, 0xFF00897B, 0xFFFB8C00)
        return colors.random().toInt()
    }
}
