package com.sophos.security.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sophos.security.mobile.core.designsystem.theme.*
import com.sophos.security.mobile.core.network.MockSecurityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onNavigateToLogin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Sophos Security",
                tint = SecurityGreen,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SOPHOS SECURITY",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enterprise Mobile SOC Console",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkOnSurfaceMuted
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = SecurityGreen,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToMfa: () -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // Sophos Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SecurityGreen,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "SOPHOS CENTRAL",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Cloud Mobile SOC Console",
                    style = MaterialTheme.typography.labelMedium,
                    color = DarkOnSurfaceMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // User ID Field
        Text(
            text = "User ID",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = userId,
            onValueChange = {
                userId = it
                if (errorMessage != null) errorMessage = null
            },
            placeholder = { Text("Enter Sophos Client ID / User ID", color = DarkOnSurfaceMuted) },
            leadingIcon = {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SecurityGreen)
            },
            trailingIcon = {
                if (userId.isNotEmpty()) {
                    IconButton(onClick = { userId = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = DarkOnSurfaceMuted)
                    }
                } else {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { text ->
                            userId = text.trim()
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = DarkOnSurfaceMuted)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SecurityGreen,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Secret Field
        Text(
            text = "Secret",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = secret,
            onValueChange = {
                secret = it
                if (errorMessage != null) errorMessage = null
            },
            placeholder = { Text("Enter Sophos Client Secret", color = DarkOnSurfaceMuted) },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SecurityGreen)
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (secret.isEmpty()) {
                        IconButton(onClick = {
                            clipboardManager.getText()?.text?.let { text ->
                                secret = text.trim()
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = DarkOnSurfaceMuted)
                        }
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = DarkOnSurfaceMuted
                        )
                    }
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SecurityGreen,
                unfocusedBorderColor = DarkBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Error Banner
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2B1214), RoundedCornerShape(8.dp))
                    .border(1.dp, SeverityCritical, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SeverityCritical, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Login Button
        Button(
            onClick = {
                if (userId.isNotBlank() && secret.isNotBlank() && !isLoading) {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        val result = MockSecurityRepository.login(userId, secret)
                        isLoading = false
                        result.onSuccess {
                            onLoginSuccess()
                        }.onFailure { err ->
                            errorMessage = err.message ?: "Authentication failed. Please verify credentials."
                        }
                    }
                }
            },
            enabled = userId.isNotBlank() && secret.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecurityGreen,
                disabledContainerColor = DarkSurfaceVariant
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.5.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AUTHENTICATING WITH CENTRAL...",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    fontSize = 13.sp
                )
            } else {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = if (userId.isNotBlank() && secret.isNotBlank()) Color.Black else DarkOnSurfaceMuted)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ENTER SOC CONSOLE",
                    fontWeight = FontWeight.Bold,
                    color = if (userId.isNotBlank() && secret.isNotBlank()) Color.Black else DarkOnSurfaceMuted,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security Notice
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = CardDefaults.outlinedCardBorder().copy(width = 0.5.dp, brush = androidx.compose.ui.graphics.SolidColor(DarkBorder))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = SecurityGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Session-Only Security: Credentials authenticate directly against id.sophos.com over TLS. They remain in volatile memory for the active session and are never saved to device storage.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkOnSurfaceMuted,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun MfaScreen(onMfaSuccess: () -> Unit) {
    var otpCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.PhonelinkLock,
            contentDescription = null,
            tint = SecurityGreen,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Two-Factor Verification",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter the 6-digit TOTP code generated by your Authenticator app",
            style = MaterialTheme.typography.bodySmall,
            color = DarkOnSurfaceMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) otpCode = it },
            label = { Text("6-Digit OTP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 4.sp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onMfaSuccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
        ) {
            Text("VERIFY & CONTINUE", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun BiometricPromptScreen(onSuccess: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Fingerprint,
            contentDescription = null,
            tint = SecurityGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Biometric Re-Authentication",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unlock secure hardware keystore session for sensitive SOC actions",
            style = MaterialTheme.typography.bodySmall,
            color = DarkOnSurfaceMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSuccess,
            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
        ) {
            Text("TOUCH SENSOR / CONFIRM", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}
