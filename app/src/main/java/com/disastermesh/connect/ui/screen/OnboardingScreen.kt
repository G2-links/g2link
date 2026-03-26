package com.disastermesh.connect.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.disastermesh.connect.ui.theme.MeshColors
import com.disastermesh.connect.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var showPhoneField by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    val keyboard = LocalSoftwareKeyboardController.current
    val phoneFocus = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MeshColors.Background,
                        Color(0xFF0A1628)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            // ── App Icon / Logo ────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MeshColors.Primary, Color(0xFF0A1628))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp)
                )
            }

            // ── Title ─────────────────────────────────────
            Text(
                text = "DisasterMesh",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Communicate without internet,\nSIM cards, or phone signal",
                fontSize = 16.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(16.dp))

            // ── Feature Cards ─────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureChip(Icons.Default.Wifi, "Mesh Network")
                FeatureChip(Icons.Default.Lock, "Encrypted")
                FeatureChip(Icons.Default.OfflineBolt, "Offline")
            }

            Spacer(Modifier.height(16.dp))

            // ── Name Input ────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Name",
                    fontSize = 14.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        nameError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your name", color = Color(0xFF8B949E)) },
                    singleLine = true,
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("Name is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = if (showPhoneField) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboard?.hide() },
                        onNext = { phoneFocus.requestFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MeshColors.Primary,
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = MeshColors.Primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF8B949E)
                        )
                    }
                )
            }

            // ── Phone Number (Optional) ───────────────────
            if (!showPhoneField) {
                TextButton(
                    onClick = { showPhoneField = true }
                ) {
                    Text(
                        "Add phone number (optional)",
                        color = MeshColors.Primary,
                        fontSize = 14.sp
                    )
                }
            } else {
                AnimatedVisibility(
                    visible = showPhoneField,
                    enter = expandVertically() + fadeIn()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Phone Number (optional)",
                            fontSize = 14.sp,
                            color = Color(0xFF8B949E),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Used to connect with contacts when internet is available",
                            fontSize = 12.sp,
                            color = Color(0xFF6E7681),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(phoneFocus),
                            placeholder = { Text("+1 555 000 0000", color = Color(0xFF8B949E)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MeshColors.Primary,
                                unfocusedBorderColor = Color(0xFF30363D),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MeshColors.Primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF8B949E)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Continue Button ───────────────────────────
            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        nameError = true
                    } else {
                        viewModel.saveProfile(
                            displayName.trim(),
                            phoneNumber.takeIf { it.isNotBlank() }
                        )
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeshColors.Primary
                ),
                enabled = displayName.isNotBlank()
            ) {
                Text(
                    "Start Communicating",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }

            // ── Privacy note ──────────────────────────────
            Text(
                text = "🔒 Your data never leaves your device.\nNo account, no servers, no tracking.",
                fontSize = 12.sp,
                color = Color(0xFF6E7681),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeatureChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = Color(0xFF161B22),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MeshColors.Primary, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 12.sp, color = Color.White)
        }
    }
}
