package com.disastermesh.connect.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.disastermesh.connect.domain.model.BatteryMode
import com.disastermesh.connect.domain.model.ConnectionStatus
import com.disastermesh.connect.ui.theme.MeshColors
import com.disastermesh.connect.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToQrShow: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val displayName by viewModel.displayName.collectAsState(initial = "")
    val myDeviceId by viewModel.myDeviceId.collectAsState(initial = "")
    val batteryMode by viewModel.batteryMode.collectAsState(initial = BatteryMode.NORMAL)
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
    val locationShareEnabled by viewModel.locationShareEnabled.collectAsState(initial = false)
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val peerCount by viewModel.connectedPeerCount.collectAsStateWithLifecycle()

    var showEditName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(displayName) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MeshColors.Surface)
            )
        },
        containerColor = MeshColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── IDENTITY SECTION ──────────────────────────
            SettingsSection(title = "IDENTITY") {
                // Profile card
                Surface(
                    color = MeshColors.SurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ContactAvatar(name = displayName.ifBlank { "?" }, size = 52)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                displayName.ifBlank { "No name set" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                myDeviceId.take(20) + "...",
                                color = Color(0xFF6E7681),
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = {
                            nameInput = displayName
                            showEditName = true
                        }) {
                            Icon(Icons.Default.Edit, "Edit name", tint = MeshColors.Primary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // QR actions
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToQrShow,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MeshColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCode, null, tint = MeshColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("My QR Code", color = MeshColors.Primary, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onNavigateToQrScan,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MeshColors.Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = MeshColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scan QR", color = MeshColors.Primary, fontSize = 13.sp)
                    }
                }
            }

            // ── MESH STATUS SECTION ───────────────────────
            SettingsSection(title = "MESH STATUS") {
                Surface(
                    color = MeshColors.SurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
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
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED    -> "Connected"
                                    ConnectionStatus.SEARCHING    -> "Searching"
                                    ConnectionStatus.DISCONNECTED -> "Offline"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when (connectionStatus) {
                                    ConnectionStatus.CONNECTED    -> "$peerCount peer${if (peerCount != 1) "s" else ""} in mesh range"
                                    ConnectionStatus.SEARCHING    -> "Scanning for nearby devices..."
                                    ConnectionStatus.DISCONNECTED -> "No nearby devices found"
                                },
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── BATTERY MODE SECTION ──────────────────────
            SettingsSection(title = "BATTERY MODE") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BatteryModeCard(
                        mode = BatteryMode.EMERGENCY,
                        title = "⚡ Emergency Mode",
                        description = "High scan frequency — Maximum connectivity",
                        isSelected = batteryMode == BatteryMode.EMERGENCY,
                        color = MeshColors.Emergency,
                        onClick = { viewModel.setBatteryMode(BatteryMode.EMERGENCY) }
                    )
                    BatteryModeCard(
                        mode = BatteryMode.NORMAL,
                        title = "🔵 Normal Mode",
                        description = "Balanced scanning — Default",
                        isSelected = batteryMode == BatteryMode.NORMAL,
                        color = MeshColors.Primary,
                        onClick = { viewModel.setBatteryMode(BatteryMode.NORMAL) }
                    )
                    BatteryModeCard(
                        mode = BatteryMode.SAVER,
                        title = "🔋 Battery Saver",
                        description = "Reduced scanning — Preserves battery",
                        isSelected = batteryMode == BatteryMode.SAVER,
                        color = MeshColors.Connected,
                        onClick = { viewModel.setBatteryMode(BatteryMode.SAVER) }
                    )
                }
            }

            // ── PREFERENCES SECTION ───────────────────────
            SettingsSection(title = "PREFERENCES") {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Show alerts for incoming messages",
                    checked = notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                HorizontalDivider(color = Color(0xFF21262D), modifier = Modifier.padding(start = 56.dp))
                SettingsToggleRow(
                    icon = Icons.Default.LocationOn,
                    title = "Auto-share Location",
                    subtitle = "Include GPS in broadcasts",
                    checked = locationShareEnabled,
                    onCheckedChange = { viewModel.setLocationShareEnabled(it) }
                )
            }

            // ── ABOUT SECTION ─────────────────────────────
            SettingsSection(title = "ABOUT") {
                SettingsInfoRow(Icons.Default.Security,  "Encryption", "AES-256-GCM, keys generated locally")
                HorizontalDivider(color = Color(0xFF21262D), modifier = Modifier.padding(start = 56.dp))
                SettingsInfoRow(Icons.Default.Hub,       "Protocol",   "Google Nearby Connections P2P_CLUSTER")
                HorizontalDivider(color = Color(0xFF21262D), modifier = Modifier.padding(start = 56.dp))
                SettingsInfoRow(Icons.Default.Storage,   "Database",   "Encrypted Room DB — local only")
                HorizontalDivider(color = Color(0xFF21262D), modifier = Modifier.padding(start = 56.dp))
                SettingsInfoRow(Icons.Default.PhoneAndroid, "Version", "1.0.0 — DisasterMesh")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Edit Name Dialog ──────────────────────────────────
    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Edit Display Name", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeshColors.Primary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (nameInput.isNotBlank()) {
                        viewModel.updateDisplayName(nameInput.trim())
                        showEditName = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditName = false }) {
                    Text("Cancel", color = Color(0xFF8B949E))
                }
            },
            containerColor = MeshColors.Surface
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color(0xFF8B949E),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
        )
        Surface(color = MeshColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp), content = content)
        }
    }
}

@Composable
private fun BatteryModeCard(
    mode: BatteryMode,
    title: String,
    description: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color.copy(alpha = 0.12f) else MeshColors.SurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) BorderStroke(1.5.dp, color) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(description, color = Color(0xFF8B949E), fontSize = 12.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF8B949E), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(subtitle, color = Color(0xFF6E7681), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MeshColors.Primary
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF8B949E), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(value, color = Color(0xFF6E7681), fontSize = 12.sp)
        }
    }
}
