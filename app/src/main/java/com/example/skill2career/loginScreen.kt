package com.example.skill2career

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    mainViewModel: MainViewModel,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    // Separate login systems - user selects which system to use
    var selectedLoginType by remember { mutableStateOf("Student") } // "Student" or "Admin"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var adminLoginStep by remember { mutableStateOf("email") } // "email" or "password" for admin

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        startAnimation = true
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun validateForm(): String? {
        return when {
            email.isBlank() -> "Email is required"
            !isValidEmail(email) -> "Please enter a valid email address"
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    // Clean monochrome theme
    val primaryColor = Color.Black
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8F9FA),
            Color(0xFFFFFFFF)
        )
    )
    val cardBackground = Color.White
    val surfaceVariant = Color(0xFFF1F3F4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo and Title Section
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo Circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Text(
                        text = "Sign in to continue",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Login Type Selection - Two separate systems
            Text(
                text = "Select Login Type",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Login Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Student" to Icons.Default.Person, "Admin" to Icons.Default.AdminPanelSettings).forEach { (type, icon) ->
                    val selected = selectedLoginType == type
                    val containerColor by animateColorAsState(
                        targetValue = if (selected) Color.Black else Color.Transparent,
                        label = "loginTypeContainer"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) Color.White else Color.Gray,
                        label = "loginTypeContent"
                    )

                    Surface(
                        onClick = { 
                            selectedLoginType = type
                            // Reset fields when switching
                            email = ""
                            password = ""
                            adminLoginStep = "email"
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .height(48.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type,
                                color = contentColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Show different UI based on selected login type
            if (selectedLoginType == "Admin") {
                // Admin Login Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin Login",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (adminLoginStep == "email") 
                                "Enter your email to request access. Super admin will approve and set your password."
                            else 
                                "Enter your password to login.",
                            color = Color(0xFFE65100),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Form Card
            AnimatedVisibility(
                visible = startAnimation,
                enter = fadeIn(tween(600, delayMillis = 200)) + slideInVertically(tween(600, delayMillis = 200)) { it / 5 }
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                emailError = !isValidEmail(it) && it.isNotEmpty()
                                errorMessage = null
                            },
                            label = { Text("Email address") },
                            placeholder = { Text("name@company.com") },
                            isError = emailError || errorMessage != null,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = if (emailError) Color.Red else Color.Gray
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                autoCorrectEnabled = false
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLeadingIconColor = Color.Black,
                                unfocusedLeadingIconColor = Color.Gray
                            )
                        )

                        if (emailError) {
                            Text(
                                text = "Please enter a valid email address",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedLeadingIconColor = Color.Black,
                                unfocusedLeadingIconColor = Color.Gray,
                                focusedTrailingIconColor = Color.Gray,
                                unfocusedTrailingIconColor = Color.Gray
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Forgot Password
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(onClick = { /* TODO: Forgot password */ }) {
                                Text(
                                    "Forgot Password?",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Error Message
                        errorMessage?.let {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFEBEE)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = it,
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Login Button
                        var buttonPressed by remember { mutableStateOf(false) }
                        val buttonScale by animateFloatAsState(
                            targetValue = if (buttonPressed) 0.97f else 1f,
                            label = "buttonScale"
                        )

                        Button(
                            onClick = {
                                buttonPressed = true
                                val validationError = validateForm()
                                if (validationError != null) {
                                    errorMessage = validationError
                                    emailError = email.isBlank() || !isValidEmail(email)
                                } else {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    if (selectedLoginType == "Student") {
                                        // Student Login - email + password
                                        mainViewModel.login(email, password) { success, error ->
                                            isLoading = false
                                            if (success) {
                                                onLoginClick()
                                            } else {
                                                errorMessage = error ?: "Invalid credentials"
                                            }
                                        }
                                    } else {
                                        // Admin Login - two step process
                                        if (adminLoginStep == "email") {
                                            // Step 1: Submit email only
                                            mainViewModel.adminEmailLogin(email) { result ->
                                                isLoading = false
                                                when (result.status) {
                                                    "requires_password" -> {
                                                        // Admin exists with password - show password field
                                                        adminLoginStep = "password"
                                                        errorMessage = null
                                                    }
                                                    "pending_approval" -> {
                                                        errorMessage = "Your admin access request is pending approval. Please wait for super admin to approve."
                                                    }
                                                    "rejected" -> {
                                                        errorMessage = "Your previous request was rejected. Contact super admin."
                                                    }
                                                    "request_created" -> {
                                                        errorMessage = "Request submitted! Super admin will review and set your password. Check back later."
                                                    }
                                                    else -> {
                                                        errorMessage = result.message ?: "Unknown error"
                                                    }
                                                }
                                            }
                                        } else {
                                            // Step 2: Submit password
                                            mainViewModel.adminPasswordLogin(email, password) { success, error ->
                                                isLoading = false
                                                if (success) {
                                                    onLoginClick()
                                                } else {
                                                    errorMessage = error ?: "Invalid password"
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(buttonScale),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                disabledContainerColor = Color.LightGray
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                val buttonText = when {
                                    selectedLoginType == "Student" -> "Sign In as Student"
                                    adminLoginStep == "email" -> "Submit Email for Access"
                                    else -> "Sign In as Admin"
                                }
                                Text(
                                    text = buttonText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "  OR  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sign Up Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = Color.Gray
                            )
                            TextButton(onClick = onSignUpClick) {
                                Text(
                                    text = "Sign Up",
                                    color = Color.Black,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
