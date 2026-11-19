package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.local.BusinessSettingsEntity
import com.example.data.repository.AuthManager
import com.example.ui.components.NaqshLogoImage
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneGoldLight
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldPrimary
import com.example.util.AppStrings
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    settings: BusinessSettingsEntity,
    authManager: AuthManager,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var phoneInput by remember { mutableStateOf(settings.ownerMobile.ifBlank { "+91 9471477887" }) }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryHelp by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000)
            resendSeconds -= 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(DeepEmerald, EmeraldPrimary, DeepEmerald))
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = AppStrings.BISMILLAH,
                color = ChampagneGoldLight,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            NaqshLogoImage(customLogoUri = settings.customLogoUri, size = 140.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = settings.businessName,
                color = ChampagneGold,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = settings.businessCategory,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.6.sp),
                textAlign = TextAlign.Center
            )
            Text(
                text = settings.tagline,
                color = ChampagneGoldLight.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Owner Secure Login",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Owner: ${settings.ownerName} • Offline business mode unlocks automatically after sign-in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            authManager.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AccountCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sign in with Google (Owner Account)")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Or Login with Real Mobile SMS OTP",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Requires Firebase Phone Sign-In enabled + Google Play Services on device.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Owner Mobile (+91...)") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = verificationId == null && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_phone_input")
                    )

                    if (verificationId != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it },
                            label = { Text("Enter 6-Digit SMS OTP") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val vId = verificationId ?: return@Button
                                if (otpCode.trim().length < 6) {
                                    errorMessage = "Please enter the 6-digit OTP received via SMS."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val res = authManager.verifyOtpCode(vId, otpCode)
                                    isLoading = false
                                    if (res.isSuccess) {
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Invalid OTP code."
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_otp_button")
                        ) {
                            Text("Verify OTP & Enter Boutique")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    verificationId = null
                                    otpCode = ""
                                    errorMessage = null
                                }
                            ) {
                                Text("Change Number")
                            }
                            TextButton(
                                enabled = resendSeconds == 0 && !isLoading,
                                onClick = {
                                    val activity = context as? Activity ?: return@TextButton
                                    val normalized = phoneInput.replace(" ", "")
                                    isLoading = true
                                    authManager.sendPhoneOtp(
                                        activity = activity,
                                        phoneNumberE164 = normalized,
                                        resendToken = resendToken,
                                        onCodeSent = { vid, tok ->
                                            verificationId = vid
                                            resendToken = tok
                                            resendSeconds = 60
                                            isLoading = false
                                            statusMessage = "OTP resent to $normalized"
                                        },
                                        onAutoVerified = { cred ->
                                            scope.launch {
                                                val r = authManager.signInWithPhoneCredential(cred)
                                                isLoading = false
                                                if (r.isSuccess) onAuthSuccess()
                                                else errorMessage = r.exceptionOrNull()?.localizedMessage
                                            }
                                        },
                                        onError = { msg ->
                                            isLoading = false
                                            errorMessage = msg
                                        }
                                    )
                                }
                            ) {
                                Text(if (resendSeconds > 0) "Resend (${resendSeconds}s)" else "Resend OTP")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                val activity = context as? Activity
                                if (activity == null) {
                                    errorMessage = "Activity context unavailable."
                                    return@OutlinedButton
                                }
                                val normalized = phoneInput.replace(" ", "")
                                if (!normalized.startsWith("+") || normalized.length < 11) {
                                    errorMessage = "Enter number with country code, e.g. +919471477887"
                                    return@OutlinedButton
                                }
                                isLoading = true
                                errorMessage = null
                                statusMessage = null
                                authManager.sendPhoneOtp(
                                    activity = activity,
                                    phoneNumberE164 = normalized,
                                    onCodeSent = { vid, tok ->
                                        verificationId = vid
                                        resendToken = tok
                                        resendSeconds = 60
                                        isLoading = false
                                        statusMessage = "Real SMS OTP sent to $normalized"
                                    },
                                    onAutoVerified = { cred ->
                                        scope.launch {
                                            val r = authManager.signInWithPhoneCredential(cred)
                                            isLoading = false
                                            if (r.isSuccess) onAuthSuccess()
                                            else errorMessage = r.exceptionOrNull()?.localizedMessage
                                        }
                                    },
                                    onError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    }
                                )
                            },
                            enabled = !isLoading,
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_otp_button")
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Send Real SMS OTP")
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showRecoveryHelp = !showRecoveryHelp },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Account Recovery & Firebase OTP Setup Info")
                    }

                    if (showRecoveryHelp) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "1. Google Sign-In works immediately with the provisioned Firebase project.\n" +
                                    "2. For Real SMS OTP: Open Firebase Console -> Authentication -> Sign-in method -> Enable 'Phone', and add +91 9471477887.\n" +
                                    "3. Account Recovery: Sign in with your verified Google account or verified Owner Mobile OTP to regain access and update your mobile number in Settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
