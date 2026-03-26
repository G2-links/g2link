package com.disastermesh.connect.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.disastermesh.connect.data.local.entity.ContactEntity
import com.disastermesh.connect.domain.model.ContactType
import com.disastermesh.connect.ui.theme.MeshColors
import com.disastermesh.connect.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateToChat: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val contacts by viewModel.contactInfo.collectAsState(initial = null)
    // For the full contacts list use a dedicated ViewModel in production
    // Here we reuse available state for demonstration

    var selectedContact by remember { mutableStateOf<ContactEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                title = { Text("Contacts", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* navigate to QR scan */ }) {
                        Icon(Icons.Default.QrCodeScanner, "Scan QR", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MeshColors.Surface)
            )
        },
        containerColor = MeshColors.Background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Add contact hint ──────────────────────────
            Surface(
                color = MeshColors.Primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = MeshColors.Primary, modifier = Modifier.size(18.dp))
                    Text(
                        "Scan a QR code to add contacts offline. Nearby devices appear automatically.",
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            // Contact list will populate from nearby discoveries
            // Showing empty state guidance here
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        null,
                        tint = Color(0xFF30363D),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No contacts yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Contacts appear here when:\n• You scan their QR code\n• They connect to your mesh\n• You mark peers as contacts",
                        color = Color(0xFF8B949E),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { /* trigger QR scan */ },
                        colors = ButtonDefaults.buttonColors(containerColor = MeshColors.Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan QR Code")
                    }
                }
            }
        }
    }
}

// ─── Reusable Contact Row ─────────────────────────────────
@Composable
fun ContactRow(
    contact: ContactEntity,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    showArrow: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with online indicator
        Box {
            ContactAvatar(name = contact.displayName, size = 46)
            if (contact.lastSeenAt > System.currentTimeMillis() - 60_000) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MeshColors.Connected, androidx.compose.foundation.shape.CircleShape)
                        .border(2.dp, MeshColors.Surface, androidx.compose.foundation.shape.CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    contact.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                // Priority badge
                when (contact.contactType) {
                    ContactType.FAMILY -> Surface(
                        color = MeshColors.PriorityFamily.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Family",
                            color = MeshColors.PriorityFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    ContactType.EMERGENCY -> Surface(
                        color = MeshColors.Emergency.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Emergency",
                            color = MeshColors.Emergency,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    else -> {}
                }
            }
            Text(
                if (contact.lastSeenAt > 0)
                    "Last seen: ${formatRelativeTime(contact.lastSeenAt)}"
                else "Not yet connected",
                color = Color(0xFF6E7681),
                fontSize = 12.sp
            )
        }

        if (showArrow) {
            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF6E7681), modifier = Modifier.size(20.dp))
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000      -> "just now"
        diff < 3600_000    -> "${diff / 60_000}m ago"
        diff < 86400_000   -> "${diff / 3600_000}h ago"
        else               -> "${diff / 86400_000}d ago"
    }
}
