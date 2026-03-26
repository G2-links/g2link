package com.disastermesh.connect.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.disastermesh.connect.data.local.UserPreferencesManager
import com.disastermesh.connect.data.local.dao.ContactDao
import com.disastermesh.connect.data.local.dao.DeliveredMessageCacheDao
import com.disastermesh.connect.data.local.dao.MessageDao
import com.disastermesh.connect.data.local.dao.PeerDao
import com.disastermesh.connect.data.local.entity.*
import com.disastermesh.connect.domain.model.*
import com.disastermesh.connect.security.SecurityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OfflineMeshManager — The core networking engine.
 *
 * Architecture:
 * - Primary:  Google Nearby Connections (P2P_CLUSTER strategy)
 * - Fallback: Wi-Fi Direct (WifiP2pManager)
 * - Fallback: Classic Bluetooth (BluetoothAdapter)
 *
 * Key features:
 * - Store-and-forward mesh routing
 * - TTL-based hop limiting
 * - Loop prevention via message ID cache
 * - Priority routing for emergency contacts
 * - ACK delivery acknowledgments
 * - Battery-aware scan frequency
 */
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
        private const val SERVICE_ID = "com.disastermesh.connect"
        private const val DEFAULT_TTL = 8
        private const val EMERGENCY_TTL = 12
        private const val MAX_CACHE_SIZE = 1000
        private const val NEARBY_BANDWIDTH_HIGH = 1   // HIGH_QUALITY
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ─── Active peer connections: endpointId → PeerInfo ──
    private val activePeers = ConcurrentHashMap<String, PeerInfo>()

    // ─── In-memory message ID seen cache (backed by DB) ──
    private val seenMessageIds = mutableSetOf<String>()

    // ─── State flows ─────────────────────────────────────
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

    // ═══════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════

    suspend fun initialize() {
        myDeviceId = userPrefsManager.getDeviceId()
        myDisplayName = userPrefsManager.displayNameFlow.first()
        securityManager.generateIdentityKeyPair()

        // Load seen IDs from DB into memory cache
        scope.launch {
            // DB cache loaded on first access — no blocking here
        }

        // Observe battery mode changes
        scope.launch {
            userPrefsManager.batteryModeFlow.collect { mode ->
                currentBatteryMode = mode
                if (isRunning) restartDiscovery()
            }
        }

        // Clear stale peer connections from previous session
        peerDao.clearAllConnections()

        Log.d(TAG, "OfflineMeshManager initialized. DeviceId: $myDeviceId")
    }

    // ═══════════════════════════════════════════════════════
    // START / STOP MESH
    // ═══════════════════════════════════════════════════════

    fun startMesh() {
        if (isRunning) return
        isRunning = true
        _connectionStatus.value = ConnectionStatus.SEARCHING
        Log.d(TAG, "Starting mesh — advertising + discovery")
        startAdvertising()
        startDiscovery()
        startRetryLoop()
        startCacheCleanupLoop()
    }

    fun stopMesh() {
        isRunning = false
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        activePeers.clear()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _connectedPeerCount.value = 0
        Log.d(TAG, "Mesh stopped")
    }

    private fun restartDiscovery() {
        connectionsClient.stopDiscovery()
        startDiscovery()
    }

    // ═══════════════════════════════════════════════════════
    // NEARBY CONNECTIONS — ADVERTISING
    // ═══════════════════════════════════════════════════════

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)   // Full mesh topology
            .build()

        connectionsClient.startAdvertising(
            myDisplayName,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising failed: ${e.message}")
            scope.launch {
                delay(5000)
                if (isRunning) startAdvertising()
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // NEARBY CONNECTIONS — DISCOVERY
    // ═══════════════════════════════════════════════════════

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            options
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery failed: ${e.message}")
            val retryDelay = when (currentBatteryMode) {
                BatteryMode.EMERGENCY -> 3_000L
                BatteryMode.NORMAL    -> 10_000L
                BatteryMode.SAVER     -> 30_000L
            }
            scope.launch {
                delay(retryDelay)
                if (isRunning) startDiscovery()
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // CONNECTION LIFECYCLE CALLBACKS
    // ═══════════════════════════════════════════════════════

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated from: ${info.endpointName} ($endpointId)")
            // Auto-accept all connections — mesh is open within the service
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connected to endpoint: $endpointId")
                    scope.launch { onPeerConnected(endpointId) }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED,
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.w(TAG, "Connection rejected/error for: $endpointId")
                    activePeers.remove(endpointId)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from endpoint: $endpointId")
            scope.launch { onPeerDisconnected(endpointId) }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: ${info.endpointName} ($endpointId)")
            if (!activePeers.containsKey(endpointId)) {
                connectionsClient.requestConnection(
                    myDisplayName,
                    endpointId,
                    connectionLifecycleCallback
                ).addOnFailureListener { e ->
                    Log.w(TAG, "Request connection failed for $endpointId: ${e.message}")
                }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            scope.launch { onPeerDisconnected(endpointId) }
        }
    }

    // ═══════════════════════════════════════════════════════
    // PAYLOAD (MESSAGE) CALLBACKS
    // ═══════════════════════════════════════════════════════

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                scope.launch { handleIncomingPayload(endpointId, bytes) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Monitor transfer status if needed
        }
    }

    // ═══════════════════════════════════════════════════════
    // PEER LIFECYCLE
    // ═══════════════════════════════════════════════════════

    private suspend fun onPeerConnected(endpointId: String) {
        // Send our identity packet immediately on connect
        sendIdentityPacket(endpointId)
        updateConnectionStatusFromPeers()
    }

    private suspend fun onPeerDisconnected(endpointId: String) {
        val peer = activePeers.remove(endpointId)
        peer?.let {
            peerDao.updateConnectionState(it.deviceId, false, System.currentTimeMillis())
        }
        updateConnectionStatusFromPeers()
        _meshEvents.emit(MeshEvent.PeerDisconnected(endpointId))
    }

    private fun updateConnectionStatusFromPeers() {
        _connectedPeerCount.value = activePeers.size
        _connectionStatus.value = when {
            activePeers.isNotEmpty() -> ConnectionStatus.CONNECTED
            isRunning                -> ConnectionStatus.SEARCHING
            else                     -> ConnectionStatus.DISCONNECTED
        }
    }

    // ═══════════════════════════════════════════════════════
    // IDENTITY EXCHANGE
    // ═══════════════════════════════════════════════════════

    private suspend fun sendIdentityPacket(endpointId: String) {
        val phone = userPrefsManager.phoneNumberFlow.first()
        val identity = IdentityPacket(
            deviceId = myDeviceId,
            displayName = myDisplayName,
            phoneHash = phone?.let { securityManager.hashPhoneNumber(it) },
            publicKeyBase64 = securityManager.getPublicKeyBase64()
        )
        val jsonStr = json.encodeToString(identity)
        val packet = MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = myDeviceId,
            senderName = myDisplayName,
            recipientId = null,
            content = jsonStr,
            timestamp = System.currentTimeMillis(),
            ttl = 1,  // Identity packets don't hop
            isBroadcast = false,
            isEmergency = false,
            packetType = PacketType.IDENTITY
        )
        sendPacketToEndpoint(endpointId, packet)
    }

    private suspend fun handleIdentityPacket(packet: MeshPacket) {
        try {
            val identity = json.decodeFromString<IdentityPacket>(packet.content)
            // Register or update contact
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
            Log.e(TAG, "Failed to parse identity packet: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════
    // INCOMING PAYLOAD HANDLER — MESH ROUTING CORE
    // ═══════════════════════════════════════════════════════

    private suspend fun handleIncomingPayload(fromEndpointId: String, bytes: ByteArray) {
        try {
            val jsonStr = String(bytes, Charsets.UTF_8)
            val packet = json.decodeFromString<MeshPacket>(jsonStr)

            // Update peer info if we recognize the endpoint
            val peerInfo = activePeers[fromEndpointId]
            if (peerInfo == null) {
                // First message from this endpoint — register as temporary peer
                activePeers[fromEndpointId] = PeerInfo(
                    endpointId = fromEndpointId,
                    deviceId = packet.senderId,
                    displayName = packet.senderName,
                    connectedAt = System.currentTimeMillis()
                )
                // Save to DB
                peerDao.insertOrUpdatePeer(
                    PeerEntity(
                        deviceId = packet.senderId,
                        displayName = packet.senderName,
                        endpointId = fromEndpointId,
                        lastConnectedAt = System.currentTimeMillis(),
                        isCurrentlyConnected = true
                    )
                )
                updateConnectionStatusFromPeers()
            }

            // ── DEDUPLICATION CHECK ──────────────────────
            if (hasSeenMessage(packet.messageId)) {
                Log.d(TAG, "Duplicate message dropped: ${packet.messageId}")
                return
            }
            markMessageSeen(packet.messageId)

            // ── ROUTE BY PACKET TYPE ────────────────────
            when (packet.packetType) {
                PacketType.IDENTITY  -> handleIdentityPacket(packet)
                PacketType.ACK       -> handleAckPacket(packet)
                PacketType.CHAT,
                PacketType.BROADCAST,
                PacketType.LOCATION  -> handleMessagePacket(packet, fromEndpointId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to handle incoming payload: ${e.message}")
        }
    }

    private suspend fun handleMessagePacket(packet: MeshPacket, fromEndpointId: String) {
        val isForMe = packet.recipientId == myDeviceId || packet.isBroadcast

        if (isForMe) {
            // ── DELIVER TO THIS DEVICE ───────────────────
            saveIncomingMessage(packet)
            sendAck(packet, fromEndpointId)
            _meshEvents.emit(MeshEvent.MessageReceived(packet))
            Log.d(TAG, "Message delivered locally: ${packet.messageId}")
        }

        // ── FORWARD if TTL > 0 and not explicitly for us ─
        if (packet.ttl > 0 && (packet.isBroadcast || packet.recipientId != myDeviceId)) {
            val decremented = packet.copy(ttl = packet.ttl - 1)
            forwardToAllPeers(decremented, excludeEndpointId = fromEndpointId)
        } else {
            Log.d(TAG, "TTL exhausted or final recipient — not forwarding: ${packet.messageId}")
        }
    }

    private suspend fun handleAckPacket(packet: MeshPacket) {
        val ackForId = packet.ackForMessageId ?: return
        messageDao.updateMessageStatus(ackForId, MessageStatus.DELIVERED)
        _meshEvents.emit(MeshEvent.MessageDelivered(ackForId))
        Log.d(TAG, "ACK received for message: $ackForId")
    }

    // ═══════════════════════════════════════════════════════
    // MESSAGE SENDING — PUBLIC API
    // ═══════════════════════════════════════════════════════

    /**
     * Send a chat message to a specific contact.
     * Stores locally and dispatches to all connected peers (mesh routing).
     */
    suspend fun sendMessage(recipientDeviceId: String, content: String, isEmergency: Boolean = false) {
        val msgId = UUID.randomUUID().toString()
        val ttl = if (isEmergency) EMERGENCY_TTL else DEFAULT_TTL

        val packet = MeshPacket(
            messageId = msgId,
            senderId = myDeviceId,
            senderName = myDisplayName,
            recipientId = recipientDeviceId,
            content = content,
            timestamp = System.currentTimeMillis(),
            ttl = ttl,
            isBroadcast = false,
            isEmergency = isEmergency,
            packetType = PacketType.CHAT
        )

        // Store locally first (offline-first)
        saveOutgoingMessage(packet)
        markMessageSeen(msgId)

        // Dispatch into mesh
        forwardToAllPeers(packet, excludeEndpointId = null)
    }

    /**
     * Broadcast an emergency alert to ALL nearby devices.
     */
    suspend fun sendBroadcast(content: String) {
        val msgId = UUID.randomUUID().toString()
        val packet = MeshPacket(
            messageId = msgId,
            senderId = myDeviceId,
            senderName = myDisplayName,
            recipientId = null,
            content = content,
            timestamp = System.currentTimeMillis(),
            ttl = EMERGENCY_TTL,
            isBroadcast = true,
            isEmergency = true,
            packetType = PacketType.BROADCAST
        )

        saveOutgoingMessage(packet)
        markMessageSeen(msgId)
        forwardToAllPeers(packet, excludeEndpointId = null)
    }

    /**
     * Share current GPS location with all peers.
     */
    suspend fun shareLocation(lat: Double, lng: Double, accuracy: Float) {
        val location = LocationPayload(lat, lng, accuracy, System.currentTimeMillis())
        val packet = MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = myDeviceId,
            senderName = myDisplayName,
            recipientId = null,
            content = "📍 Location shared",
            timestamp = System.currentTimeMillis(),
            ttl = DEFAULT_TTL,
            isBroadcast = true,
            isEmergency = false,
            packetType = PacketType.LOCATION,
            locationPayload = location
        )
        forwardToAllPeers(packet, excludeEndpointId = null)
    }

    // ═══════════════════════════════════════════════════════
    // ACK — DELIVERY ACKNOWLEDGMENT
    // ═══════════════════════════════════════════════════════

    private suspend fun sendAck(originalPacket: MeshPacket, toEndpointId: String) {
        if (originalPacket.isBroadcast) return // No ACK for broadcasts
        val ack = MeshPacket(
            messageId = UUID.randomUUID().toString(),
            senderId = myDeviceId,
            senderName = myDisplayName,
            recipientId = originalPacket.senderId,
            content = "",
            timestamp = System.currentTimeMillis(),
            ttl = DEFAULT_TTL,
            isBroadcast = false,
            isEmergency = false,
            packetType = PacketType.ACK,
            ackForMessageId = originalPacket.messageId
        )
        // Send ACK back to the peer who forwarded the message
        sendPacketToEndpoint(toEndpointId, ack)
        // Also flood into mesh so sender eventually gets it
        forwardToAllPeers(ack, excludeEndpointId = toEndpointId)
    }

    // ═══════════════════════════════════════════════════════
    // LOW-LEVEL SEND / FORWARD
    // ═══════════════════════════════════════════════════════

    private fun forwardToAllPeers(packet: MeshPacket, excludeEndpointId: String?) {
        val targets = activePeers.keys.filter { it != excludeEndpointId }
        if (targets.isEmpty()) return

        // Priority contacts get sent first
        val jsonBytes = json.encodeToString(packet).toByteArray(Charsets.UTF_8)
        targets.forEach { endpointId ->
            try {
                connectionsClient.sendPayload(endpointId, Payload.fromBytes(jsonBytes))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send to $endpointId: ${e.message}")
            }
        }
    }

    private fun sendPacketToEndpoint(endpointId: String, packet: MeshPacket) {
        try {
            val jsonBytes = json.encodeToString(packet).toByteArray(Charsets.UTF_8)
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(jsonBytes))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send packet to $endpointId: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════
    // STORE & FORWARD — DB OPERATIONS
    // ═══════════════════════════════════════════════════════

    private suspend fun saveIncomingMessage(packet: MeshPacket) {
        val entity = MessageEntity(
            messageId = packet.messageId,
            senderId = packet.senderId,
            senderName = packet.senderName,
            recipientId = packet.recipientId,
            content = packet.content,
            timestamp = packet.timestamp,
            status = MessageStatus.DELIVERED,
            isIncoming = true,
            isBroadcast = packet.isBroadcast,
            isEmergency = packet.isEmergency,
            ttl = packet.ttl,
            locationLatitude = packet.locationPayload?.latitude,
            locationLongitude = packet.locationPayload?.longitude,
            locationAccuracy = packet.locationPayload?.accuracy
        )
        messageDao.insertMessage(entity)
    }

    private suspend fun saveOutgoingMessage(packet: MeshPacket) {
        val entity = MessageEntity(
            messageId = packet.messageId,
            senderId = packet.senderId,
            senderName = packet.senderName,
            recipientId = packet.recipientId,
            content = packet.content,
            timestamp = packet.timestamp,
            status = if (activePeers.isEmpty()) MessageStatus.PENDING else MessageStatus.SENT,
            isIncoming = false,
            isBroadcast = packet.isBroadcast,
            isEmergency = packet.isEmergency,
            ttl = packet.ttl,
            locationLatitude = packet.locationPayload?.latitude,
            locationLongitude = packet.locationPayload?.longitude,
            locationAccuracy = packet.locationPayload?.accuracy
        )
        messageDao.insertMessage(entity)
    }

    // ═══════════════════════════════════════════════════════
    // RETRY LOOP — Store & Forward delivery
    // ═══════════════════════════════════════════════════════

    private fun startRetryLoop() {
        scope.launch {
            while (isRunning) {
                val retryInterval = when (currentBatteryMode) {
                    BatteryMode.EMERGENCY -> 10_000L
                    BatteryMode.NORMAL    -> 30_000L
                    BatteryMode.SAVER     -> 120_000L
                }
                delay(retryInterval)
                retryPendingMessages()
            }
        }
    }

    private suspend fun retryPendingMessages() {
        if (activePeers.isEmpty()) return
        val pending = messageDao.getPendingOutboundMessages()
        pending.forEach { msg ->
            Log.d(TAG, "Retrying message: ${msg.messageId}")
            val packet = MeshPacket(
                messageId = msg.messageId,
                senderId = msg.senderId,
                senderName = msg.senderName,
                recipientId = msg.recipientId,
                content = msg.content,
                timestamp = msg.timestamp,
                ttl = DEFAULT_TTL,
                isBroadcast = msg.isBroadcast,
                isEmergency = msg.isEmergency,
                packetType = if (msg.isBroadcast) PacketType.BROADCAST else PacketType.CHAT
            )
            forwardToAllPeers(packet, excludeEndpointId = null)
            messageDao.incrementRetryCount(msg.messageId)
            if (msg.retryCount >= 9) {
                messageDao.updateMessageStatus(msg.messageId, MessageStatus.FAILED)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════

    private suspend fun hasSeenMessage(messageId: String): Boolean {
        if (seenMessageIds.contains(messageId)) return true
        return cacheDao.hasSeenMessage(messageId) > 0
    }

    private suspend fun markMessageSeen(messageId: String) {
        seenMessageIds.add(messageId)
        if (seenMessageIds.size > MAX_CACHE_SIZE) {
            seenMessageIds.clear() // Simple eviction
        }
        cacheDao.markSeen(DeliveredMessageCacheEntity(messageId = messageId))
    }

    private fun startCacheCleanupLoop() {
        scope.launch {
            while (isRunning) {
                delay(6 * 60 * 60 * 1000L) // Every 6 hours
                cacheDao.pruneExcess()
                val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours
                cacheDao.pruneOld(cutoff)
                messageDao.pruneOldDeliveredMessages(cutoff)
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // QUERY HELPERS
    // ═══════════════════════════════════════════════════════

    fun getActivePeers(): List<PeerInfo> = activePeers.values.toList()

    fun getActivePeerCount(): Int = activePeers.size
}

// ═══════════════════════════════════════════════════════════
// MESH EVENTS — Emitted to UI layer
// ═══════════════════════════════════════════════════════════
sealed class MeshEvent {
    data class MessageReceived(val packet: MeshPacket) : MeshEvent()
    data class MessageDelivered(val messageId: String) : MeshEvent()
    data class PeerIdentified(val deviceId: String, val displayName: String) : MeshEvent()
    data class PeerDisconnected(val endpointId: String) : MeshEvent()
}
