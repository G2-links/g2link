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
            .background(MeshColors.Background)
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

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MeshColors.Primary.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = MeshColors.Primary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text("DisasterMesh", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                "Offline mesh messaging\nNo internet required",
                fontSize = 15.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Your Name", fontSize = 13.sp, color = Color(0xFF8B949E),
                    modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; nameError = false },
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
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF8B949E)) }
                )
            }

            if (!showPhoneField) {
                TextButton(onClick = { showPhoneField = true }) {
                    Icon(Icons.Default.AddCircleOutline, null,
                        tint = MeshColors.Primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add phone number (optional)", color = MeshColors.Primary, fontSize = 13.sp)
                }
            } else {
                AnimatedVisibility(visible = true, enter = expandVertically() + fadeIn()) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(phoneFocus),
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
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF8B949E)) }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    if (displayName.isBlank()) {
                        nameError = true
                    } else {
                        viewModel.saveProfile(displayName.trim(), phoneNumber.takeIf { it.isNotBlank() })
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MeshColors.Primary),
                enabled = displayName.isNotBlank()
            ) {
                Text("Join the Mesh", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.Black)
            }

            Text(
                "No account. No servers. No tracking.",
                fontSize = 12.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
