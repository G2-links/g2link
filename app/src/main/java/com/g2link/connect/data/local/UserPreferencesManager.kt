package com.g2link.connect.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.g2link.connect.domain.model.BatteryMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "user_prefs")

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
    }

    val deviceIdFlow: Flow<String> = store.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: "DM-${UUID.randomUUID()}"
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

    val displayNameFlow: Flow<String> = store.data.map { it[KEY_DISPLAY_NAME] ?: "" }
    suspend fun setDisplayName(name: String) { store.edit { it[KEY_DISPLAY_NAME] = name.trim() } }

    val phoneNumberFlow: Flow<String?> = store.data.map { it[KEY_PHONE_NUMBER] }
    suspend fun setPhoneNumber(phone: String?) {
        store.edit {
            if (phone != null) it[KEY_PHONE_NUMBER] = phone else it.remove(KEY_PHONE_NUMBER)
        }
    }

    val batteryModeFlow: Flow<BatteryMode> = store.data.map {
        BatteryMode.valueOf(it[KEY_BATTERY_MODE] ?: BatteryMode.NORMAL.name)
    }
    suspend fun setBatteryMode(mode: BatteryMode) { store.edit { it[KEY_BATTERY_MODE] = mode.name } }

    val isOnboardingDoneFlow: Flow<Boolean> = store.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    suspend fun setOnboardingDone(done: Boolean) { store.edit { it[KEY_ONBOARDING_DONE] = done } }

    val notificationsEnabledFlow: Flow<Boolean> = store.data.map { it[KEY_NOTIFICATIONS] ?: true }
    suspend fun setNotificationsEnabled(enabled: Boolean) { store.edit { it[KEY_NOTIFICATIONS] = enabled } }

    val locationShareEnabledFlow: Flow<Boolean> = store.data.map { it[KEY_LOCATION_SHARE] ?: false }
    suspend fun setLocationShareEnabled(enabled: Boolean) { store.edit { it[KEY_LOCATION_SHARE] = enabled } }
}
