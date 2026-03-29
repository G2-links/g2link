package com.g2link.connect.ui.screen

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap  // ✅ FIX: proper import
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g2link.connect.sharing.ApkShareManager
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.update.UpdateInfo
import com.g2link.connect.update.UpdateState
// ✅ FIX: generateQrBitmap is defined in SettingsContactsQrScreens.kt (same package)
// No import needed — same package resolves it automatically

// ═══════════════════════════════════════════════════════════
// UPDATE BANNER
// ═══════════════════════════════════════════════════════════

@Composable
fun UpdateBanner(
    updateState: UpdateState,
    onDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = updateState is UpdateState.UpdateAvailable,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        val info = (updateState as? UpdateState.UpdateAvailable)?.info ?: return@AnimatedVisibility

        Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                G2Colors.BrandDeep.copy(alpha = 0.95f),
                                G2Colors.Brand.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.SystemUpdate, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("G2-Link v${info.latestVersion} available",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(info.releaseNotes, color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp, maxLines = 1)
                    }
                    TextButton(
                        onClick = { onDownload(info.downloadUrl) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.2f)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Update", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null,
                            tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SHARE G2-LINK SCREEN
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAppScreen(
    onBack: () -> Unit,
    apkShareManager: ApkShareManager
) {
    val apkSize = remember { apkShareManager.getApkSizeMb() }
    val qrContent = remember { apkShareManager.getDownloadQrContent() }
    // ✅ FIX: generateQrBitmap comes from SettingsContactsQrScreens.kt (same package, no import needed)
    val qrBitmap = remember(qrContent) { generateQrBitmap(qrContent, 500) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = { Text("Share G2-Link", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = G2Colors.Surface)
            )
        },
        containerColor = G2Colors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = G2Colors.SurfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).background(G2Colors.BrandGlow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Hub, null, tint = G2Colors.Brand, modifier = Modifier.size(34.dp))
                    }
                    Text("G2-Link", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Offline Mesh Communication", color = Color(0xFF8BA0BF), fontSize = 13.sp)
                    Text("Works without internet or cell signal",
                        color = Color(0xFF6E7681), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }

            ShareMethodCard(
                icon = Icons.Default.Bluetooth,
                title = "Share App Directly",
                subtitle = "Send APK ($apkSize) via Bluetooth — no internet needed",
                color = G2Colors.Brand,
                buttonLabel = "Share Now",
                onClick = { apkShareManager.shareApkViaBluetoothOrAny() }
            )

            ShareMethodCard(
                icon = Icons.Default.Link,
                title = "Share Download Link",
                subtitle = "Send GitHub link via SMS or WhatsApp",
                color = G2Colors.Connected,
                buttonLabel = "Share Link",
                onClick = { apkShareManager.shareDownloadLink() }
            )

            Text("Or scan this QR code", color = Color(0xFF8BA0BF), fontSize = 13.sp,
                fontWeight = FontWeight.Medium)

            if (qrBitmap != null) {
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(200.dp)) {
                    Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                        Image(
                            // ✅ FIX: asImageBitmap() now resolves via import at top
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "G2-Link download QR",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Surface(color = G2Colors.BrandGlow, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Info, null, tint = G2Colors.Brand, modifier = Modifier.size(18.dp))
                    Text(
                        "In emergencies, install G2-Link from a neighbor's phone via Bluetooth.",
                        color = Color(0xFF8BA0BF), fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// SHARE METHOD CARD
// ═══════════════════════════════════════════════════════════

@Composable
private fun ShareMethodCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Surface(
        color = G2Colors.SurfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Color(0xFF8BA0BF), fontSize = 12.sp, lineHeight = 16.sp)
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

// ✅ NOTE: generateQrBitmap is intentionally NOT defined here.
// It lives in SettingsContactsQrScreens.kt (same package com.g2link.connect.ui.screen)
// and is accessible without any import.
