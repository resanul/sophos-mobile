package com.sophos.security.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sophos.security.mobile.core.designsystem.theme.*
import kotlinx.coroutines.delay

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
    var clientId by remember { mutableStateOf(com.sophos.security.mobile.core.network.MockSecurityRepository.clientId) }
    var clientSecret by remember { mutableStateOf(com.sophos.security.mobile.core.network.MockSecurityRepository.clientSecret) }
    var dataRegion by remember { mutableStateOf(com.sophos.security.mobile.core.network.MockSecurityRepository.dataRegionUrl) }
    var isConnecting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SecurityGreen,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "SOPHOS CENTRAL CLOUD",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Direct Mobile API Gateway",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkOnSurfaceMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = clientId,
            onValueChange = { clientId = it },
            label = { Text("Sophos Client ID") },
            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = DarkOnSurfaceMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = clientSecret,
            onValueChange = { clientSecret = it },
            label = { Text("Sophos Client Secret") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = DarkOnSurfaceMuted) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = dataRegion,
            onValueChange = { dataRegion = it },
            label = { Text("Regional API Endpoint") },
            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = DarkOnSurfaceMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                isConnecting = true
                com.sophos.security.mobile.core.network.MockSecurityRepository.clientId = clientId.trim()
                com.sophos.security.mobile.core.network.MockSecurityRepository.clientSecret = clientSecret.trim()
                com.sophos.security.mobile.core.network.MockSecurityRepository.dataRegionUrl = dataRegion.trim()
                com.sophos.security.mobile.core.network.MockSecurityRepository.refresh()
                onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecurityGreen)
        ) {
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
            } else {
                Text(
                    text = "CONNECT DIRECT TO SOPHOS CENTRAL",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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
