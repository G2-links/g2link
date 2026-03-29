package com.g2link.connect.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.g2link.connect.data.local.entity.MessageEntity
import com.g2link.connect.domain.model.ConnectionStatus
import com.g2link.connect.domain.model.ContactType
import com.g2link.connect.domain.model.MessageStatus
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactDeviceId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    LaunchedEffect(contactDeviceId) { viewModel.setContact(contactDeviceId) }

    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val contactInfo by viewModel.contactInfo.collectAsState(initial = null)
    val myDeviceId by viewModel.myDeviceId.collectAsState(initial = "")
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    var showEmergencyConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ContactAvatar(name = contactInfo?.displayName ?: "?", size = 36)
                        Column {
                            Text(contactInfo?.displayName ?: "Unknown", color = Color.White,
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            when (contactInfo?.contactType) {
                                ContactType.FAMILY    -> Text("⭐ Family", fontSize = 11.sp, color = G2Colors.PriorityFamily)
                                ContactType.EMERGENCY -> Text("🚨 Emergency", fontSize = 11.sp, color = G2Colors.Emergency)
                                else -> ConnectionStatusBadge(connectionStatus, 0)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showEmergencyConfirm = true }) {
                        Icon(Icons.Default.Warning, "Emergency", tint = G2Colors.Emergency)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        bottomBar = {
            Surface(color = G2Colors.Surface, tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(
                            if (connectionStatus == ConnectionStatus.DISCONNECTED) "Offline — will retry when connected"
                            else "Message...", color = Color(0xFF4A5568), fontSize = 14.sp)
                        },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (messageText.isNotBlank()) { viewModel.sendMessage(messageText.trim()); messageText = "" }
                        }),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = G2Colors.Brand,
                            unfocusedBorderColor = Color(0xFF1E2A3A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = G2Colors.SurfaceVariant,
                            unfocusedContainerColor = G2Colors.SurfaceVariant
                        )
                    )
                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank()) { viewModel.sendMessage(messageText.trim()); messageText = "" }
                        },
                        enabled = messageText.isNotBlank(),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = G2Colors.Brand,
                            disabledContainerColor = Color(0xFF1E2A3A)
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.Black)
                    }
                }
            }
        },
        containerColor = G2Colors.Background
    ) { padding ->
        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ContactAvatar(name = contactInfo?.displayName ?: "?", size = 64)
                    Spacer(Modifier.height(12.dp))
                    Text(contactInfo?.displayName ?: "Unknown", color = Color.White,
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No messages yet.\nMessages relay through the mesh network.",
                        color = Color(0xFF8BA0BF), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    MessageBubble(message = msg, isOutgoing = msg.senderId == myDeviceId)
                }
            }
        }
    }

    if (showEmergencyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmergencyConfirm = false },
            icon = { Icon(Icons.Default.Warning, null, tint = G2Colors.Emergency) },
            title = { Text("Send Emergency Message?", color = Color.White) },
            text = {
                Column {
                    Text("Emergency messages get maximum priority and extra retries.", color = Color(0xFF8BA0BF))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type emergency message...", color = Color(0xFF4A5568)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = G2Colors.Emergency,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (messageText.isNotBlank()) { viewModel.sendMessage(messageText.trim(), isEmergency = true); messageText = "" }
                    showEmergencyConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Emergency)) {
                    Text("SEND EMERGENCY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyConfirm = false }) {
                    Text("Cancel", color = Color(0xFF8BA0BF))
                }
            },
            containerColor = G2Colors.Surface
        )
    }
}

@Composable
private fun MessageBubble(message: MessageEntity, isOutgoing: Boolean) {
    val bubbleColor = when {
        message.isEmergency -> G2Colors.Emergency.copy(alpha = 0.85f)
        isOutgoing          -> G2Colors.OutgoingBubble
        else                -> G2Colors.IncomingBubble
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isOutgoing) 48.dp else 0.dp, end = if (isOutgoing) 0.dp else 48.dp),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ),
                color = bubbleColor
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (message.isEmergency) {
                        Text("🚨 EMERGENCY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(message.content, color = Color.White, fontSize = 15.sp, lineHeight = 20.sp)
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                    color = Color(0xFF4A5568), fontSize = 11.sp
                )
                if (isOutgoing) {
                    when (message.status) {
                        MessageStatus.PENDING   -> Icon(Icons.Default.Schedule, null, tint = Color(0xFF4A5568), modifier = Modifier.size(12.dp))
                        MessageStatus.SENT      -> Icon(Icons.Default.Check, null, tint = Color(0xFF8BA0BF), modifier = Modifier.size(12.dp))
                        MessageStatus.DELIVERED -> Icon(Icons.Default.DoneAll, null, tint = G2Colors.Brand, modifier = Modifier.size(12.dp))
                        MessageStatus.FAILED    -> Icon(Icons.Default.ErrorOutline, null, tint = G2Colors.Emergency, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
    }
}
