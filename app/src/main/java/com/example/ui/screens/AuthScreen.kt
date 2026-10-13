package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentIndigo
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun AuthScreen(
    isAuthenticating: Boolean,
    authError: String?,
    demoUsers: List<UserEntity>,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String, role: String) -> Unit,
    onSocialLogin: (name: String, email: String, role: String, provider: String) -> Unit = { _, _, _, _ -> },
    onResetPassword: (email: String, onResult: (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onQuickLogin: (UserEntity) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var selectedRole by remember { mutableStateOf("EDUCATOR") } // "EDUCATOR" or "STUDENT"

    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    // Dialog states
    var showGoogleDialog by remember { mutableStateOf(false) }
    var showSchoolSsoDialog by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Password strength calculation
    val passwordStrength = remember(passwordInput) {
        calculatePasswordStrength(passwordInput)
    }

    // Common email domain chips
    val emailDomains = listOf("@gmail.com", "@googlemail.com", "@university.edu", "@school.org")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                // Logo & App Name Header
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(listOf(PrimaryBlue, AccentCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "EduPulse Logo",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "EduPulse",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " AI",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Adaptive Classroom & AI-Powered Quiz Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Tab Selector: Sign In vs Register
            item {
                Spacer(modifier = Modifier.height(4.dp))
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            onClearError()
                        },
                        text = {
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier.testTag("tab_sign_in")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            onClearError()
                        },
                        text = {
                            Text(
                                "Create Account",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier.testTag("tab_register")
                    )
                }
            }

            // Role Selection Cards
            item {
                Text(
                    text = if (selectedTab == 1) "Choose Account Role *" else "Role Preference",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Educator Card
                    RoleSelectionCard(
                        roleTitle = "Educator",
                        roleDescription = "Manage classes, upload materials & publish RAG quizzes",
                        icon = Icons.Default.School,
                        isSelected = selectedRole == "EDUCATOR",
                        accentColor = PrimaryBlue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_select_educator"),
                        onClick = { selectedRole = "EDUCATOR" }
                    )

                    // Student Card
                    RoleSelectionCard(
                        roleTitle = "Student",
                        roleDescription = "Join with class code, review flashcards & test mastery",
                        icon = Icons.Default.Person,
                        isSelected = selectedRole == "STUDENT",
                        accentColor = AccentIndigo,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_select_student"),
                        onClick = { selectedRole = "STUDENT" }
                    )
                }
            }

            // Error banner
            if (authError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRose.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRose, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = authError,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ErrorRose,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onClearError, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ErrorRose, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // SECTION: FAST SOCIAL & SSO SIGN-IN (GOOGLE / GMAIL & SCHOOL SSO)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF4285F4).copy(alpha = 0.4f), Color(0xFF34A853).copy(alpha = 0.4f)))),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Quick & Secure Logins",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Badge(containerColor = Color(0xFF4285F4).copy(alpha = 0.15f), contentColor = Color(0xFF1A73E8)) {
                                Text("Recommended", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        // Primary Google / Gmail Sign In Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder(),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clickable(enabled = !isAuthenticating) {
                                    showGoogleDialog = true
                                }
                                .testTag("google_sign_in_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleLogo(modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = if (selectedTab == 0) "Continue with Google / Gmail" else "Sign up with Google / Gmail",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "1-Tap sign in as $selectedRole",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Secondary SSO Options: Microsoft School SSO & Apple ID
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Microsoft / School Email SSO Button
                            OutlinedButton(
                                onClick = { showSchoolSsoDialog = true },
                                enabled = !isAuthenticating,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("microsoft_sso_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                MicrosoftLogo(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "School SSO",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Apple ID Sign In Button
                            OutlinedButton(
                                onClick = {
                                    onSocialLogin("Apple User", "apple.user@icloud.com", selectedRole, "Apple")
                                },
                                enabled = !isAuthenticating,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("apple_sso_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                AppleLogo(modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Apple ID",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Divider: OR CONTINUE WITH EMAIL
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Text(
                        text = "  or continue with email  ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }

            // Form Inputs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Name Field (Only for Registration)
                        AnimatedVisibility(
                            visible = selectedTab == 1,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it
                                    onClearError()
                                },
                                label = { Text("Full Name *") },
                                placeholder = { Text("e.g. Dr. Jordan Reed") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input"),
                                singleLine = true
                            )
                        }

                        // Email Field
                        Column {
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    onClearError()
                                },
                                label = { Text("Email Address *") },
                                placeholder = { Text("e.g. yourname@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue)
                                },
                                trailingIcon = {
                                    if (emailInput.endsWith("@gmail.com")) {
                                        GoogleLogo(modifier = Modifier.size(18.dp))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input"),
                                singleLine = true
                            )

                            // Quick Domain Completion Chips (e.g. for @gmail.com)
                            if (emailInput.isNotBlank() && !emailInput.contains("@")) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    emailDomains.forEach { domain ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.clickable {
                                                emailInput = emailInput.trim() + domain
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (domain.contains("gmail")) {
                                                    GoogleLogo(modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = domain,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                                    color = PrimaryBlue
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Password Field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                onClearError()
                            },
                            label = { Text("Password *") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (selectedTab == 0) {
                                        onLogin(emailInput, passwordInput)
                                    } else {
                                        onRegister(nameInput, emailInput, passwordInput, selectedRole)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input"),
                            singleLine = true
                        )

                        // Password Strength Indicator (For Register Mode)
                        if (selectedTab == 1 && passwordInput.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Password Strength:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = passwordStrength.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = passwordStrength.color
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { passwordStrength.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = passwordStrength.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }

                        // Remember Me & Forgot Password Row (Sign In Mode)
                        if (selectedTab == 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                                ) {
                                    Checkbox(
                                        checked = rememberMe,
                                        onCheckedChange = { rememberMe = it },
                                        modifier = Modifier.testTag("remember_me_checkbox")
                                    )
                                    Text(
                                        text = "Remember me",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = { showForgotDialog = true },
                                    modifier = Modifier.testTag("forgot_password_button")
                                ) {
                                    Text(
                                        text = "Forgot password?",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = PrimaryBlue
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (selectedTab == 0) {
                                    onLogin(emailInput, passwordInput)
                                } else {
                                    onRegister(nameInput, emailInput, passwordInput, selectedRole)
                                }
                            },
                            enabled = !isAuthenticating && (selectedTab == 0 || nameInput.isNotBlank()) && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == "EDUCATOR") PrimaryBlue else AccentIndigo
                            )
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text(
                                    text = if (selectedTab == 0) {
                                        "Sign In as $selectedRole"
                                    } else {
                                        "Create $selectedRole Account"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        // Bottom Switching Link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTab == 0) "Don't have an account? " else "Already have an account? ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (selectedTab == 0) "Sign up" else "Sign in",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryBlue,
                                modifier = Modifier
                                    .clickable {
                                        selectedTab = if (selectedTab == 0) 1 else 0
                                        onClearError()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Quick Demo Profiles (1-Tap Fast Evaluation)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "— OR 1-TAP DEMO PROFILES —",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        demoUsers.take(3).forEach { user ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onQuickLogin(user) }
                                    .testTag("quick_login_${user.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (user.role == "EDUCATOR") PrimaryBlue.copy(alpha = 0.15f) else AccentIndigo.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (user.role == "EDUCATOR") Icons.Default.School else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (user.role == "EDUCATOR") PrimaryBlue else AccentIndigo,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = user.name.split(" ").firstOrNull() ?: user.name,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (user.role == "EDUCATOR") "Educator" else "Student",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (user.role == "EDUCATOR") PrimaryBlue else AccentIndigo
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Trust & Security Footer
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted Authentication • SSL 256-bit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By signing in, you agree to EduPulse AI Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // DIALOG: GOOGLE & GMAIL ACCOUNT CHOOSER
    if (showGoogleDialog) {
        GoogleAccountChooserDialog(
            currentRole = selectedRole,
            onSelectAccount = { name, email, role ->
                showGoogleDialog = false
                onSocialLogin(name, email, role, "Google")
            },
            onDismiss = { showGoogleDialog = false }
        )
    }

    // DIALOG: SCHOOL / MICROSOFT SSO
    if (showSchoolSsoDialog) {
        SchoolSsoDialog(
            currentRole = selectedRole,
            onSelectSchoolAccount = { name, email, role ->
                showSchoolSsoDialog = false
                onSocialLogin(name, email, role, "Microsoft")
            },
            onDismiss = { showSchoolSsoDialog = false }
        )
    }

    // DIALOG: FORGOT PASSWORD RESET
    if (showForgotDialog) {
        ForgotPasswordDialog(
            initialEmail = emailInput,
            onRequestReset = { email, callback ->
                onResetPassword(email, callback)
            },
            onDismiss = { showForgotDialog = false }
        )
    }
}

// -------------------------------------------------------------
// GOOGLE ACCOUNT CHOOSER DIALOG (One-Tap / Gmail Login)
// -------------------------------------------------------------
@Composable
fun GoogleAccountChooserDialog(
    currentRole: String,
    onSelectAccount: (name: String, email: String, role: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isAddingCustomAccount by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customGmail by remember { mutableStateOf("") }
    var customRole by remember { mutableStateOf(currentRole) }

    val presetGoogleAccounts = listOf(
        Triple("Dr. Aris Vance", "aris.vance@gmail.com", "EDUCATOR"),
        Triple("Maya Chen", "maya.chen@gmail.com", "STUDENT"),
        Triple("Leo Patel", "leo.patel@gmail.com", "STUDENT")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GoogleLogo(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Choose an account for EduPulse AI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!isAddingCustomAccount) {
                    Text(
                        text = "Select a connected Google / Gmail account:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    presetGoogleAccounts.forEach { (name, email, role) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAccount(name, email, role)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (role == "EDUCATOR") PrimaryBlue else AccentTeal),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.first().toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Badge(
                                    containerColor = if (role == "EDUCATOR") PrimaryBlue.copy(alpha = 0.15f) else AccentTeal.copy(alpha = 0.15f),
                                    contentColor = if (role == "EDUCATOR") PrimaryBlue else AccentTeal
                                ) {
                                    Text(if (role == "EDUCATOR") "Educator" else "Student", modifier = Modifier.padding(horizontal = 4.dp))
                                }
                            }
                        }
                    }

                    // Add Custom Google Account Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAddingCustomAccount = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Use another Gmail / Google account",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = PrimaryBlue
                            )
                        }
                    }
                } else {
                    // Custom Google Account Input Form
                    Text(
                        text = "Enter your Google / Gmail credentials:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. Alex Rivera") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customGmail,
                        onValueChange = { customGmail = it },
                        label = { Text("Gmail Address") },
                        placeholder = { Text("e.g. alex.rivera@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { customRole = "EDUCATOR" },
                            modifier = Modifier.weight(1f),
                            colors = if (customRole == "EDUCATOR") ButtonDefaults.outlinedButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Educator", color = if (customRole == "EDUCATOR") PrimaryBlue else MaterialTheme.colorScheme.onSurface)
                        }
                        OutlinedButton(
                            onClick = { customRole = "STUDENT" },
                            modifier = Modifier.weight(1f),
                            colors = if (customRole == "STUDENT") ButtonDefaults.outlinedButtonColors(containerColor = AccentIndigo.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Student", color = if (customRole == "STUDENT") AccentIndigo else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAddingCustomAccount = false }) {
                            Text("Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val finalEmail = if (!customGmail.contains("@")) "$customGmail@gmail.com" else customGmail
                                onSelectAccount(
                                    customName.ifBlank { finalEmail.substringBefore("@") },
                                    finalEmail,
                                    customRole
                                )
                            },
                            enabled = customGmail.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                        ) {
                            Text("Continue with Google")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "To continue, Google will share your name, email address, and profile info with EduPulse AI.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// SCHOOL / MICROSOFT SSO DIALOG
// -------------------------------------------------------------
@Composable
fun SchoolSsoDialog(
    currentRole: String,
    onSelectSchoolAccount: (name: String, email: String, role: String) -> Unit,
    onDismiss: () -> Unit
) {
    var schoolEmail by remember { mutableStateOf("") }
    var schoolRole by remember { mutableStateOf(currentRole) }

    val presetSchoolProfiles = listOf(
        Pair("Prof. Marcus Vance (University)", "m.vance@university.edu" to "EDUCATOR"),
        Pair("Maya Chen (Student Union)", "m.chen@campus.edu" to "STUDENT")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MicrosoftLogo(modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "School & Microsoft SSO",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Single Sign-On for campus accounts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Fast 1-tap campus profiles:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                presetSchoolProfiles.forEach { (label, account) ->
                    val (email, role) = account
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSchoolAccount(label.substringBefore(" ("), email, role)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Badge(
                                containerColor = if (role == "EDUCATOR") PrimaryBlue.copy(alpha = 0.15f) else AccentIndigo.copy(alpha = 0.15f),
                                contentColor = if (role == "EDUCATOR") PrimaryBlue else AccentIndigo
                            ) {
                                Text(if (role == "EDUCATOR") "Educator" else "Student", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Or enter your institutional email:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = schoolEmail,
                    onValueChange = { schoolEmail = it },
                    label = { Text("School / University Email") },
                    placeholder = { Text("name@school.edu") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { schoolRole = "EDUCATOR" },
                        modifier = Modifier.weight(1f),
                        colors = if (schoolRole == "EDUCATOR") ButtonDefaults.outlinedButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Educator", color = if (schoolRole == "EDUCATOR") PrimaryBlue else MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedButton(
                        onClick = { schoolRole = "STUDENT" },
                        modifier = Modifier.weight(1f),
                        colors = if (schoolRole == "STUDENT") ButtonDefaults.outlinedButtonColors(containerColor = AccentIndigo.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text("Student", color = if (schoolRole == "STUDENT") AccentIndigo else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEmail = if (!schoolEmail.contains("@")) "$schoolEmail@university.edu" else schoolEmail
                    onSelectSchoolAccount(
                        finalEmail.substringBefore("@").replace(".", " "),
                        finalEmail,
                        schoolRole
                    )
                },
                enabled = schoolEmail.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4))
            ) {
                Text("Sign In with School SSO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// FORGOT PASSWORD DIALOG
// -------------------------------------------------------------
@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onRequestReset: (email: String, callback: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var isSending by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Password", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (resultMessage == null) {
                    Text(
                        text = "Enter your registered email address or Gmail to receive password reset instructions and a 6-digit recovery code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryBlue) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess) SuccessGreen.copy(alpha = 0.12f) else ErrorRose.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isSuccess) SuccessGreen else ErrorRose,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = resultMessage!!,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSuccess) SuccessGreen else ErrorRose
                            )
                        }
                    }

                    if (isSuccess) {
                        Text(
                            text = "Please check your Gmail inbox and spam folders. The link will expire in 15 minutes.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (resultMessage == null) {
                Button(
                    onClick = {
                        isSending = true
                        onRequestReset(email) { success, msg ->
                            isSending = false
                            isSuccess = success
                            resultMessage = msg
                        }
                    },
                    enabled = email.isNotBlank() && !isSending,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Send Reset Link")
                    }
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (resultMessage == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

// -------------------------------------------------------------
// BRAND LOGOS (Google, Microsoft, Apple)
// -------------------------------------------------------------
@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (minOf(w, h) / 2f) * 0.9f
        val strokeW = r * 0.35f

        // Draw Google 4-color arcs
        // Red arc (top / top-left)
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 210f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = strokeW)
        )
        // Yellow arc (bottom-left)
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = strokeW)
        )
        // Green arc (bottom / bottom-right)
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = strokeW)
        )
        // Blue arc (right)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = strokeW)
        )
        // Blue horizontal crossbar
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx - (strokeW * 0.2f), cy - (strokeW / 2f)),
            size = Size(r + (strokeW * 0.2f), strokeW)
        )
    }
}

@Composable
fun MicrosoftLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gap = minOf(w, h) * 0.1f
        val boxW = (w - gap) / 2f
        val boxH = (h - gap) / 2f

        // Top-Left: Red
        drawRect(Color(0xFFF25022), topLeft = Offset(0f, 0f), size = Size(boxW, boxH))
        // Top-Right: Green
        drawRect(Color(0xFF7FBA00), topLeft = Offset(boxW + gap, 0f), size = Size(boxW, boxH))
        // Bottom-Left: Blue
        drawRect(Color(0xFF00A4EF), topLeft = Offset(0f, boxH + gap), size = Size(boxW, boxH))
        // Bottom-Right: Yellow
        drawRect(Color(0xFFFFB900), topLeft = Offset(boxW + gap, boxH + gap), size = Size(boxW, boxH))
    }
}

@Composable
fun AppleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Apple silhouette (Body + Leaf)
        drawCircle(
            color = Color.DarkGray,
            radius = minOf(w, h) * 0.42f,
            center = Offset(cx, cy + (h * 0.05f))
        )
        // Top leaf
        drawCircle(
            color = Color.DarkGray,
            radius = minOf(w, h) * 0.16f,
            center = Offset(cx + (w * 0.12f), cy - (h * 0.38f))
        )
    }
}

// -------------------------------------------------------------
// HELPER MODELS & CALCULATIONS
// -------------------------------------------------------------
data class PasswordStrength(
    val label: String,
    val progress: Float,
    val color: Color
)

fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 4) {
        return PasswordStrength("Very Weak", 0.2f, Color(0xFFEF4444))
    }
    var score = 0
    if (password.length >= 6) score++
    if (password.length >= 10) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() } || password.any { it.isUpperCase() }) score++

    return when (score) {
        0, 1 -> PasswordStrength("Weak", 0.35f, Color(0xFFEF4444))
        2 -> PasswordStrength("Fair", 0.6f, Color(0xFFF59E0B))
        3 -> PasswordStrength("Good", 0.85f, Color(0xFF10B981))
        else -> PasswordStrength("Strong", 1.0f, Color(0xFF059669))
    }
}

@Composable
fun RoleSelectionCard(
    roleTitle: String,
    roleDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) accentColor else Color.Gray.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Badge(containerColor = accentColor, contentColor = Color.White) {
                        Text("Active", modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = roleTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = roleDescription,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
