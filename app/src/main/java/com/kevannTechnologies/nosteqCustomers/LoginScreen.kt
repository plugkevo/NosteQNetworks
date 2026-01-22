package com.kevannTechnologies.nosteqCustomers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    username: String,
    password: String,
    passwordVisible: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onContactSupportClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Gradient colors - Deep blue to teal blend
    val gradientColors = listOf(
        Color(0xFF1A237E),  // Deep indigo
        Color(0xFF0D47A1),  // Dark blue
        Color(0xFF00695C)   // Teal
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo and Title - White text on gradient
            Text(
                text = "NosteQ Networks",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Internet Provider",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Form container - no card, directly on gradient
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sign in to your account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Username Field - white/transparent style
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Username", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLoginClick()
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible)
                                    "Hide password"
                                else
                                    "Show password",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        cursorColor = Color.White
                    )
                )

                // Error Message
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF8A80),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot Password
                TextButton(
                    onClick = onForgotPasswordClick,
                    modifier = Modifier.align(Alignment.End),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Forgot password?",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login Button - white on gradient
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF1A237E),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1A237E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Support Text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Need help? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = onContactSupportClick,
                        enabled = !isLoading
                    ) {
                        Text(
                            text = "Contact Support",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
