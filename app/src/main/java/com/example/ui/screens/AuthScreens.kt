package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.export.PdfAndExportEngine
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.DiagnosticCheckStatus
import com.example.data.model.WebsiteSettings
import com.example.ui.components.AuthPermissionDiagnosticCard
import com.example.ui.components.BrandTopBar

@Composable
fun GoogleAuthScreen(
    lang: AppLang,
    darkTheme: Boolean,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Scaffold(
        topBar = {
            BrandTopBar(
                settings = WebsiteSettings(),
                lang = lang,
                darkTheme = darkTheme,
                roleLabel = null,
                onToggleLang = onToggleLang,
                onToggleTheme = onToggleTheme
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_auth_hero),
                    contentDescription = AppStrings.pick(lang, "ব্যক্তিগত তথ্য ব্যানার", "PIMS Hero"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.brand_logo),
                                    contentDescription = AppStrings.pick(lang, "লোগো", "App Logo"),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(2.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text(
                                text = AppStrings.pick(
                                    lang,
                                    "ব্যক্তিগত তথ্য (PIMS)",
                                    "Personal Information Management System"
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = AppStrings.pick(
                                lang,
                                "নিরাপদ ব্যক্তিগত ও প্রাতিষ্ঠানিক প্রোফাইল, বাংলাদেশ লোকেশন ডাটাবেস, বাংলা PDF রিপোর্ট এবং এন্ড-টু-এন্ড এনক্রিপ্টেড ভল্ট সংরক্ষণের জন্য আপনার Google অ্যাকাউন্ট দিয়ে ক্লাউড ওয়ার্কস্পেসে প্রবেশ করুন।",
                                "Sign in with Google to connect your encrypted cloud workspace for personal profiles, Bangladesh location hierarchy, Bengali Unicode PDFs, and secure vault storage."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                onGoogleSignInClicked(
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
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.CloudDone, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Sign in with Google / Google দিয়ে সাইন ইন")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PimsAuthScreen(
    settings: WebsiteSettings,
    hasExistingAccounts: Boolean,
    isLoadingAccounts: Boolean,
    accountsError: String?,
    authBusy: Boolean,
    authMessage: String?,
    diagnostics: AuthPermissionDiagnostics,
    lang: AppLang,
    darkTheme: Boolean,
    googleEmail: String?,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onLogin: (personalId: String, password: String) -> Unit,
    onInitializeSuperAdmin: (
        fullNameBn: String,
        fullNameEn: String,
        customPid: String?,
        password: String,
        mobile: String,
        email: String
    ) -> Unit,
    onRetryAccounts: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onSignOutGoogle: () -> Unit
) {
    Scaffold(
        topBar = {
            BrandTopBar(
                settings = settings,
                lang = lang,
                darkTheme = darkTheme,
                roleLabel = googleEmail,
                onToggleLang = onToggleLang,
                onToggleTheme = onToggleTheme,
                onLogout = onSignOutGoogle
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val headerLogoBmp = remember(settings.logoBase64) {
                    PdfAndExportEngine.decodeBase64Bitmap(settings.logoBase64)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(52.dp)
                        ) {
                            if (headerLogoBmp != null) {
                                Image(
                                    bitmap = headerLogoBmp.asImageBitmap(),
                                    contentDescription = AppStrings.pick(lang, "লোগো", "Logo"),
                                    modifier = Modifier.size(52.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = painterResource(id = R.drawable.brand_logo),
                                    contentDescription = AppStrings.pick(lang, "লোগো", "Logo"),
                                    modifier = Modifier.size(52.dp).padding(2.dp).clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Text(
                            text = if (lang == AppLang.BN) settings.welcomeMessageBn else settings.welcomeMessageEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isLoadingAccounts) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (!accountsError.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.testTag("accounts_error_card")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = AppStrings.pick(
                                        lang,
                                        "ডাটাবেস থেকে অ্যাকাউন্ট তথ্য লোড হয়নি",
                                        "Failed to Load Accounts from Database"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = accountsError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = onRetryAccounts,
                                modifier = Modifier.fillMaxWidth().testTag("retry_accounts_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(AppStrings.pick(lang, "পুনরায় চেষ্টা করুন", "Retry Connection"))
                            }
                        }
                    }
                } else if (!hasExistingAccounts) {
                    if (!diagnostics.isLoading && !diagnostics.adminPermissionGranted) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = AppStrings.pick(
                                    lang,
                                    "সতর্কতা: বর্তমান Google অ্যাকাউন্টে Firestore Admin অনুমতি সক্রিয় নেই। সুপার অ্যাডমিন তৈরি করতে যাচাইকৃত Bootstrapped Super Admin ইমেইল অথবা সক্রিয় /admins/{uid} অনুমতি প্রয়োজন।",
                                    "Notice: This Google account currently lacks Firestore Admin write permission. Verified Bootstrapped Super Admin email or an active /admins/{uid} grant is required to initialize Super Admin."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    SuperAdminBootstrapCard(
                        lang = lang,
                        busy = authBusy,
                        message = authMessage,
                        defaultEmail = googleEmail.orEmpty(),
                        onSubmit = onInitializeSuperAdmin
                    )
                } else {
                    PersonalIdLoginCard(
                        lang = lang,
                        busy = authBusy,
                        message = authMessage,
                        onLogin = onLogin
                    )
                }

                var showDiagnostics by remember {
                    mutableStateOf(
                        diagnostics.bootstrapStatus == DiagnosticCheckStatus.FAILED ||
                            !accountsError.isNullOrBlank()
                    )
                }
                OutlinedButton(
                    onClick = { showDiagnostics = !showDiagnostics },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("auth_toggle_diagnostics_button")
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (showDiagnostics) {
                            AppStrings.pick(lang, "Auth ও Permission ডায়াগনস্টিকস লুকান", "Hide Auth & Permission Diagnostics")
                        } else {
                            AppStrings.pick(lang, "Auth ও Permission ডায়াগনস্টিকস", "Auth & Permission Diagnostics")
                        }
                    )
                }

                AnimatedVisibility(visible = showDiagnostics) {
                    AuthPermissionDiagnosticCard(
                        diagnostics = diagnostics,
                        lang = lang,
                        onRefresh = onRefreshDiagnostics,
                        testTagPrefix = "auth_diag"
                    )
                }

                if (settings.showPrivacyNotice) {
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "গোপনীয়তা ও নিরাপত্তা: কোনো উন্মুক্ত স্ব-নিবন্ধন নেই। শুধুমাত্র অনুমোদিত অ্যাডমিনিস্ট্রেটর নতুন অ্যাকাউন্ট তৈরি করতে পারবেন।",
                            "Privacy & Security: Public registration is disabled. Only authorized administrators can provision accounts."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalIdLoginCard(
    lang: AppLang,
    busy: Boolean,
    message: String?,
    onLogin: (String, String) -> Unit
) {
    var personalId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    AppStrings.pick(lang, "পার্সোনাল আইডি লগইন", "Personal ID Sign-In"),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            OutlinedTextField(
                value = personalId,
                onValueChange = { personalId = it.uppercase() },
                label = { Text(AppStrings.pick(lang, "পার্সোনাল আইডি (যেমন: SA-00001)", "Personal ID")) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_personal_id_input")
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(AppStrings.pick(lang, "পাসওয়ার্ড", "Password")) },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle password"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
            )

            if (!message.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Button(
                onClick = { onLogin(personalId, password) },
                enabled = !busy && personalId.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("pims_login_button")
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.pick(lang, "লগইন করুন", "Log In"))
                }
            }
        }
    }
}

@Composable
private fun SuperAdminBootstrapCard(
    lang: AppLang,
    busy: Boolean,
    message: String?,
    defaultEmail: String,
    onSubmit: (String, String, String?, String, String, String) -> Unit
) {
    var nameBn by remember { mutableStateOf("") }
    var nameEn by remember { mutableStateOf("") }
    var customPid by remember { mutableStateOf("SA-00001") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(defaultEmail) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    AppStrings.pick(lang, "প্রাথমিক সুপার অ্যাডমিন সেটআপ", "Initial Super Admin Setup"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                AppStrings.pick(
                    lang,
                    "আপনার নতুন ডাটাবেসে প্রথম সুপার অ্যাডমিন অ্যাকাউন্ট তৈরি করুন। এরপর শুধুমাত্র অ্যাডমিনরা নতুন ব্যবহারকারী যুক্ত করতে পারবেন।",
                    "Create the master Super Admin account for this workspace. Public registration remains disabled thereafter."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nameBn,
                onValueChange = { nameBn = it },
                label = { Text(AppStrings.pick(lang, "পূর্ণ নাম (বাংলা) *", "Full Name (Bangla) *")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("init_name_bn_input")
            )
            OutlinedTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                label = { Text(AppStrings.pick(lang, "পূর্ণ নাম (ইংরেজি)", "Full Name (English)")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customPid,
                onValueChange = { customPid = it.uppercase() },
                label = { Text(AppStrings.pick(lang, "পার্সোনাল আইডি (কাস্টম বা স্বয়ংক্রিয়)", "Personal ID")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text(AppStrings.pick(lang, "মোবাইল নম্বর", "Mobile Number")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(AppStrings.pick(lang, "ইমেইল", "Email")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(AppStrings.pick(lang, "শক্তিশালী পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর) *", "Strong Password (min 8 chars) *")) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("init_password_input")
            )
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = { Text(AppStrings.pick(lang, "পাসওয়ার্ড নিশ্চিত করুন *", "Confirm Password *")) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("init_confirm_password_input")
            )

            val err = localError ?: message
            if (!err.isNullOrBlank()) {
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    when {
                        nameBn.isBlank() -> localError = AppStrings.pick(lang, "বাংলা নাম আবশ্যক", "Bangla name is required")
                        password.length < 8 -> localError = AppStrings.pick(lang, "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে", "Password must be at least 8 characters")
                        password != confirm -> localError = AppStrings.pick(lang, "পাসওয়ার্ড মিলছে না", "Passwords do not match")
                        else -> {
                            localError = null
                            onSubmit(nameBn, nameEn, customPid.ifBlank { null }, password, mobile, email)
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_super_admin_button")
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(AppStrings.pick(lang, "সুপার অ্যাডমিন তৈরি ও শুরু করুন", "Create Super Admin & Continue"))
            }
        }
    }
}
