package com.g2link.connect.ui.screen

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.g2link.connect.domain.model.BatteryMode
import com.g2link.connect.domain.model.ConnectionStatus
import com.g2link.connect.domain.model.ContactType
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.ui.viewmodel.QrViewModel
import com.g2link.connect.ui.viewmodel.SettingsViewModel

// ═══════════════════════════════════════════════════════════
// SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════
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
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            // Identity section
            Text("IDENTITY", color = Color(0xFF8BA0BF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
            Surface(color = G2Colors.Surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Surface(color = G2Colors.SurfaceVariant, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            ContactAvatar(name = displayName.ifBlank { "?" }, size = 52)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName.ifBlank { "No name set" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(myDeviceId.take(20) + "...", color = Color(0xFF4A5568), fontSize = 11.sp)
                            }
                            IconButton(onClick = { nameInput = displayName; showEditName = true }) {
                                Icon(Icons.Default.Edit, "Edit", tint = G2Colors.Brand)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onNavigateToQrShow, modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, G2Colors.Brand), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.QrCode, null, tint = G2Colors.Brand, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("My QR Code", color = G2Colors.Brand, fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = onNavigateToQrScan, modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, G2Colors.Brand), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = G2Colors.Brand, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scan QR", color = G2Colors.Brand, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Mesh status
            Text("MESH STATUS", color = Color(0xFF8BA0BF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
            Surface(color = G2Colors.Surface, modifier = Modifier.fillMaxWidth()) {
                Surface(color = G2Colors.SurfaceVariant, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Hub, null, tint = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> G2Colors.Connected
                            ConnectionStatus.SEARCHING -> G2Colors.Searching
                            else -> Color(0xFF4A5568)
                        }, modifier = Modifier.size(32.dp))
                        Column {
                            Text(when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> "Connected — $peerCount peer${if (peerCount != 1) "s" else ""}"
                                ConnectionStatus.SEARCHING -> "Searching..."
                                else -> "Offline"
                            }, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Mesh networking active", color = Color(0xFF8BA0BF), fontSize = 12.sp)
                        }
                    }
                }
            }

            // Battery mode
            Text("BATTERY MODE", color = Color(0xFF8BA0BF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
            Surface(color = G2Colors.Surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(BatteryMode.EMERGENCY, "⚡ Emergency Mode", G2Colors.Emergency),
                        Triple(BatteryMode.NORMAL, "🔵 Normal Mode", G2Colors.Brand),
                        Triple(BatteryMode.SAVER, "🔋 Battery Saver", G2Colors.Connected)
                    ).forEach { (mode, title, color) ->
                        Surface(onClick = { viewModel.setBatteryMode(mode) },
                            color = if (batteryMode == mode) color.copy(alpha = 0.12f) else G2Colors.SurfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = if (batteryMode == mode) BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (batteryMode == mode) Icon(Icons.Default.CheckCircle, null, tint = color, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // Preferences
            Text("PREFERENCES", color = Color(0xFF8BA0BF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
            Surface(color = G2Colors.Surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingsToggleRow(Icons.Default.Notifications, "Notifications", "Show alerts for incoming messages",
                        notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
                    HorizontalDivider(color = Color(0xFF1E2A3A), modifier = Modifier.padding(start = 56.dp))
                    SettingsToggleRow(Icons.Default.LocationOn, "Auto-share Location", "Include GPS in broadcasts",
                        locationShareEnabled) { viewModel.setLocationShareEnabled(it) }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditName) {
        AlertDialog(
            onDismissRequest = { showEditName = false },
            title = { Text("Edit Display Name", color = Color.White) },
            text = {
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = G2Colors.Brand,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White))
            },
            confirmButton = {
                Button(onClick = { if (nameInput.isNotBlank()) { viewModel.updateDisplayName(nameInput.trim()); showEditName = false } }) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { showEditName = false }) { Text("Cancel", color = Color(0xFF8BA0BF)) } },
            containerColor = G2Colors.Surface
        )
    }
}

@Composable
private fun SettingsToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF8BA0BF), modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp)
            Text(subtitle, color = Color(0xFF4A5568), fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = G2Colors.Brand))
    }
}

// ═══════════════════════════════════════════════════════════
// CONTACTS SCREEN
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(onNavigateToChat: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                title = { Text("Contacts", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.People, null, tint = Color(0xFF1E2A3A), modifier = Modifier.size(64.dp))
                Text("No contacts yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Contacts appear here when:\n• You scan their QR code\n• They connect to your mesh",
                    color = Color(0xFF8BA0BF), fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// QR SHOW SCREEN
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShowScreen(onBack: () -> Unit, viewModel: QrViewModel = hiltViewModel()) {
    val qrContent by viewModel.qrContent.collectAsState(initial = "")
    val displayName by viewModel.displayName.collectAsState(initial = "")
    val qrBitmap: Bitmap? = remember(qrContent) { if (qrContent.isNotBlank()) generateQrBitmap(qrContent, 600) else null }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                title = { Text("My QR Code", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(Modifier.height(8.dp))
            Text("Let others scan this to add you as a contact", color = Color(0xFF8BA0BF), fontSize = 14.sp, textAlign = TextAlign.Center)
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 8.dp, modifier = Modifier.size(280.dp)) {
                Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.fillMaxSize())
                    } else {
                        CircularProgressIndicator(color = G2Colors.Brand)
                    }
                }
            }
            Surface(color = G2Colors.SurfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Person, null, tint = G2Colors.Brand, modifier = Modifier.size(20.dp))
                    Text(displayName.ifBlank { "Your Name" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// QR SCAN SCREEN
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(onBack: () -> Unit, onContactAdded: () -> Unit, viewModel: QrViewModel = hiltViewModel()) {
    var manualInput by remember { mutableStateOf("") }
    var addSuccess by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                title = { Text("Scan QR Code", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.size(280.dp).background(G2Colors.SurfaceVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.QrCodeScanner, null, tint = G2Colors.Brand, modifier = Modifier.size(64.dp))
                    Text("Point camera at\ncontact's QR code", color = Color(0xFF8BA0BF), textAlign = TextAlign.Center, fontSize = 14.sp)
                }
            }
            Text("Or enter the code manually", color = Color(0xFF4A5568), fontSize = 13.sp)
            OutlinedTextField(
                value = manualInput, onValueChange = { manualInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("DM:deviceId:name", color = Color(0xFF4A5568)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = G2Colors.Brand,
                    unfocusedBorderColor = Color(0xFF1E2A3A), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = {
                    val success = viewModel.addContactFromQr(manualInput.trim())
                    if (success) { addSuccess = true } else { addError = true }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Brand),
                enabled = manualInput.isNotBlank()
            ) {
                Icon(Icons.Default.PersonAdd, null, tint = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Add Contact", fontWeight = FontWeight.Bold, color = Color.Black)
            }
            if (addSuccess) {
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); onContactAdded() }
                Text("✅ Contact added!", color = G2Colors.Connected, fontWeight = FontWeight.Bold)
            }
            if (addError) Text("❌ Invalid QR code format", color = G2Colors.Emergency)
        }
    }
}

fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) { null }
}
