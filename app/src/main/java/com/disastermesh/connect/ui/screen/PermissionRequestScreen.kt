package com.disastermesh.connect.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disastermesh.connect.ui.theme.MeshColors

private data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val reason: String
)

private val PERMISSION_ITEMS = listOf(
    PermissionItem(Icons.Default.Bluetooth, "Bluetooth", "Find and connect to nearby devices without Wi-Fi"),
    PermissionItem(Icons.Default.Wifi, "Wi-Fi Direct", "High-speed peer-to-peer connections in your area"),
    PermissionItem(Icons.Default.LocationOn, "Location", "Required by Android for Bluetooth/Wi-Fi scanning"),
    PermissionItem(Icons.Default.CameraAlt, "Camera", "Scan QR codes to add contacts offline"),
    PermissionItem(Icons.Default.Notifications, "Notifications", "Alert you when messages arrive in background"),
)

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshColors.Background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MeshColors.Primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Security,
                null,
                tint = MeshColors.Primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            "Permissions Required",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "DisasterMesh needs these permissions to create a mesh network between nearby devices. No data is sent to the internet.",
            color = Color(0xFF8B949E),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(8.dp))

        // Permission list
        PERMISSION_ITEMS.forEach { item ->
            Surface(
                color = MeshColors.SurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MeshColors.Primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(item.icon, null, tint = MeshColors.Primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(item.reason, color = Color(0xFF8B949E), fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MeshColors.Primary)
        ) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(Modifier.width(10.dp))
            Text("Grant Permissions", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            "🔒 Your location data is only used locally.\nIt is never sent to any server.",
            color = Color(0xFF6E7681),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))
    }
}
