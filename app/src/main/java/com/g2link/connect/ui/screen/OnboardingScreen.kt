package com.g2link.connect.ui.screen

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
import com.g2link.connect.ui.theme.G2Colors
import com.g2link.connect.ui.viewmodel.OnboardingViewModel

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
                    colors = listOf(G2Colors.Background, Color(0xFF060C1A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(48.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(G2Colors.Brand.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        Brush.linearGradient(listOf(G2Colors.Brand, G2Colors.BrandDeep)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = G2Colors.Brand,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text("G2-Link", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "Communicate without internet,\nSIM card, or phone signal",
                fontSize = 15.sp,
                color = Color(0xFF8BA0BF),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            // Feature chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OnboardingChip(Icons.Default.Hub, "Mesh Network")
                OnboardingChip(Icons.Default.Lock, "Encrypted")
                OnboardingChip(Icons.Default.OfflineBolt, "Offline First")
            }

            Spacer(Modifier.height(8.dp))

            // Name field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Your Name", fontSize = 13.sp, color = Color(0xFF8BA0BF),
                    modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; nameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your name", color = Color(0xFF4A5568)) },
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
                        focusedBorderColor = G2Colors.Brand,
                        unfocusedBorderColor = Color(0xFF1E2A3A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = G2Colors.Brand,
                        focusedContainerColor = G2Colors.SurfaceVariant,
                        unfocusedContainerColor = G2Colors.SurfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF4A5568)) }
                )
            }

            // Optional phone
            if (!showPhoneField) {
                TextButton(onClick = { showPhoneField = true }) {
                    Icon(Icons.Default.AddCircleOutline, null,
                        tint = G2Colors.Brand, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add phone number (optional)", color = G2Colors.Brand, fontSize = 13.sp)
                }
            } else {
                AnimatedVisibility(visible = true, enter = expandVertically() + fadeIn()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Phone (optional)", fontSize = 13.sp, color = Color(0xFF8BA0BF),
                            modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(phoneFocus),
                            placeholder = { Text("+1 555 000 0000", color = Color(0xFF4A5568)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = G2Colors.Brand,
                                unfocusedBorderColor = Color(0xFF1E2A3A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = G2Colors.SurfaceVariant,
                                unfocusedContainerColor = G2Colors.SurfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF4A5568)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Join button
            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        nameError = true
                    } else {
                        viewModel.saveProfile(displayName.trim(), phoneNumber.takeIf { it.isNotBlank() })
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = G2Colors.Brand),
                enabled = displayName.isNotBlank()
            ) {
                Text(
                    "Join the Mesh",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.Black)
            }

            Text(
                "🔒 No account. No servers. No tracking.\nYour data never leaves your device.",
                fontSize = 12.sp,
                color = Color(0xFF4A5568),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = G2Colors.SurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, G2Colors.Brand.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = G2Colors.Brand, modifier = Modifier.size(14.dp))
            Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}
