package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutGoogleWorkspace
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.model.PimsAccount
import com.example.data.model.UiState
import com.example.data.preferences.UiPreferences
import com.example.data.preferences.UserPreferencesStore
import com.example.data.repository.PimsRepository
import com.example.ui.components.BrandTopBar
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.StatusNotificationBanner
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DataManagementScreen
import com.example.ui.screens.EncryptedVaultScreen
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.PimsAuthScreen
import com.example.ui.screens.SettingsAndBackupScreen
import com.example.ui.screens.UserDetailScreen
import com.example.ui.screens.UserFormScreen
import com.example.ui.screens.UserSelfDashboardScreen
import com.example.ui.screens.UsersDirectoryScreen
import com.example.ui.theme.BengaliFontOption
import com.example.ui.theme.ByaktigatoTheme
import com.example.ui.viewmodel.PimsViewModel
import com.example.ui.viewmodel.SearchFilterState
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            if (FirebaseApp.getApps(applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(applicationContext)
            }
        }
        enableEdgeToEdge()
        setContent {
            PimsAppRoot()
        }
    }
}

private enum class NavDest(val tag: String, val icon: ImageVector) {
    DASHBOARD("nav_dashboard", Icons.Filled.Dashboard),
    USERS("nav_users", Icons.Filled.Group),
    DATA("nav_data", Icons.Filled.Map),
    SETTINGS("nav_settings", Icons.Filled.Settings),
    SELF_PROFILE("nav_self_profile", Icons.Filled.Person),
    VAULT("nav_vault", Icons.Filled.EnhancedEncryption)
}

private sealed interface SubScreenState {
    data object None : SubScreenState
    data class Detail(val accountId: String) : SubScreenState
    data class Form(val editAccountId: String?) : SubScreenState
}

@Composable
fun PimsAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsStore = remember { UserPreferencesStore(context.applicationContext) }
    val uiPrefs by prefsStore.preferences.collectAsStateWithLifecycle(initialValue = UiPreferences())
    val systemDark = isSystemInDarkTheme()
    val isDark = uiPrefs.darkMode ?: systemDark

    val firebaseAuth = remember(context) {
        runCatching {
            if (FirebaseApp.getApps(context.applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext)
            }
            Firebase.auth
        }.getOrNull()
    }

    if (firebaseAuth == null) {
        ByaktigatoTheme(darkTheme = isDark, bengaliFont = uiPrefs.bengaliFont) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.pick(
                        uiPrefs.lang,
                        "Firebase কনফিগারেশন চালু করা যায়নি। অনুগ্রহ করে অ্যাপটি বন্ধ করে আবার খুলুন।",
                        "Unable to initialize Firebase configuration. Please restart the app."
                    )
                )
            }
        }
        return
    }

    val currentUser by firebaseAuth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)

    val onToggleLang: () -> Unit = {
        scope.launch {
            val next = if (uiPrefs.lang == AppLang.BN) AppLang.EN else AppLang.BN
            prefsStore.setLanguage(next)
        }
    }
    val onToggleTheme: () -> Unit = {
        scope.launch {
            prefsStore.setDarkMode(!isDark)
        }
    }

    val onChangeBengaliFont: (BengaliFontOption) -> Unit = { font ->
        scope.launch {
            prefsStore.setBengaliFont(font)
        }
    }

    val user = currentUser
    if (user == null) {
        ByaktigatoTheme(darkTheme = isDark, bengaliFont = uiPrefs.bengaliFont) {
            GoogleAuthScreen(
                lang = uiPrefs.lang,
                darkTheme = isDark,
                onToggleLang = onToggleLang,
                onToggleTheme = onToggleTheme,
                onAuthSuccess = {}
            )
        }
    } else {
        key(user.uid) {
            AuthenticatedWorkspaceHost(
                currentUserId = user.uid,
                googleEmail = user.email,
                lang = uiPrefs.lang,
                darkTheme = isDark,
                bengaliFont = uiPrefs.bengaliFont,
                onToggleLang = onToggleLang,
                onToggleTheme = onToggleTheme,
                onChangeBengaliFont = onChangeBengaliFont
            )
        }
    }
}

@Composable
private fun AuthenticatedWorkspaceHost(
    currentUserId: String,
    googleEmail: String?,
    lang: AppLang,
    darkTheme: Boolean,
    bengaliFont: BengaliFontOption,
    onToggleLang: () -> Unit,
    onToggleTheme: () -> Unit,
    onChangeBengaliFont: (BengaliFontOption) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: PimsViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                PimsViewModel(
                    repository = PimsRepository(db),
                    currentUserId = currentUserId,
                    geoRepository = com.example.data.geo.SundarganjGeoRepository(app)
                )
            }
        }
    )

    val bootstrapDone by viewModel.bootstrapDone.collectAsStateWithLifecycle()
    val settings by viewModel.websiteSettings.collectAsStateWithLifecycle()
    val accountsState by viewModel.accountsState.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activePimsAccount.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val mergedGeoNodes by viewModel.mergedGeoNodes.collectAsStateWithLifecycle()
    val unionGeoSummaries by viewModel.unionGeoSummaries.collectAsStateWithLifecycle()
    val institutions by viewModel.institutions.collectAsStateWithLifecycle()
    val orgPositions by viewModel.orgPositions.collectAsStateWithLifecycle()
    val vaultEntries by viewModel.vaultEntries.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val filterState by viewModel.searchFilter.collectAsStateWithLifecycle()
    val banner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()
    val authMessage by viewModel.authMessage.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()

    val onSignOutGoogle: () -> Unit = {
        viewModel.logoutPimsSession()
        signOutGoogleWorkspace(credentialManager, scope)
    }

    ByaktigatoTheme(
        darkTheme = darkTheme,
        primaryHex = settings.primaryColorHex,
        accentHex = settings.accentColorHex,
        bengaliFont = bengaliFont
    ) {
        if (!bootstrapDone) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ByaktigatoTheme
        }

        val allAccounts = (accountsState as? UiState.Success)?.data ?: emptyList()
        val isLoadingAccounts = accountsState is UiState.Loading
        val accountsError = (accountsState as? UiState.Error)?.message

        val sessionAccount = activeAccount
        if (sessionAccount == null) {
            PimsAuthScreen(
                settings = settings,
                hasExistingAccounts = allAccounts.isNotEmpty(),
                isLoadingAccounts = isLoadingAccounts,
                accountsError = accountsError,
                authBusy = authBusy,
                authMessage = authMessage,
                diagnostics = diagnostics,
                lang = lang,
                darkTheme = darkTheme,
                googleEmail = googleEmail,
                onToggleLang = onToggleLang,
                onToggleTheme = onToggleTheme,
                onLogin = { pid, pass -> viewModel.loginWithPersonalId(pid, pass) },
                onInitializeSuperAdmin = { nBn, nEn, pid, pass, mob, mail ->
                    viewModel.initializeSuperAdmin(nBn, nEn, pid, pass, mob, mail)
                },
                onRetryAccounts = { viewModel.retryBootstrapAndRefresh() },
                onRefreshDiagnostics = { viewModel.refreshReadOnlyDiagnostics() },
                onSignOutGoogle = onSignOutGoogle
            )
            return@ByaktigatoTheme
        }

        val role = sessionAccount.roleEnum()
        val destinations = remember(role) {
            if (role.canAccessManagement()) {
                listOf(NavDest.DASHBOARD, NavDest.USERS, NavDest.DATA, NavDest.SETTINGS, NavDest.VAULT)
            } else {
                listOf(NavDest.SELF_PROFILE, NavDest.VAULT)
            }
        }

        var selectedTabIndex by remember(sessionAccount.id) { mutableIntStateOf(0) }
        val safeTabIndex = selectedTabIndex.coerceIn(0, destinations.lastIndex)
        val currentDest = destinations[safeTabIndex]
        var subScreen by remember(sessionAccount.id) { mutableStateOf<SubScreenState>(SubScreenState.None) }

        val destLabel: (NavDest) -> String = { d ->
            when (d) {
                NavDest.DASHBOARD -> AppStrings.pick(lang, "ড্যাশবোর্ড", "Dashboard")
                NavDest.USERS -> AppStrings.pick(lang, "ব্যবহারকারী", "Users")
                NavDest.DATA -> AppStrings.pick(lang, "ডেটাবেস", "Geo/Schools")
                NavDest.SETTINGS -> AppStrings.pick(lang, "সেটিংস", "Settings")
                NavDest.SELF_PROFILE -> AppStrings.pick(lang, "আমার প্রোফাইল", "My Profile")
                NavDest.VAULT -> AppStrings.pick(lang, "সুরক্ষিত ভল্ট", "Vault")
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                BrandTopBar(
                    settings = settings,
                    lang = lang,
                    darkTheme = darkTheme,
                    roleLabel = "${sessionAccount.personalId} • ${AppStrings.roleLabel(lang, sessionAccount.role)}",
                    onToggleLang = onToggleLang,
                    onToggleTheme = onToggleTheme,
                    onLogout = { viewModel.logoutPimsSession() }
                )
            },
            bottomBar = {
                BoxWithConstraints {
                    if (maxWidth < 600.dp && subScreen is SubScreenState.None) {
                        NavigationBar {
                            destinations.forEachIndexed { index, dest ->
                                NavigationBarItem(
                                    selected = safeTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    icon = { Icon(dest.icon, contentDescription = destLabel(dest)) },
                                    label = { Text(destLabel(dest), maxLines = 1) },
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val useRail = maxWidth >= 600.dp
                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail && subScreen is SubScreenState.None) {
                        NavigationRail(modifier = Modifier.fillMaxHeight()) {
                            destinations.forEachIndexed { index, dest ->
                                NavigationRailItem(
                                    selected = safeTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    icon = { Icon(dest.icon, contentDescription = destLabel(dest)) },
                                    label = { Text(destLabel(dest)) },
                                    modifier = Modifier.testTag(dest.tag)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        StatusNotificationBanner(banner = banner, onDismiss = { viewModel.clearBanner() })

                        when (val sub = subScreen) {
                            is SubScreenState.Detail -> {
                                val target = allAccounts.firstOrNull { it.id == sub.accountId }
                                if (target == null || !role.canViewAccount(target, sessionAccount.personalId, sessionAccount.assignedRecordIds)) {
                                    subScreen = SubScreenState.None
                                } else {
                                    UserDetailScreen(
                                        actor = sessionAccount,
                                        target = target,
                                        settings = settings,
                                        lang = lang,
                                        onBack = { subScreen = SubScreenState.None },
                                        onEdit = { subScreen = SubScreenState.Form(target.id) },
                                        onToggleActive = { viewModel.toggleUserActive(target) },
                                        onResetPassword = { newPass -> viewModel.resetUserPassword(target, newPass) },
                                        onDelete = {
                                            viewModel.deleteUser(target) {
                                                subScreen = SubScreenState.None
                                            }
                                        },
                                        onReportBanner = { err, msg -> viewModel.reportStatus(err, msg) }
                                    )
                                }
                            }

                            is SubScreenState.Form -> {
                                val editTarget = sub.editAccountId?.let { id -> allAccounts.firstOrNull { it.id == id } }
                                UserFormScreen(
                                    actor = sessionAccount,
                                    existing = editTarget,
                                    locations = locations,
                                    mergedGeoNodes = mergedGeoNodes,
                                    institutions = institutions,
                                    orgPositions = orgPositions,
                                    settings = settings,
                                    lang = lang,
                                    onGenerateId = { prefix -> viewModel.generateNextPersonalId(prefix) },
                                    onQuickAddInstitution = { inst -> viewModel.saveInstitution(inst) },
                                    onSave = { isNew, draft, initPass ->
                                        viewModel.saveUserProfile(isNew, draft, initPass) {
                                            subScreen = SubScreenState.None
                                        }
                                    },
                                    onCancel = { subScreen = SubScreenState.None }
                                )
                            }

                            SubScreenState.None -> {
                                when (currentDest) {
                                    NavDest.DASHBOARD -> {
                                        AdminDashboardScreen(
                                            stats = stats,
                                            accountsState = accountsState,
                                            settings = settings,
                                            activeAccount = sessionAccount,
                                            lang = lang,
                                            onSearchProfiles = { q, cat ->
                                                viewModel.filterVisibleAccounts(
                                                    allAccounts,
                                                    sessionAccount,
                                                    SearchFilterState(query = q, categoryFilter = cat)
                                                )
                                            },
                                            onOpenDirectoryWithFilter = { q, cat ->
                                                viewModel.updateSearchFilter {
                                                    it.copy(query = q, categoryFilter = cat, pageIndex = 0)
                                                }
                                                selectedTabIndex = destinations.indexOf(NavDest.USERS)
                                            },
                                            onCreateUser = { subScreen = SubScreenState.Form(null) },
                                            onOpenDirectory = { selectedTabIndex = destinations.indexOf(NavDest.USERS) },
                                            onOpenLocations = { selectedTabIndex = destinations.indexOf(NavDest.DATA) },
                                            onOpenExport = { selectedTabIndex = destinations.indexOf(NavDest.SETTINGS) },
                                            onViewProfile = { acc -> subScreen = SubScreenState.Detail(acc.id) },
                                            onRetry = { viewModel.retryBootstrapAndRefresh() }
                                        )
                                    }

                                    NavDest.USERS -> {
                                        val visibleAccounts = remember(allAccounts, sessionAccount, filterState) {
                                            viewModel.filterVisibleAccounts(allAccounts, sessionAccount)
                                        }
                                        UsersDirectoryScreen(
                                            activeAccount = sessionAccount,
                                            filteredAccounts = visibleAccounts,
                                            filterState = filterState,
                                            locations = locations,
                                            institutions = institutions,
                                            orgPositions = orgPositions,
                                            settings = settings,
                                            lang = lang,
                                            onUpdateFilter = { viewModel.updateSearchFilter(it) },
                                            onCreateUser = { subScreen = SubScreenState.Form(null) },
                                            onSelectAccount = { acc -> subScreen = SubScreenState.Detail(acc.id) },
                                            onReportBanner = { err, msg -> viewModel.reportStatus(err, msg) }
                                        )
                                    }

                                    NavDest.DATA -> {
                                        DataManagementScreen(
                                            actor = sessionAccount,
                                            locations = locations,
                                            mergedGeoNodes = mergedGeoNodes,
                                            unionGeoSummaries = unionGeoSummaries,
                                            geoMeta = viewModel.geoDatasetMeta,
                                            institutions = institutions,
                                            orgPositions = orgPositions,
                                            lang = lang,
                                            onSaveLocation = { viewModel.saveLocation(it) },
                                            onAssignVillageToWard = { village, wardId, wardCode ->
                                                viewModel.assignVillageToWard(village, wardId, wardCode)
                                            },
                                            onExportGeoData = { asCsv ->
                                                viewModel.exportGeographyDataset(context, asCsv, lang)
                                            },
                                            onDeleteLocation = { viewModel.deleteLocation(it) },
                                            onImportLocationsCsv = { viewModel.importLocationsCsv(it) },
                                            onSaveInstitution = { viewModel.saveInstitution(it) },
                                            onDeleteInstitution = { viewModel.deleteInstitution(it) },
                                            onImportInstitutionsCsv = { viewModel.importInstitutionsCsv(it) },
                                            onSaveOrgPosition = { viewModel.saveOrgPosition(it) },
                                            onDeleteOrgPosition = { viewModel.deleteOrgPosition(it) }
                                        )
                                    }

                                    NavDest.SETTINGS -> {
                                        SettingsAndBackupScreen(
                                            actor = sessionAccount,
                                            allAccounts = allAccounts,
                                            settings = settings,
                                            logs = activityLogs,
                                            diagnostics = diagnostics,
                                            lang = lang,
                                            bengaliFont = bengaliFont,
                                            onChangeBengaliFont = onChangeBengaliFont,
                                            onSaveSettings = { viewModel.saveWebsiteSettings(it) },
                                            onChangeOwnPassword = { cur, next -> viewModel.changeOwnPassword(cur, next) },
                                            onRefreshDiagnostics = { viewModel.refreshReadOnlyDiagnostics() },
                                            onSignOutGoogle = onSignOutGoogle,
                                            onReportBanner = { err, msg -> viewModel.reportStatus(err, msg) }
                                        )
                                    }

                                    NavDest.SELF_PROFILE -> {
                                        UserSelfDashboardScreen(
                                            account = sessionAccount,
                                            settings = settings,
                                            diagnostics = diagnostics,
                                            lang = lang,
                                            bengaliFont = bengaliFont,
                                            onChangeBengaliFont = onChangeBengaliFont,
                                            onEditOwnProfile = if (sessionAccount.canEditSelfProfile) {
                                                { subScreen = SubScreenState.Form(sessionAccount.id) }
                                            } else null,
                                            onChangePassword = { cur, next -> viewModel.changeOwnPassword(cur, next) },
                                            onRefreshDiagnostics = { viewModel.refreshReadOnlyDiagnostics() },
                                            onLogout = { viewModel.logoutPimsSession() },
                                            onReportBanner = { err, msg -> viewModel.reportStatus(err, msg) }
                                        )
                                    }

                                    NavDest.VAULT -> {
                                        EncryptedVaultScreen(
                                            entries = vaultEntries,
                                            lang = lang,
                                            onSaveEntry = { cat, title, secret, pass ->
                                                viewModel.saveVaultEntry(cat, title, secret, pass)
                                            },
                                            onDeleteEntry = { viewModel.deleteVaultEntry(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sessionAccount.mustChangePassword) {
            ChangePasswordDialog(
                lang = lang,
                mandatoryFirstLogin = true,
                onDismiss = {},
                onSubmit = { cur, next -> viewModel.changeOwnPassword(cur, next) }
            )
        }
    }
}
