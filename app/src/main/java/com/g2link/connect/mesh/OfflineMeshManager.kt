package com.g2link.connect.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.g2link.connect.data.local.UserPreferencesManager
import com.g2link.connect.data.local.dao.ContactDao
import com.g2link.connect.data.local.dao.DeliveredMessageCacheDao
import com.g2link.connect.data.local.dao.MessageDao
import com.g2link.connect.data.local.dao.PeerDao
import com.g2link.connect.data.local.entity.*
import com.g2link.connect.domain.model.*
import com.g2link.connect.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMeshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val peerDao: PeerDao,
    private val cacheDao: DeliveredMessageCacheDao,
    private val securityManager: SecurityManager,
    private val userPrefsManager: UserPreferencesManager
) {
    companion object {
        private const val TAG = "OfflineMeshManager"
        private const val SERVICE_ID = "com.g2link.connect"
        private const val DEFAULT_TTL = 8
        private const val EMERGENCY_TTL = 12
        private const val MAX_CACHE_SIZE = 1000
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val activePeers = ConcurrentHashMap<String, PeerInfo>()
    private val seenMessageIds = mutableSetOf<String>()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectedPeerCount = MutableStateFlow(0)
    val connectedPeerCount: StateFlow<Int> = _connectedPeerCount.asStateFlow()

    private val _meshEvents = MutableSharedFlow<MeshEvent>(replay = 0, extraBufferCapacity = 64)
    val meshEvents: SharedFlow<MeshEvent> = _meshEvents.asSharedFlow()

    private var myDeviceId: String = ""
    private var myDisplayName: String = ""
    private var isRunning = false
    private var currentBatteryMode = BatteryMode.NORMAL

    suspend fun initialize() {
        myDeviceId = userPrefsManager.getDeviceId()
        myDisplayName = userPrefsManager.displayNameFlow.first()
        securityManager.generateIdentityKeyPair()
        scope.launch {
            userPrefsManager.batteryModeFlow.collect { mode ->
                currentBatteryMode = mode
                if (isRunning) restartDiscovery()
            }
        }
        peerDao.clearAllConnections()
        Log.d(TAG, "Initialized. DeviceId: $myDeviceId")
    }

    fun startMesh() {
        if (isRunning) return
        isRunning = true
        _connectionStatus.value = ConnectionStatus.SEARCHING
        startAdvertising()
        startDiscovery()
        startRetryLoop()
    }

    fun stopMesh() {
        isRunning = false
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        activePeers.clear()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectedPeerCount.value = 0
    }

    private fun restartDiscovery() {
        connectionsClient.stopDiscovery()
        startDiscovery()
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(myDisplayName, SERVICE_ID, connectionLifecycleCallback, options)
            .addOnFailureListener { e ->
                Log.e(TAG, "Advertising failed: ${e.message}")
                scope.launch { delay(5000); if (isRunning) startAdvertising() }
            }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnFailureListener { e ->
                Log.e(TAG, "Discovery failed: ${e.message}")
                val retryDelay = when (currentBatteryMode) {
                    BatteryMode.EMERGENCY -> 3_000L
                    BatteryMode.NORMAL -> 10_000L
                    BatteryMode.SAVER -> 30_000L
                }
                scope.launch { delay(retryDelay); if (isRunning) startDiscovery() }
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                scope.launch { onPeerConnected(endpointId) }
            } else {
                activePeers.remove(endpointId)
            }
        }
        override fun onDisconnected(endpointId: String) {
            scope.launch { onPeerDisconnected(endpointId) }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (!activePeers.containsKey(endpointId)) {
                connectionsClient.requestConnection(myDisplayName, endpointId, connectionLifecycleCallback)
            }
        }
        override fun onEndpointLost(endpointId: String) {
            scope.launch { onPeerDisconnected(endpointId) }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                scope.launch { handleIncomingPayload(endpointId, bytes) }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private suspend fun onPeerConnected(endpointId: String) {
        sendIdentityPacket(endpointId)
        updateConnectionStatusFromPeers()
    }

    private suspend fun onPeerDisconnected(endpointId: String) {
        val peer = activePeers.remove(endpointId)
        peer?.let { peerDao.updateConnectionState(it.deviceId, false, System.currentTimeMillis()) }
        updateConnectionStatusFromPeers()
        _meshEvents.emit(MeshEvent.PeerDisconnected(endpointId))
    }

    private fun updateConnectionStatusFromPeers() {
        _connectedPeerCount.value = activePeers.size
        _connectionStatus.value = when {
            activePeers.isNotEmpty() -> ConnectionStatus.CONNECTED
            isRunning -> ConnectionStatus.SEARCHING
            else -> ConnectionStatus.DISCONNECTED
        }
    }

    private suspend fun sendIdentityPacket(endpointId: String) {
        val phone = userPrefsManager.phoneNumberFlow.first()
        val identity = IdentityPacket(
            deviceId = myDeviceId,
            displayName = myDisplayName,
            phoneHash = phone?.let { securityManager.hashPhoneNumber(it) },
            publicKeyBase64 = securityManager.getPublicKeyBase64()
        )
        val packet = MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = myDeviceId, senderName = myDisplayName,
            recipientId = null, content = json.encodeToString(identity),
            timestamp = System.currentTimeMillis(), ttl = 1,
            isBroadcast = false, isEmergency = false, packetType = PacketType.IDENTITY
        )
        sendPacketToEndpoint(endpointId, packet)
    }

    private suspend fun handleIdentityPacket(packet: MeshPacket) {
        try {
            val identity = json.decodeFromString<IdentityPacket>(packet.content)
            val existing = contactDao.getContactByDeviceId(identity.deviceId)
            val contact = ContactEntity(
                id = existing?.id ?: 0,
                deviceId = identity.deviceId,
                displayName = identity.displayName,
                publicKeyBase64 = identity.publicKeyBase64,
                contactType = existing?.contactType ?: ContactType.UNKNOWN,
                lastSeenAt = System.currentTimeMillis(),
                addedAt = existing?.addedAt ?: System.currentTimeMillis()
            )
            contactDao.insertOrUpdateContact(contact)
            _meshEvents.emit(MeshEvent.PeerIdentified(identity.deviceId, identity.displayName))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse identity: ${e.message}")
        }
    }

    private suspend fun handleIncomingPayload(fromEndpointId: String, bytes: ByteArray) {
        try {
            val packet = json.decodeFromString<MeshPacket>(String(bytes, Charsets.UTF_8))

            if (activePeers[fromEndpointId] == null) {
                activePeers[fromEndpointId] = PeerInfo(
                    endpointId = fromEndpointId, deviceId = packet.senderId,
                    displayName = packet.senderName, connectedAt = System.currentTimeMillis()
                )
                peerDao.insertOrUpdatePeer(PeerEntity(
                    deviceId = packet.senderId, displayName = packet.senderName,
                    endpointId = fromEndpointId, lastConnectedAt = System.currentTimeMillis(),
                    isCurrentlyConnected = true
                ))
                updateConnectionStatusFromPeers()
            }

            if (hasSeenMessage(packet.messageId)) return
            markMessageSeen(packet.messageId)

            when (packet.packetType) {
                PacketType.IDENTITY -> handleIdentityPacket(packet)
                PacketType.ACK -> handleAckPacket(packet)
                else -> handleMessagePacket(packet, fromEndpointId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle payload: ${e.message}")
        }
    }

    private suspend fun handleMessagePacket(packet: MeshPacket, fromEndpointId: String) {
        val isForMe = packet.recipientId == myDeviceId || packet.isBroadcast
        if (isForMe) {
            saveIncomingMessage(packet)
            sendAck(packet, fromEndpointId)
            _meshEvents.emit(MeshEvent.MessageReceived(packet))
        }
        if (packet.ttl > 0 && (packet.isBroadcast || packet.recipientId != myDeviceId)) {
            forwardToAllPeers(packet.copy(ttl = packet.ttl - 1), excludeEndpointId = fromEndpointId)
        }
    }

    private suspend fun handleAckPacket(packet: MeshPacket) {
        val ackForId = packet.ackForMessageId ?: return
        messageDao.updateMessageStatus(ackForId, MessageStatus.DELIVERED)
        _meshEvents.emit(MeshEvent.MessageDelivered(ackForId))
    }

    suspend fun sendMessage(recipientDeviceId: String, content: String, isEmergency: Boolean = false) {
        val msgId = UUID.randomUUID().toString()
        val packet = MeshPacket(
            messageId = msgId, senderId = myDeviceId, senderName = myDisplayName,
            recipientId = recipientDeviceId, content = content,
            timestamp = System.currentTimeMillis(),
            ttl = if (isEmergency) EMERGENCY_TTL else DEFAULT_TTL,
            isBroadcast = false, isEmergency = isEmergency, packetType = PacketType.CHAT
        )
        saveOutgoingMessage(packet)
        markMessageSeen(msgId)
        forwardToAllPeers(packet, excludeEndpointId = null)
    }

    suspend fun sendBroadcast(content: String) {
        val msgId = UUID.randomUUID().toString()
        val packet = MeshPacket(
            messageId = msgId, senderId = myDeviceId, senderName = myDisplayName,
            recipientId = null, content = content,
            timestamp = System.currentTimeMillis(), ttl = EMERGENCY_TTL,
            isBroadcast = true, isEmergency = true, packetType = PacketType.BROADCAST
        )
        saveOutgoingMessage(packet)
        markMessageSeen(msgId)
        forwardToAllPeers(packet, excludeEndpointId = null)
    }

    suspend fun shareLocation(lat: Double, lng: Double, accuracy: Float) {
        val location = LocationPayload(lat, lng, accuracy, System.currentTimeMillis())
        val packet = MeshPacket(
            messageId = UUID.randomUUID().toString(), senderId = myDeviceId, senderName = myDisplayName,
            recipientId = null, content = "📍 Location shared",
            timestamp = System.currentTimeMillis(), ttl = DEFAULT_TTL,
            isBroadcast = true, isEmergency = false, packetType = PacketType.LOCATION,
            locationPayload = location
        )
        forwardToAllPeers(packet, excludeEndpointId = null)
    }

    private suspend fun sendAck(originalPacket: MeshPacket, toEndpointId: String) {
        if (originalPacket.isBroadcast) return
        val ack = MeshPacket(
            messageId = UUID.randomUUID().toString(), senderId = myDeviceId, senderName = myDisplayName,
            recipientId = originalPacket.senderId, content = "",
            timestamp = System.currentTimeMillis(), ttl = DEFAULT_TTL,
            isBroadcast = false, isEmergency = false, packetType = PacketType.ACK,
            ackForMessageId = originalPacket.messageId
        )
        sendPacketToEndpoint(toEndpointId, ack)
        forwardToAllPeers(ack, excludeEndpointId = toEndpointId)
    }

    private fun forwardToAllPeers(packet: MeshPacket, excludeEndpointId: String?) {
        val targets = activePeers.keys.filter { it != excludeEndpointId }
        if (targets.isEmpty()) return
        val jsonBytes = json.encodeToString(packet).toByteArray(Charsets.UTF_8)
        targets.forEach { endpointId ->
            try { connectionsClient.sendPayload(endpointId, Payload.fromBytes(jsonBytes)) }
            catch (e: Exception) { Log.w(TAG, "Send failed to $endpointId: ${e.message}") }
        }
    }

    private fun sendPacketToEndpoint(endpointId: String, packet: MeshPacket) {
        try {
            val jsonBytes = json.encodeToString(packet).toByteArray(Charsets.UTF_8)
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(jsonBytes))
        } catch (e: Exception) { Log.w(TAG, "Send failed: ${e.message}") }
    }

    private suspend fun saveIncomingMessage(packet: MeshPacket) {
        messageDao.insertMessage(MessageEntity(
            messageId = packet.messageId, senderId = packet.senderId, senderName = packet.senderName,
            recipientId = packet.recipientId, content = packet.content, timestamp = packet.timestamp,
            status = MessageStatus.DELIVERED, isIncoming = true, isBroadcast = packet.isBroadcast,
            isEmergency = packet.isEmergency, ttl = packet.ttl,
            locationLatitude = packet.locationPayload?.latitude,
            locationLongitude = packet.locationPayload?.longitude,
            locationAccuracy = packet.locationPayload?.accuracy
        ))
    }

    private suspend fun saveOutgoingMessage(packet: MeshPacket) {
        messageDao.insertMessage(MessageEntity(
            messageId = packet.messageId, senderId = packet.senderId, senderName = packet.senderName,
            recipientId = packet.recipientId, content = packet.content, timestamp = packet.timestamp,
            status = if (activePeers.isEmpty()) MessageStatus.PENDING else MessageStatus.SENT,
            isIncoming = false, isBroadcast = packet.isBroadcast,
            isEmergency = packet.isEmergency, ttl = packet.ttl
        ))
    }

    private fun startRetryLoop() {
        scope.launch {
            while (isRunning) {
                val retryInterval = when (currentBatteryMode) {
                    BatteryMode.EMERGENCY -> 10_000L
                    BatteryMode.NORMAL -> 30_000L
                    BatteryMode.SAVER -> 120_000L
                }
                delay(retryInterval)
                if (activePeers.isEmpty()) continue
                val pending = messageDao.getPendingOutboundMessages()
                pending.forEach { msg ->
                    val packet = MeshPacket(
                        messageId = msg.messageId, senderId = msg.senderId, senderName = msg.senderName,
                        recipientId = msg.recipientId, content = msg.content, timestamp = msg.timestamp,
                        ttl = DEFAULT_TTL, isBroadcast = msg.isBroadcast, isEmergency = msg.isEmergency,
                        packetType = if (msg.isBroadcast) PacketType.BROADCAST else PacketType.CHAT
                    )
                    forwardToAllPeers(packet, excludeEndpointId = null)
                    messageDao.incrementRetryCount(msg.messageId)
                    if (msg.retryCount >= 9) messageDao.updateMessageStatus(msg.messageId, MessageStatus.FAILED)
                }
            }
        }
    }

    private suspend fun hasSeenMessage(messageId: String): Boolean {
        if (seenMessageIds.contains(messageId)) return true
        return cacheDao.hasSeenMessage(messageId) > 0
    }

    private suspend fun markMessageSeen(messageId: String) {
        seenMessageIds.add(messageId)
        if (seenMessageIds.size > MAX_CACHE_SIZE) seenMessageIds.clear()
        cacheDao.markSeen(DeliveredMessageCacheEntity(messageId = messageId))
    }

    fun getActivePeers(): List<PeerInfo> = activePeers.values.toList()
    fun getActivePeerCount(): Int = activePeers.size
}

sealed class MeshEvent {
    data class MessageReceived(val packet: MeshPacket) : MeshEvent()
    data class MessageDelivered(val messageId: String) : MeshEvent()
    data class PeerIdentified(val deviceId: String, val displayName: String) : MeshEvent()
    data class PeerDisconnected(val endpointId: String) : MeshEvent()
}
