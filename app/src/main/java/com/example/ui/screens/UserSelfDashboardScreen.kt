package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.core.security.CryptoSecurity
import com.example.data.export.PdfAndExportEngine
import com.example.data.export.PdfFieldSelection
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.PimsAccount
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.model.WebsiteSettings
import com.example.ui.components.AuthPermissionDiagnosticCard
import com.example.ui.components.BengaliFontSelectorCard
import com.example.ui.components.CategoryFilterChipRow
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.PersonalIdChip
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileSearchBar
import com.example.ui.components.SearchCategoryOption
import com.example.ui.components.SearchablePickerField
import com.example.ui.theme.BengaliFontOption

@Composable
fun UserSelfDashboardScreen(
    account: PimsAccount,
    settings: WebsiteSettings,
    diagnostics: AuthPermissionDiagnostics,
    lang: AppLang,
    bengaliFont: BengaliFontOption = BengaliFontOption.HIND_SILIGURI,
    onChangeBengaliFont: (BengaliFontOption) -> Unit = {},
    onEditOwnProfile: (() -> Unit)?,
    onChangePassword: (String, String) -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onLogout: () -> Unit,
    onReportBanner: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var showChangePassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = AppStrings.pick(
                        lang,
                        "স্বাগতম, ${account.fullNameBn}",
                        "Welcome, ${account.fullNameEn.ifBlank { account.fullNameBn }}"
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (lang == AppLang.BN) settings.welcomeMessageBn else settings.welcomeMessageEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(account.photoBase64, account.fullNameBn, sizeDp = 72)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(account.fullNameBn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (account.fullNameEn.isNotBlank()) {
                        Text(account.fullNameEn, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersonalIdChip(account.personalId)
                        if (account.bloodGroup.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                Text(
                                    account.bloodGroup,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    PdfAndExportEngine.generateProfilesPdf(
                        context = context,
                        accounts = listOf(account),
                        settings = settings,
                        selection = PdfFieldSelection(),
                        lang = lang,
                        fileNamePrefix = "my_profile_${account.personalId}",
                        bengaliFontResId = bengaliFont.fontResId
                    ).onSuccess { file ->
                        PdfAndExportEngine.shareExportFile(context, file, "application/pdf", "My Profile PDF")
                    }.onFailure {
                        onReportBanner(true, it.message ?: "PDF Error")
                    }
                },
                modifier = Modifier.weight(1f).testTag("self_download_pdf_button")
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.pick(lang, "PDF ডাউনলোড", "Download PDF"))
            }
            OutlinedButton(
                onClick = {
                    PdfAndExportEngine.generateProfilesPdf(
                        context = context,
                        accounts = listOf(account),
                        settings = settings,
                        selection = PdfFieldSelection(),
                        lang = lang,
                        fileNamePrefix = "print_${account.personalId}",
                        bengaliFontResId = bengaliFont.fontResId
                    ).onSuccess { file ->
                        PdfAndExportEngine.printPdfFile(context, file, "PIMS_${account.personalId}")
                    }.onFailure {
                        onReportBanner(true, it.message ?: "Print Error")
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.pick(lang, "প্রিন্ট", "Print"))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showChangePassword = true },
                modifier = Modifier.weight(1f).testTag("self_change_password_button")
            ) {
                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.pick(lang, "পাসওয়ার্ড পরিবর্তন", "Change Password"))
            }
            if (onEditOwnProfile != null) {
                OutlinedButton(
                    onClick = onEditOwnProfile,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.pick(lang, "সম্পাদনা", "Edit Profile"))
                }
            }
        }

        var profileQuery by remember(account.id) { mutableStateOf("") }
        var profileCategory by remember(account.id) { mutableStateOf("") }
        val profileCategories = remember(lang) {
            listOf(
                SearchCategoryOption("", AppStrings.pick(lang, "সকল তথ্য", "All")),
                SearchCategoryOption("BASIC", AppStrings.pick(lang, "ব্যক্তিগত", "Personal")),
                SearchCategoryOption("EDU", AppStrings.pick(lang, "শিক্ষা", "Education")),
                SearchCategoryOption("ORG", AppStrings.pick(lang, "সাংগঠনিক", "Org")),
                SearchCategoryOption("ADDR", AppStrings.pick(lang, "ঠিকানা", "Address"))
            )
        }

        ProfileSearchBar(
            query = profileQuery,
            onQueryChange = { profileQuery = it },
            placeholder = AppStrings.pick(lang, "প্রোফাইলের তথ্য বা ক্যাটাগরি খুঁজুন...", "Filter profile info by name or category..."),
            lang = lang,
            testTag = "self_profile_search"
        )
        CategoryFilterChipRow(
            categories = profileCategories,
            selectedKey = profileCategory,
            onSelectCategory = { profileCategory = it },
            testTagPrefix = "self_category"
        )

        val selfSections = listOf(
            Triple(
                "BASIC",
                AppStrings.pick(lang, "ক. ব্যক্তিগত তথ্য", "A. Personal Information"),
                listOf(
                    AppStrings.pick(lang, "পিতার নাম", "Father's Name") to account.fatherName,
                    AppStrings.pick(lang, "মাতার নাম", "Mother's Name") to account.motherName,
                    AppStrings.pick(lang, "জন্ম তারিখ", "Date of Birth") to account.dateOfBirth,
                    AppStrings.pick(lang, "লিঙ্গ", "Gender") to account.gender,
                    AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group") to account.bloodGroup,
                    AppStrings.pick(lang, "মোবাইল", "Mobile") to account.mobileNumber,
                    AppStrings.pick(lang, "ইমেইল", "Email") to account.emailAddress
                )
            ),
            Triple(
                "EDU",
                AppStrings.pick(lang, "খ. শিক্ষাগত তথ্য", "B. Educational Information"),
                listOf(
                    AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান", "Institution") to account.institutionName,
                    AppStrings.pick(lang, "শ্রেণি / বর্ষ", "Class / Year") to account.currentClassOrYear,
                    AppStrings.pick(lang, "যোগ্যতা", "Qualification") to account.educationalQualification,
                    AppStrings.pick(lang, "বিভাগ / বিষয়", "Department") to account.departmentOrSubject
                )
            ),
            Triple(
                "ORG",
                AppStrings.pick(lang, "গ. সাংগঠনিক তথ্য", "C. Organizational Information"),
                listOf(
                    AppStrings.pick(lang, "স্তর", "Level") to account.orgLevel,
                    AppStrings.pick(lang, "পদবী", "Designation") to account.orgDesignation,
                    AppStrings.pick(lang, "যোগদান", "Joined") to account.orgJoiningDate,
                    AppStrings.pick(lang, "অবস্থা", "Status") to account.orgStatus
                )
            ),
            Triple(
                "ADDR",
                AppStrings.pick(lang, "ঘ. ঠিকানা", "D. Address"),
                listOf(
                    AppStrings.pick(lang, "পূর্ণ ঠিকানা", "Full Address") to account.fullAddressLabel()
                )
            )
        )

        val pq = profileQuery.trim().lowercase()
        val visibleSections = selfSections.mapNotNull { (catKey, title, rows) ->
            if (profileCategory.isNotBlank() && profileCategory != catKey) return@mapNotNull null
            val filteredRows = if (pq.isBlank()) {
                rows
            } else {
                val titleMatches = title.lowercase().contains(pq)
                if (titleMatches) rows
                else rows.filter { (label, value) ->
                    value.isNotBlank() && (label.lowercase().contains(pq) || value.lowercase().contains(pq))
                }
            }
            if (pq.isNotBlank() && filteredRows.none { it.second.isNotBlank() }) null
            else Triple(catKey, title, filteredRows)
        }

        if (visibleSections.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    text = AppStrings.pick(lang, "কোনো মিল থাকা তথ্য পাওয়া যায়নি।", "No matching profile information found."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        } else {
            visibleSections.forEach { (_, title, rows) ->
                ProfileSectionCard(title = title, rows = rows)
            }
        }

        BengaliFontSelectorCard(
            selectedFont = bengaliFont,
            lang = lang,
            onSelectFont = onChangeBengaliFont,
            testTagPrefix = "self_font"
        )

        var showDiagnostics by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showDiagnostics = !showDiagnostics },
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("self_toggle_diagnostics_button")
        ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (showDiagnostics) {
                    AppStrings.pick(lang, "Auth ও Permission ডায়াগনস্টিকস লুকান", "Hide Auth & Permission Diagnostics")
                } else {
                    AppStrings.pick(lang, "Auth ও Permission ডায়াগনস্টিকস দেখুন", "View Auth & Permission Diagnostics")
                }
            )
        }

        AnimatedVisibility(visible = showDiagnostics) {
            AuthPermissionDiagnosticCard(
                diagnostics = diagnostics,
                lang = lang,
                onRefresh = onRefreshDiagnostics,
                testTagPrefix = "self_diag"
            )
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().testTag("self_logout_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(AppStrings.pick(lang, "লগআউট করুন", "Log Out"))
        }

        Text(
            text = settings.footerText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            lang = lang,
            mandatoryFirstLogin = false,
            onDismiss = { showChangePassword = false },
            onSubmit = { cur, next ->
                showChangePassword = false
                onChangePassword(cur, next)
            }
        )
    }
}

@Composable
fun EncryptedVaultScreen(
    entries: List<VaultEntry>,
    lang: AppLang,
    onSaveEntry: (VaultCategory, String, String, String) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var decryptTarget by remember { mutableStateOf<VaultEntry?>(null) }
    var decryptedPlaintext by remember { mutableStateOf<String?>(null) }
    var vaultQuery by remember { mutableStateOf("") }
    var vaultCategory by remember { mutableStateOf("") }

    val vaultCategories = remember(lang) {
        listOf(
            SearchCategoryOption("", AppStrings.pick(lang, "সকল", "All")),
            SearchCategoryOption(VaultCategory.CREDENTIAL.name, AppStrings.vaultCategoryLabel(lang, VaultCategory.CREDENTIAL.name)),
            SearchCategoryOption(VaultCategory.DOCUMENT.name, AppStrings.vaultCategoryLabel(lang, VaultCategory.DOCUMENT.name)),
            SearchCategoryOption(VaultCategory.PRIVATE_NOTE.name, AppStrings.vaultCategoryLabel(lang, VaultCategory.PRIVATE_NOTE.name))
        )
    }

    val filteredEntries = remember(entries, vaultQuery, vaultCategory) {
        val q = vaultQuery.trim().lowercase()
        entries.filter { entry ->
            val matchesCat = vaultCategory.isBlank() || entry.category == vaultCategory
            val matchesQ = q.isBlank() ||
                entry.title.lowercase().contains(q) ||
                AppStrings.vaultCategoryLabel(AppLang.BN, entry.category).lowercase().contains(q) ||
                AppStrings.vaultCategoryLabel(AppLang.EN, entry.category).lowercase().contains(q)
            matchesCat && matchesQ
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_vault_entry")
            ) {
                Icon(Icons.Filled.Add, contentDescription = AppStrings.pick(lang, "ভল্টে যোগ করুন", "Add Vault Item"))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.EnhancedEncryption,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "এন্ড-টু-এন্ড এনক্রিপ্টেড ব্যক্তিগত ভল্ট (AES-256-GCM): আপনার পাসফ্রেজ ছাড়া এই ক্রেডেনশিয়াল, নথি ও নোট ডিক্রিপ্ট করা সম্ভব নয়।",
                            "End-to-End Encrypted Vault (AES-256-GCM): Only your vault passphrase can decrypt these credentials, documents, and private notes."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            if (entries.isNotEmpty()) {
                ProfileSearchBar(
                    query = vaultQuery,
                    onQueryChange = { vaultQuery = it },
                    placeholder = AppStrings.pick(lang, "ভল্ট ডকুমেন্ট খুঁজুন (নাম বা ধরন)...", "Search vault items by name or category..."),
                    lang = lang,
                    testTag = "vault_search_input"
                )
                CategoryFilterChipRow(
                    categories = vaultCategories,
                    selectedKey = vaultCategory,
                    onSelectCategory = { vaultCategory = it },
                    testTagPrefix = "vault_category"
                )
            }

            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.VpnKey,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (entries.isEmpty()) {
                            AppStrings.pick(lang, "ভল্টে এখনো কোনো গোপন তথ্য সংরক্ষিত নেই।", "Your encrypted vault is empty.")
                        } else {
                            AppStrings.pick(lang, "এই নাম বা ক্যাটাগরিতে কোনো ভল্ট আইটেম পাওয়া যায়নি।", "No vault items match your search or category.")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        AppStrings.vaultCategoryLabel(lang, entry.category),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    decryptedPlaintext = null
                                    decryptTarget = entry
                                }) {
                                    Icon(Icons.Filled.LockOpen, contentDescription = "Decrypt")
                                }
                                IconButton(onClick = { onDeleteEntry(entry.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        var category by remember { mutableStateOf(VaultCategory.CREDENTIAL) }
        var title by remember { mutableStateOf("") }
        var secret by remember { mutableStateOf("") }
        var passphrase by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(AppStrings.pick(lang, "এনক্রিপ্টেড তথ্য যোগ করুন", "Add Encrypted Item")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "ধরন", "Category"),
                        selectedLabel = AppStrings.vaultCategoryLabel(lang, category.name),
                        items = VaultCategory.entries,
                        itemLabel = { AppStrings.vaultCategoryLabel(lang, it.name) },
                        onSelect = { category = it }
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(AppStrings.pick(lang, "শিরোনাম *", "Title *")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = secret,
                        onValueChange = { secret = it },
                        label = { Text(AppStrings.pick(lang, "গোপন তথ্য / পাসওয়ার্ড / নোট *", "Secret Content *")) },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(AppStrings.pick(lang, "এনক্রিপশন পাসফ্রেজ *", "Encryption Passphrase *")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && secret.isNotBlank() && passphrase.isNotBlank()) {
                            onSaveEntry(category, title, secret, passphrase)
                            showAddDialog = false
                        }
                    }
                ) { Text(AppStrings.pick(lang, "এনক্রিপ্ট ও সংরক্ষণ", "Encrypt & Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) }
            }
        )
    }

    if (decryptTarget != null) {
        val target = decryptTarget!!
        var passphrase by remember { mutableStateOf("") }
        var decryptErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { decryptTarget = null },
            title = { Text(target.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (decryptedPlaintext == null) {
                        OutlinedTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it },
                            label = { Text(AppStrings.pick(lang, "ডিক্রিপশন পাসফ্রেজ দিন", "Enter Vault Passphrase")) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        if (decryptErr != null) {
                            Text(decryptErr!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = decryptedPlaintext!!,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (decryptedPlaintext == null) {
                    Button(onClick = {
                        CryptoSecurity.decryptAesGcm(
                            target.ciphertextBase64,
                            target.ivBase64,
                            target.saltBase64,
                            passphrase
                        ).onSuccess { plain ->
                            decryptedPlaintext = plain
                            decryptErr = null
                        }.onFailure {
                            decryptErr = AppStrings.pick(lang, "ভুল পাসফ্রেজ! ডিক্রিপ্ট ব্যর্থ হয়েছে।", "Wrong passphrase. Decryption failed.")
                        }
                    }) { Text(AppStrings.pick(lang, "আনলক করুন", "Decrypt")) }
                } else {
                    Button(onClick = { decryptTarget = null }) { Text(AppStrings.pick(lang, "বন্ধ করুন", "Close")) }
                }
            },
            dismissButton = {
                if (decryptedPlaintext == null) {
                    TextButton(onClick = { decryptTarget = null }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) }
                }
            }
        )
    }
}
