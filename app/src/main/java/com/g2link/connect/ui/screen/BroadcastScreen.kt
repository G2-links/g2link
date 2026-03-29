package com.g2link.connect.ui.screen

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
import com.g2link.connect.data.local.entity.MessageEntity
import com.g2link.connect.domain.model.ConnectionStatus
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.ui.viewmodel.BroadcastViewModel
import java.text.SimpleDateFormat
import java.util.*

private data class AlertTemplate(val icon: ImageVector, val label: String, val message: String, val color: Color)

private val ALERT_TEMPLATES = listOf(
    AlertTemplate(Icons.Default.Warning,            "NEED HELP",  "🆘 I NEED HELP — Please come to my location!", Color(0xFFFF1744)),
    AlertTemplate(Icons.Default.LocalHospital,      "MEDICAL",    "🏥 MEDICAL EMERGENCY — Need medical assistance urgently!", Color(0xFFFF6D00)),
    AlertTemplate(Icons.Default.WhereToVote,        "SAFE ZONE",  "✅ SAFE ZONE HERE — Area is secure, come here!", Color(0xFF00C853)),
    AlertTemplate(Icons.Default.LocalFireDepartment,"FIRE",       "🔥 FIRE ALERT — Fire detected nearby!", Color(0xFFFF3D00)),
    AlertTemplate(Icons.Default.Flood,              "FLOOD",      "🌊 FLOOD WARNING — Elevated ground needed!", Color(0xFF1565C0)),
    AlertTemplate(Icons.Default.People,             "SURVIVOR",   "👥 SURVIVOR GROUP — Multiple survivors at this location.", Color(0xFF6A0080)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(onBack: () -> Unit, viewModel: BroadcastViewModel = hiltViewModel()) {
    val peerCount by viewModel.connectedPeerCount.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val broadcastHistory by viewModel.broadcastHistory.collectAsState(initial = emptyList())
    val broadcastSent by viewModel.broadcastSent.collectAsStateWithLifecycle()
    var customMessage by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = EaseInOut), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )

    LaunchedEffect(broadcastSent) {
        if (broadcastSent) { kotlinx.coroutines.delay(3000); viewModel.resetSentState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                title = { Text("Emergency Broadcast", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A0000))
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(visible = broadcastSent, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                    Surface(color = G2Colors.Connected.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, G2Colors.Connected)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = G2Colors.Connected)
                            Column {
                                Text("Broadcast sent!", color = G2Colors.Connected, fontWeight = FontWeight.Bold)
                                Text("Relaying to $peerCount peer${if (peerCount != 1) "s" else ""}", color = Color(0xFF8BA0BF), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    color = when (connectionStatus) {
                        ConnectionStatus.CONNECTED -> G2Colors.Connected.copy(alpha = 0.1f)
                        ConnectionStatus.SEARCHING -> G2Colors.Searching.copy(alpha = 0.1f)
                        else -> Color(0xFF1E2A3A)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Hub, null, tint = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> G2Colors.Connected
                            ConnectionStatus.SEARCHING -> G2Colors.Searching
                            else -> Color(0xFF4A5568)
                        }, modifier = Modifier.size(28.dp))
                        Text(when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> "$peerCount device${if (peerCount != 1) "s" else ""} will receive this"
                            ConnectionStatus.SEARCHING -> "Searching for nearby devices..."
                            else -> "Offline — stored, will send when connected"
                        }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier.size(180.dp).scale(pulseScale)
                        .background(G2Colors.Emergency.copy(alpha = 0.15f), CircleShape)
                        .border(3.dp, G2Colors.Emergency.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.sendEmergencyBroadcast("🆘 SOS — I NEED HELP!") },
                        modifier = Modifier.size(148.dp), shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Emergency)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SOS", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("SEND ALERT", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item { Text("Quick Alerts", color = Color(0xFF8BA0BF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth()) }

            items(ALERT_TEMPLATES.chunked(2)) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { template ->
                        Surface(
                            modifier = Modifier.weight(1f).height(80.dp).clickable { viewModel.sendEmergencyBroadcast(template.message) },
                            color = template.color.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, template.color.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(template.icon, null, tint = template.color, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(template.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item {
                if (!showCustomInput) {
                    OutlinedButton(onClick = { showCustomInput = true }, modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF1E2A3A)), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFF8BA0BF))
                        Spacer(Modifier.width(8.dp))
                        Text("Custom Message", color = Color(0xFF8BA0BF))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customMessage, onValueChange = { customMessage = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Type your message...", color = Color(0xFF4A5568)) },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = G2Colors.Emergency,
                                unfocusedBorderColor = Color(0xFF1E2A3A), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { if (customMessage.isNotBlank()) { viewModel.sendEmergencyBroadcast(customMessage.trim()); customMessage = ""; showCustomInput = false } },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Emergency),
                            shape = RoundedCornerShape(12.dp), enabled = customMessage.isNotBlank()
                        ) {
                            Icon(Icons.Default.Campaign, null)
                            Spacer(Modifier.width(8.dp))
                            Text("BROADCAST NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            if (broadcastHistory.isNotEmpty()) {
                item {
                    HorizontalDivider(color = Color(0xFF1E2A3A))
                    Spacer(Modifier.height(4.dp))
                    Text("Recent Broadcasts", color = Color(0xFF8BA0BF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                }
                items(broadcastHistory.take(10), key = { it.messageId }) { msg ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (msg.isIncoming) Icons.Default.DownloadDone else Icons.Default.Campaign,
                            null, tint = if (msg.isIncoming) G2Colors.Searching else G2Colors.Emergency, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (msg.isIncoming) "From: ${msg.senderName}" else "You broadcast", color = Color(0xFF8BA0BF), fontSize = 11.sp)
                            Text(msg.content, color = Color.White, fontSize = 13.sp, maxLines = 2)
                        }
                        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp)), color = Color(0xFF4A5568), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
