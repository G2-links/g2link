package com.disastermesh.connect.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.disastermesh.connect.data.local.entity.MessageEntity
import com.disastermesh.connect.domain.model.ConnectionStatus
import com.disastermesh.connect.ui.theme.MeshColors
import com.disastermesh.connect.ui.viewmodel.BroadcastViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── Preset emergency messages ────────────────────────────
private data class AlertTemplate(
    val icon: ImageVector,
    val label: String,
    val message: String,
    val color: Color
)

private val ALERT_TEMPLATES = listOf(
    AlertTemplate(Icons.Default.Warning,       "NEED HELP",    "🆘 I NEED HELP — Please come to my location!", Color(0xFFFF1744)),
    AlertTemplate(Icons.Default.LocalHospital, "MEDICAL",      "🏥 MEDICAL EMERGENCY — Need medical assistance urgently!", Color(0xFFFF6D00)),
    AlertTemplate(Icons.Default.WhereToVote,   "SAFE ZONE",    "✅ SAFE ZONE HERE — Area is secure, come here!", Color(0xFF00C853)),
    AlertTemplate(Icons.Default.LocalFireDepartment, "FIRE",   "🔥 FIRE ALERT — Fire detected nearby!", Color(0xFFFF3D00)),
    AlertTemplate(Icons.Default.Flood,         "FLOOD",        "🌊 FLOOD WARNING — Elevated ground needed!", Color(0xFF1565C0)),
    AlertTemplate(Icons.Default.People,        "SURVIVOR",     "👥 SURVIVOR GROUP — Multiple survivors at this location.", Color(0xFF6A1B9A)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    onBack: () -> Unit,
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val peerCount by viewModel.connectedPeerCount.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val broadcastHistory by viewModel.broadcastHistory.collectAsState(initial = emptyList())
    val broadcastSent by viewModel.broadcastSent.collectAsStateWithLifecycle()

    var customMessage by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var lastSentMessage by remember { mutableStateOf("") }

    // Pulse animation for the main SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LaunchedEffect(broadcastSent) {
        if (broadcastSent) {
            kotlinx.coroutines.delay(3000)
            viewModel.resetSentState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = {
                    Text(
                        "Emergency Broadcast",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A0000))
            )
        },
        containerColor = MeshColors.Background
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Sent Confirmation Banner ──────────────────
            item {
                AnimatedVisibility(
                    visible = broadcastSent,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Surface(
                        color = MeshColors.Connected.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MeshColors.Connected)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MeshColors.Connected)
                            Column {
                                Text("Broadcast sent!", color = MeshColors.Connected, fontWeight = FontWeight.Bold)
                                Text(
                                    "Relaying to $peerCount peer${if (peerCount != 1) "s" else ""}",
                                    color = Color(0xFF8B949E),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── Coverage Status Card ──────────────────────
            item {
                Surface(
                    color = when (connectionStatus) {
                        ConnectionStatus.CONNECTED    -> MeshColors.Connected.copy(alpha = 0.1f)
                        ConnectionStatus.SEARCHING    -> MeshColors.Searching.copy(alpha = 0.1f)
                        ConnectionStatus.DISCONNECTED -> Color(0xFF30363D).copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, when (connectionStatus) {
                        ConnectionStatus.CONNECTED    -> MeshColors.Connected.copy(alpha = 0.4f)
                        ConnectionStatus.SEARCHING    -> MeshColors.Searching.copy(alpha = 0.4f)
                        ConnectionStatus.DISCONNECTED -> Color(0xFF30363D)
                    })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            null,
                            tint = when (connectionStatus) {
                                ConnectionStatus.CONNECTED    -> MeshColors.Connected
                                ConnectionStatus.SEARCHING    -> MeshColors.Searching
                                ConnectionStatus.DISCONNECTED -> Color(0xFF6E7681)
                            },
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED    -> "$peerCount device${if (peerCount != 1) "s" else ""} will receive this broadcast"
                                    ConnectionStatus.SEARCHING    -> "Searching for nearby devices..."
                                    ConnectionStatus.DISCONNECTED -> "No peers — message will be stored and sent when connection is found"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Messages relay hop-by-hop up to 8 devices away",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Main SOS Button ───────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(pulseScale)
                        .background(
                            MeshColors.Emergency.copy(alpha = 0.15f),
                            CircleShape
                        )
                        .border(3.dp, MeshColors.Emergency.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            viewModel.sendEmergencyBroadcast("🆘 SOS — I NEED HELP!")
                            lastSentMessage = "🆘 SOS — I NEED HELP!"
                        },
                        modifier = Modifier.size(148.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeshColors.Emergency
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "SOS",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "SEND ALERT",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ── Alert Template Grid ───────────────────────
            item {
                Text(
                    "Quick Alerts",
                    color = Color(0xFF8B949E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2-column grid of templates
            items(ALERT_TEMPLATES.chunked(2)) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { template ->
                        AlertTemplateButton(
                            template = template,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.sendEmergencyBroadcast(template.message)
                                lastSentMessage = template.message
                            }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // ── Custom Message Input ───────────────────────
            item {
                if (!showCustomInput) {
                    OutlinedButton(
                        onClick = { showCustomInput = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF30363D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFF8B949E))
                        Spacer(Modifier.width(8.dp))
                        Text("Custom Message", color = Color(0xFF8B949E))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customMessage,
                            onValueChange = { customMessage = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type your message...", color = Color(0xFF8B949E)) },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MeshColors.Emergency,
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = {
                                if (customMessage.isNotBlank()) {
                                    viewModel.sendEmergencyBroadcast(customMessage.trim())
                                    lastSentMessage = customMessage
                                    customMessage = ""
                                    showCustomInput = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MeshColors.Emergency),
                            shape = RoundedCornerShape(12.dp),
                            enabled = customMessage.isNotBlank()
                        ) {
                            Icon(Icons.Default.Campaign, null)
                            Spacer(Modifier.width(8.dp))
                            Text("BROADCAST NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            // ── Broadcast History ─────────────────────────
            if (broadcastHistory.isNotEmpty()) {
                item {
                    HorizontalDivider(color = Color(0xFF21262D))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Recent Broadcasts",
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(broadcastHistory.take(10), key = { it.messageId }) { msg ->
                    BroadcastHistoryItem(msg)
                }
            }
        }
    }
}

@Composable
private fun AlertTemplateButton(
    template: AlertTemplate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        color = template.color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, template.color.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(template.icon, null, tint = template.color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                template.label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BroadcastHistoryItem(message: MessageEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (message.isIncoming) Icons.Default.DownloadDone else Icons.Default.Campaign,
            null,
            tint = if (message.isIncoming) MeshColors.Searching else MeshColors.Emergency,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (message.isIncoming) "From: ${message.senderName}" else "You broadcast",
                color = Color(0xFF8B949E),
                fontSize = 11.sp
            )
            Text(message.content, color = Color.White, fontSize = 13.sp, maxLines = 2)
        }
        Text(
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
            color = Color(0xFF6E7681),
            fontSize = 11.sp
        )
    }
}
