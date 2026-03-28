package com.disastermesh.connect.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.disastermesh.connect.ui.theme.MeshColors
import com.disastermesh.connect.ui.viewmodel.QrViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════
// QR CODE SHOW SCREEN — Display my QR for others to scan
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrShowScreen(
    onBack: () -> Unit,
    viewModel: QrViewModel = hiltViewModel()
) {
    val qrContent by viewModel.qrContent.collectAsState(initial = "")
    val displayName by viewModel.displayName.collectAsState(initial = "")

    val qrBitmap: Bitmap? = remember(qrContent) {
        if (qrContent.isNotBlank()) generateQrBitmap(qrContent, 600) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = { Text("My QR Code", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MeshColors.Surface)
            )
        },
        containerColor = MeshColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                "Let others scan this to add you as a contact",
                color = Color(0xFF8B949E),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            // ── QR Code Card ──────────────────────────────
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.size(280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = MeshColors.Primary)
                    }
                }
            }

            // ── Name Badge ────────────────────────────────
            Surface(
                color = MeshColors.SurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Person, null, tint = MeshColors.Primary, modifier = Modifier.size(20.dp))
                    Text(
                        displayName.ifBlank { "Your Name" },
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }

            // ── Info card ─────────────────────────────────
            Surface(
                color = MeshColors.Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = MeshColors.Primary, modifier = Modifier.size(20.dp))
                    Text(
                        "Works completely offline. No internet needed to pair with contacts.",
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// QR CODE SCAN SCREEN — Camera scan to add contact
// ═══════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    onBack: () -> Unit,
    onContactAdded: () -> Unit,
    viewModel: QrViewModel = hiltViewModel()
) {
    var scanResult by remember { mutableStateOf<String?>(null) }
    var addSuccess by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf(false) }

    // ✅ FIX: Use rememberCoroutineScope instead of the broken recursive extension function
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = { Text("Scan QR Code", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MeshColors.Surface)
            )
        },
        containerColor = MeshColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Camera Viewfinder Placeholder ─────────────
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color(0xFF0D1117), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        null,
                        tint = MeshColors.Primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "Point camera at\ncontact's QR code",
                        color = Color(0xFF8B949E),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }

                // Corner markers
                QrCornerMarkers()
            }

            Text(
                "Or enter the code manually",
                color = Color(0xFF6E7681),
                fontSize = 13.sp
            )

            // ── Manual Input ─────────────────────────────
            var manualInput by remember { mutableStateOf("") }
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("DM:deviceId:name", color = Color(0xFF8B949E)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MeshColors.Primary,
                    unfocusedBorderColor = Color(0xFF30363D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = MeshColors.Primary
                ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Link, null, tint = Color(0xFF8B949E))
                }
            )

            Button(
                onClick = {
                    val success = viewModel.addContactFromQr(manualInput.trim())
                    if (success) {
                        addSuccess = true
                        // ✅ FIX: Use the remembered scope with proper imported launch/delay
                        scope.launch {
                            delay(1500)
                            onContactAdded()
                        }
                    } else {
                        addError = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeshColors.Primary),
                enabled = manualInput.isNotBlank()
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("Add Contact", fontWeight = FontWeight.Bold)
            }

            // ── Status Messages ───────────────────────────
            if (addSuccess) {
                Surface(
                    color = MeshColors.Connected.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MeshColors.Connected)
                        Text("Contact added successfully!", color = MeshColors.Connected)
                    }
                }
            }

            if (addError) {
                Surface(
                    color = MeshColors.Emergency.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = MeshColors.Emergency)
                        Text("Invalid QR code format", color = MeshColors.Emergency)
                    }
                }
            }
        }
    }
}

// ─── QR corner markers ────────────────────────────────────
@Composable
private fun QrCornerMarkers() {
    val cornerColor = MeshColors.Primary
    val cornerSize = 24.dp
    val strokeWidth = 3.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Modifier.padding(12.dp)
    }
}

// ═══════════════════════════════════════════════════════════
// QR BITMAP GENERATOR
// Uses ZXing to generate QR from string
// ═══════════════════════════════════════════════════════════
fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size, size
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

// ✅ FIX: The old recursive "launch" extension has been removed entirely.
//    Use rememberCoroutineScope() + kotlinx.coroutines.launch (imported at top).
