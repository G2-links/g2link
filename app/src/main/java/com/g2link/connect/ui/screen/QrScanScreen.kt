package com.g2link.connect.ui.screen

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.ui.viewmodel.QrViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    onBack: () -> Unit,
    onContactAdded: () -> Unit,
    viewModel: QrViewModel = hiltViewModel()
) {
    var addSuccess by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf(false) }
    var addedName by remember { mutableStateOf("") }
    var manualInput by remember { mutableStateOf("") }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // ── Real ZXing camera scanner ──────────────────────────
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { scannedText ->
            val success = viewModel.addContactFromQr(scannedText)
            if (success) {
                addedName = scannedText.split(":").getOrElse(2) { "Contact" }
                addSuccess = true
                addError = false
            } else {
                addError = true
                addSuccess = false
            }
        }
    }

    LaunchedEffect(addSuccess) {
        if (addSuccess) { kotlinx.coroutines.delay(1500); onContactAdded() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) } },
                title = { Text("Add Contact", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Status banners ─────────────────────────────
            AnimatedVisibility(visible = addSuccess) {
                Surface(color = G2Colors.Connected.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, G2Colors.Connected), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = G2Colors.Connected)
                        Text("$addedName added successfully!", color = G2Colors.Connected, fontWeight = FontWeight.Bold)
                    }
                }
            }
            AnimatedVisibility(visible = addError) {
                Surface(color = G2Colors.Emergency.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, G2Colors.Emergency), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = G2Colors.Emergency)
                        Column {
                            Text("Invalid QR code", color = G2Colors.Emergency, fontWeight = FontWeight.Bold)
                            Text("Only G2-Link QR codes are supported", color = Color(0xFF8BA0BF), fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Camera scan card ───────────────────────────
            Surface(color = G2Colors.SurfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(80.dp).background(G2Colors.Brand.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCodeScanner, null, tint = G2Colors.Brand, modifier = Modifier.size(44.dp))
                    }
                    Text("Scan QR Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Ask the other person to open G2-Link → Settings → My QR Code, then scan it with your camera.",
                        color = Color(0xFF8BA0BF), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
                    )
                    Button(
                        onClick = {
                            if (cameraPermission.status.isGranted) {
                                val options = ScanOptions().apply {
                                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    setPrompt("Scan the other person's G2-Link QR code")
                                    setCameraId(0)
                                    setBeepEnabled(true)
                                    setBarcodeImageEnabled(false)
                                    setOrientationLocked(false)
                                }
                                scanLauncher.launch(options)
                            } else {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Brand)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.Black)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (cameraPermission.status.isGranted) "Open Camera & Scan" else "Grant Camera & Scan",
                            fontWeight = FontWeight.Bold, color = Color.Black
                        )
                    }
                }
            }

            // ── Divider ────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E2A3A))
                Text("or type manually", color = Color(0xFF4A5568), fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E2A3A))
            }

            // ── Manual entry ───────────────────────────────
            Surface(color = G2Colors.SurfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter Contact Code", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Ask your contact to share their code from Settings → My QR Code → Share",
                        color = Color(0xFF8BA0BF), fontSize = 12.sp)
                    OutlinedTextField(
                        value = manualInput, onValueChange = { manualInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("DM:xxxxxxxx:Name", color = Color(0xFF4A5568)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = G2Colors.Brand, unfocusedBorderColor = Color(0xFF1E2A3A),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            val success = viewModel.addContactFromQr(manualInput.trim())
                            if (success) {
                                addedName = manualInput.split(":").getOrElse(2) { "Contact" }
                                addSuccess = true; addError = false
                            } else { addError = true; addSuccess = false }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Brand),
                        enabled = manualInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.PersonAdd, null, tint = Color.Black)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Contact", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            // ── How it works hint ──────────────────────────
            Surface(color = G2Colors.Brand.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, G2Colors.Brand.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, null, tint = G2Colors.Brand, modifier = Modifier.size(18.dp))
                    Text(
                        "QR pairing works completely offline. No internet needed. " +
                        "Once added, G2-Link will automatically find this contact via Bluetooth when nearby.",
                        color = Color(0xFF8BA0BF), fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
