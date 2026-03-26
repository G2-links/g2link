package com.disastermesh.connect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disastermesh.connect.data.local.UserPreferencesManager
import com.disastermesh.connect.data.local.dao.ContactDao
import com.disastermesh.connect.data.local.dao.MessageDao
import com.disastermesh.connect.data.local.dao.PeerDao
import com.disastermesh.connect.data.local.entity.ContactEntity
import com.disastermesh.connect.data.local.entity.MessageEntity
import com.disastermesh.connect.domain.model.*
import com.disastermesh.connect.mesh.MeshEvent
import com.disastermesh.connect.mesh.OfflineMeshManager
import com.disastermesh.connect.service.MeshForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════
// MAIN / CHAT LIST VIEW MODEL
// ═══════════════════════════════════════════════════════════
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val meshManager: OfflineMeshManager,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val peerDao: PeerDao,
    private val userPrefsManager: UserPreferencesManager
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = meshManager.connectionStatus
    val connectedPeerCount: StateFlow<Int> = meshManager.connectedPeerCount

    val myDeviceId: Flow<String> = userPrefsManager.deviceIdFlow
    val displayName: Flow<String> = userPrefsManager.displayNameFlow

    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContactsFlow()
    val connectedPeers = peerDao.getConnectedPeersFlow()

    // Latest message per conversation for chat list preview
    val latestMessages: Flow<List<MessageEntity>> = userPrefsManager.deviceIdFlow.flatMapLatest {
        messageDao.getLatestMessagesPerContact(it)
    }

    val broadcastMessages: Flow<List<MessageEntity>> = messageDao.getBroadcastMessagesFlow()

    // Incoming mesh events for real-time UI updates
    val meshEvents: SharedFlow<MeshEvent> = meshManager.meshEvents

    fun sendBroadcastAlert(message: String) {
        viewModelScope.launch {
            meshManager.sendBroadcast(message)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// CHAT SCREEN VIEW MODEL
// ═══════════════════════════════════════════════════════════
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val meshManager: OfflineMeshManager,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val userPrefsManager: UserPreferencesManager
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = meshManager.connectionStatus
    val meshEvents: SharedFlow<MeshEvent> = meshManager.meshEvents

    private val _contactDeviceId = MutableStateFlow<String?>(null)

    val messages: Flow<List<MessageEntity>> = combine(
        userPrefsManager.deviceIdFlow,
        _contactDeviceId.filterNotNull()
    ) { myId, contactId ->
        Pair(myId, contactId)
    }.flatMapLatest { (myId, contactId) ->
        messageDao.getConversationFlow(myId, contactId)
    }

    val contactInfo: Flow<ContactEntity?> = _contactDeviceId.filterNotNull().flatMapLatest {
        flow { emit(contactDao.getContactByDeviceId(it)) }
    }

    val myDeviceId: Flow<String> = userPrefsManager.deviceIdFlow

    val unreadCount: Flow<Int> = _contactDeviceId.filterNotNull().flatMapLatest { contactId ->
        messageDao.getUnreadCountForContactFlow(contactId)
    }

    fun setContact(deviceId: String) {
        _contactDeviceId.value = deviceId
    }

    fun sendMessage(content: String, isEmergency: Boolean = false) {
        val recipientId = _contactDeviceId.value ?: return
        viewModelScope.launch {
            meshManager.sendMessage(recipientId, content, isEmergency)
        }
    }

    fun shareLocation(lat: Double, lng: Double, accuracy: Float) {
        viewModelScope.launch {
            meshManager.shareLocation(lat, lng, accuracy)
        }
    }

    fun markContactAsPriority(type: ContactType) {
        viewModelScope.launch {
            _contactDeviceId.value?.let { id ->
                contactDao.updateContactType(id, type)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// ONBOARDING VIEW MODEL
// ═══════════════════════════════════════════════════════════
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPrefsManager: UserPreferencesManager
) : ViewModel() {

    val isOnboardingDone: Flow<Boolean> = userPrefsManager.isOnboardingDoneFlow
    val displayName: Flow<String> = userPrefsManager.displayNameFlow

    fun saveProfile(name: String, phone: String?) {
        viewModelScope.launch {
            userPrefsManager.setDisplayName(name)
            userPrefsManager.setPhoneNumber(phone?.takeIf { it.isNotBlank() })
            userPrefsManager.setOnboardingDone(true)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SETTINGS VIEW MODEL
// ═══════════════════════════════════════════════════════════
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val meshManager: OfflineMeshManager,
    private val userPrefsManager: UserPreferencesManager
) : ViewModel() {

    val displayName: Flow<String> = userPrefsManager.displayNameFlow
    val phoneNumber: Flow<String?> = userPrefsManager.phoneNumberFlow
    val batteryMode: Flow<BatteryMode> = userPrefsManager.batteryModeFlow
    val notificationsEnabled: Flow<Boolean> = userPrefsManager.notificationsEnabledFlow
    val locationShareEnabled: Flow<Boolean> = userPrefsManager.locationShareEnabledFlow
    val connectionStatus: StateFlow<ConnectionStatus> = meshManager.connectionStatus
    val connectedPeerCount: StateFlow<Int> = meshManager.connectedPeerCount
    val myDeviceId: Flow<String> = userPrefsManager.deviceIdFlow

    fun setBatteryMode(mode: BatteryMode) {
        viewModelScope.launch { userPrefsManager.setBatteryMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsManager.setNotificationsEnabled(enabled) }
    }

    fun setLocationShareEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefsManager.setLocationShareEnabled(enabled) }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch { userPrefsManager.setDisplayName(name) }
    }
}

// ═══════════════════════════════════════════════════════════
// BROADCAST VIEW MODEL
// ═══════════════════════════════════════════════════════════
@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val meshManager: OfflineMeshManager,
    private val messageDao: MessageDao
) : ViewModel() {

    val connectionStatus: StateFlow<ConnectionStatus> = meshManager.connectionStatus
    val connectedPeerCount: StateFlow<Int> = meshManager.connectedPeerCount

    val broadcastHistory: Flow<List<MessageEntity>> = messageDao.getBroadcastMessagesFlow()

    private val _broadcastSent = MutableStateFlow(false)
    val broadcastSent: StateFlow<Boolean> = _broadcastSent.asStateFlow()

    fun sendEmergencyBroadcast(message: String) {
        viewModelScope.launch {
            meshManager.sendBroadcast(message)
            _broadcastSent.value = true
        }
    }

    fun resetSentState() {
        _broadcastSent.value = false
    }
}

// ═══════════════════════════════════════════════════════════
// QR CODE VIEW MODEL
// ═══════════════════════════════════════════════════════════
@HiltViewModel
class QrViewModel @Inject constructor(
    private val userPrefsManager: UserPreferencesManager,
    private val contactDao: ContactDao
) : ViewModel() {

    val myDeviceId: Flow<String> = userPrefsManager.deviceIdFlow
    val displayName: Flow<String> = userPrefsManager.displayNameFlow

    // QR content = "DM:<deviceId>:<displayName>"
    val qrContent: Flow<String> = combine(myDeviceId, displayName) { id, name ->
        "DM:$id:$name"
    }

    fun addContactFromQr(qrContent: String): Boolean {
        return try {
            val parts = qrContent.split(":")
            if (parts.size >= 3 && parts[0] == "DM") {
                val deviceId = parts[1]
                val name = parts.drop(2).joinToString(":")
                viewModelScope.launch {
                    val entity = ContactEntity(
                        deviceId = deviceId,
                        displayName = name,
                        contactType = ContactType.SAVED,
                        addedAt = System.currentTimeMillis()
                    )
                    contactDao.insertOrUpdateContact(entity)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
