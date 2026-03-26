package com.disastermesh.connect.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.disastermesh.connect.data.local.entity.ContactEntity
import com.disastermesh.connect.data.local.entity.MessageEntity
import com.disastermesh.connect.domain.model.ConnectionStatus
import com.disastermesh.connect.ui.theme.MeshColors
import com.disastermesh.connect.ui.viewmodel.ChatListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToBroadcast: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToQrShow: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val peerCount by viewModel.connectedPeerCount.collectAsStateWithLifecycle()
    val contacts by viewModel.allContacts.collectAsState(initial = emptyList())
    val latestMessages by viewModel.latestMessages.collectAsState(initial = emptyList())
    val myDeviceId by viewModel.myDeviceId.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "DisasterMesh",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        ConnectionStatusBadge(connectionStatus, peerCount)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToQrShow) {
                        Icon(Icons.Default.QrCode, contentDescription = "My QR Code", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToContacts) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Contacts", tint = Color.White)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MeshColors.Surface
                )
            )
        },
        floatingActionButton = {
            // Emergency Broadcast FAB
            FloatingActionButton(
                onClick = onNavigateToBroadcast,
                containerColor = MeshColors.Emergency,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Campaign,
                    contentDescription = "Emergency Broadcast",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = MeshColors.Background
    ) { padding ->

        if (contacts.isEmpty() && latestMessages.isEmpty()) {
            // ── Empty State ────────────────────────────────
            EmptyContactsState(
                connectionStatus = connectionStatus,
                onScanQr = { /* navigate to QR scan */ },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 88.dp) // FAB clearance
            ) {
                // ── Connected Peers Section ────────────────
                if (peerCount > 0) {
                    item {
                        NearbyPeersHeader(peerCount)
                    }
                }

                // ── Conversations ─────────────────────────
                items(
                    items = buildConversationList(contacts, latestMessages, myDeviceId),
                    key = { it.deviceId }
                ) { item ->
                    ConversationListItem(
                        item = item,
                        onClick = { onNavigateToChat(item.deviceId) }
                    )
                    HorizontalDivider(
                        color = Color(0xFF21262D),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

// ─── Data class for combined conversation items ────────────
data class ConversationItem(
    val deviceId: String,
    val displayName: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val contactType: com.disastermesh.connect.domain.model.ContactType,
    val isNearby: Boolean
)

private fun buildConversationList(
    contacts: List<ContactEntity>,
    messages: List<MessageEntity>,
    myDeviceId: String
): List<ConversationItem> {
    return contacts.map { contact ->
        val lastMsg = messages.firstOrNull { msg ->
            msg.senderId == contact.deviceId || msg.recipientId == contact.deviceId
        }
        ConversationItem(
            deviceId = contact.deviceId,
            displayName = contact.displayName,
            lastMessage = lastMsg?.content ?: "Tap to send a message",
            lastMessageTime = lastMsg?.timestamp ?: contact.lastSeenAt,
            unreadCount = 0,
            contactType = contact.contactType,
            isNearby = contact.lastSeenAt > System.currentTimeMillis() - 60_000
        )
    }.sortedByDescending { it.lastMessageTime }
}

@Composable
private fun ConversationListItem(
    item: ConversationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Avatar ─────────────────────────────────────────
        Box {
            ContactAvatar(name = item.displayName, size = 50)
            if (item.isNearby) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(MeshColors.Connected, CircleShape)
                        .border(2.dp, MeshColors.Surface, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        // ── Name + Preview ─────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.displayName,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                if (item.contactType == com.disastermesh.connect.domain.model.ContactType.FAMILY ||
                    item.contactType == com.disastermesh.connect.domain.model.ContactType.EMERGENCY) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Priority",
                        tint = MeshColors.PriorityFamily,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = item.lastMessage,
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── Time + Unread ──────────────────────────────────
        Column(horizontalAlignment = Alignment.End) {
            if (item.lastMessageTime > 0) {
                Text(
                    text = formatTime(item.lastMessageTime),
                    color = Color(0xFF6E7681),
                    fontSize = 12.sp
                )
            }
            if (item.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(20.dp)
                        .background(MeshColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBadge(status: ConnectionStatus, peerCount: Int) {
    val (color, text) = when (status) {
        ConnectionStatus.CONNECTED    -> Pair(MeshColors.Connected, "● $peerCount peer${if (peerCount != 1) "s" else ""} connected")
        ConnectionStatus.SEARCHING    -> Pair(MeshColors.Searching, "● Searching for peers...")
        ConnectionStatus.DISCONNECTED -> Pair(MeshColors.Disconnected, "● Offline")
    }
    Text(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun NearbyPeersHeader(peerCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1117))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Hub,
            contentDescription = null,
            tint = MeshColors.Connected,
            modifier = Modifier.size(16.dp)
        )
        Text(
            "$peerCount device${if (peerCount != 1) "s" else ""} in mesh range",
            color = MeshColors.Connected,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyContactsState(
    connectionStatus: ConnectionStatus,
    onScanQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Hub,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = when (connectionStatus) {
                ConnectionStatus.CONNECTED    -> MeshColors.Connected
                ConnectionStatus.SEARCHING    -> MeshColors.Searching
                ConnectionStatus.DISCONNECTED -> Color(0xFF30363D)
            }
        )
        Spacer(Modifier.height(16.dp))
        Text(
            when (connectionStatus) {
                ConnectionStatus.CONNECTED    -> "Connected to mesh"
                ConnectionStatus.SEARCHING    -> "Searching for nearby devices..."
                ConnectionStatus.DISCONNECTED -> "No mesh connection"
            },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Scan a contact's QR code\nor wait for nearby devices",
            color = Color(0xFF8B949E),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onScanQr,
            border = BorderStroke(1.dp, MeshColors.Primary)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MeshColors.Primary)
            Spacer(Modifier.width(8.dp))
            Text("Scan QR Code", color = MeshColors.Primary)
        }
    }
}

@Composable
fun ContactAvatar(name: String, size: Int) {
    val initial = name.firstOrNull()?.uppercaseChar() ?: '?'
    val color = remember(name) {
        val colors = listOf(
            Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF00695C),
            Color(0xFFE65100), Color(0xFF283593), Color(0xFF880E4F)
        )
        colors[name.hashCode().and(0x7FFFFFFF) % colors.size]
    }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.toString(),
            color = Color.White,
            fontSize = (size * 0.4f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000       -> "now"
        diff < 3600_000     -> "${diff / 60_000}m"
        diff < 86400_000    -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else                -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}
