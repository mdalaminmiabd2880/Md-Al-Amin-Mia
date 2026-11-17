package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.export.PdfAndExportEngine
import com.example.data.model.ActivityLog
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.PimsAccount
import com.example.data.model.WebsiteSettings
import com.example.ui.components.AuthPermissionDiagnosticCard
import com.example.ui.components.BengaliFontSelectorCard
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.PdfFieldSelectorDialog
import com.example.ui.theme.BengaliFontOption
import com.example.ui.theme.parseBrandColor

@Composable
fun SettingsAndBackupScreen(
    actor: PimsAccount,
    allAccounts: List<PimsAccount>,
    settings: WebsiteSettings,
    logs: List<ActivityLog>,
    diagnostics: AuthPermissionDiagnostics,
    lang: AppLang,
    bengaliFont: BengaliFontOption = BengaliFontOption.HIND_SILIGURI,
    onChangeBengaliFont: (BengaliFontOption) -> Unit = {},
    onSaveSettings: (WebsiteSettings) -> Unit,
    onChangeOwnPassword: (String, String) -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onReportBanner: (Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isSuperAdmin = actor.roleEnum().canManageSettings()

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp
        ) {
            if (isSuperAdmin) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(AppStrings.pick(lang, "ব্র্যান্ডিং ও সেটিংস", "Branding")) }
                )
            }
            Tab(
                selected = selectedTab == if (isSuperAdmin) 1 else 0,
                onClick = { selectedTab = if (isSuperAdmin) 1 else 0 },
                text = { Text(AppStrings.pick(lang, "ব্যাকআপ ও রপ্তানি", "Backup/Export")) }
            )
            Tab(
                selected = selectedTab == if (isSuperAdmin) 2 else 1,
                onClick = { selectedTab = if (isSuperAdmin) 2 else 1 },
                text = { Text(AppStrings.pick(lang, "অডিট লগ", "Audit Logs")) }
            )
            Tab(
                selected = selectedTab == if (isSuperAdmin) 3 else 2,
                onClick = { selectedTab = if (isSuperAdmin) 3 else 2 },
                modifier = Modifier.testTag("settings_diagnostics_tab"),
                text = { Text(AppStrings.pick(lang, "ডায়াগনস্টিকস", "Diagnostics")) }
            )
        }

        val normalizedTab = if (isSuperAdmin) selectedTab else selectedTab + 1
        when (normalizedTab) {
            0 -> WebsiteSettingsTab(
                settings = settings,
                lang = lang,
                onSaveSettings = onSaveSettings
            )
            1 -> BackupAndSecurityTab(
                actor = actor,
                allAccounts = allAccounts,
                settings = settings,
                lang = lang,
                bengaliFont = bengaliFont,
                onChangeBengaliFont = onChangeBengaliFont,
                onChangeOwnPassword = onChangeOwnPassword,
                onSignOutGoogle = onSignOutGoogle,
                onReportBanner = onReportBanner
            )
            2 -> AuditLogsTab(logs = logs, lang = lang)
            else -> DiagnosticsTab(
                diagnostics = diagnostics,
                lang = lang,
                onRefresh = onRefreshDiagnostics
            )
        }
    }
}

@Composable
private fun DiagnosticsTab(
    diagnostics: AuthPermissionDiagnostics,
    lang: AppLang,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AuthPermissionDiagnosticCard(
            diagnostics = diagnostics,
            lang = lang,
            onRefresh = onRefresh,
            testTagPrefix = "settings_diag"
        )
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WebsiteSettingsTab(
    settings: WebsiteSettings,
    lang: AppLang,
    onSaveSettings: (WebsiteSettings) -> Unit
) {
    val context = LocalContext.current
    var draft by remember(settings) { mutableStateOf(settings) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = PdfAndExportEngine.compressUriToBase64(context, uri, maxDim = 220, maxChars = 120_000)
            if (encoded != null) {
                draft = draft.copy(logoBase64 = encoded)
            }
        }
    }

    val colorPresets = listOf(
        "#0D4A3E" to "#C9962B",
        "#1A365D" to "#D97706",
        "#4A1D43" to "#E11D48",
        "#1E4620" to "#10B981"
    )

    val optionalFieldCatalog = listOf(
        "mobileNumber" to AppStrings.pick(lang, "মোবাইল নম্বর", "Mobile"),
        "fatherName" to AppStrings.pick(lang, "পিতার নাম", "Father Name"),
        "motherName" to AppStrings.pick(lang, "মাতার নাম", "Mother Name"),
        "bloodGroup" to AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group"),
        "divisionName" to AppStrings.pick(lang, "বিভাগ", "Division"),
        "districtName" to AppStrings.pick(lang, "জেলা", "District")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "অ্যাপ ও লোগো কাস্টমাইজেশন", "Logo & Identity Settings"), style = MaterialTheme.typography.titleMedium)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val bmp = remember(draft.logoBase64) { PdfAndExportEngine.decodeBase64Bitmap(draft.logoBase64) }
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(56.dp)
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Logo",
                                modifier = Modifier.size(56.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.brand_logo),
                                contentDescription = AppStrings.pick(lang, "ডিফল্ট লোগো", "Default Logo"),
                                modifier = Modifier.size(56.dp).padding(2.dp).clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(AppStrings.pick(lang, "লোগো আপলোড / পরিবর্তন", "Upload Logo"))
                    }
                    if (draft.logoBase64.isNotBlank()) {
                        TextButton(onClick = { draft = draft.copy(logoBase64 = "") }) {
                            Text(AppStrings.pick(lang, "ডিফল্ট", "Reset"))
                        }
                    }
                }

                OutlinedTextField(
                    value = draft.appNameBn,
                    onValueChange = { draft = draft.copy(appNameBn = it) },
                    label = { Text(AppStrings.pick(lang, "অ্যাপের নাম (বাংলা)", "App Name (Bangla)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.appNameEn,
                    onValueChange = { draft = draft.copy(appNameEn = it) },
                    label = { Text(AppStrings.pick(lang, "অ্যাপের নাম (ইংরেজি)", "App Name (English)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.headerText,
                    onValueChange = { draft = draft.copy(headerText = it) },
                    label = { Text(AppStrings.pick(lang, "হেডার বার্তা", "Header Text")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.footerText,
                    onValueChange = { draft = draft.copy(footerText = it) },
                    label = { Text(AppStrings.pick(lang, "ফুটার বার্তা", "Footer Text")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.welcomeMessageBn,
                    onValueChange = { draft = draft.copy(welcomeMessageBn = it) },
                    label = { Text(AppStrings.pick(lang, "স্বাগতম বার্তা (বাংলা)", "Welcome Message (Bn)")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.welcomeMessageEn,
                    onValueChange = { draft = draft.copy(welcomeMessageEn = it) },
                    label = { Text(AppStrings.pick(lang, "স্বাগতম বার্তা (ইংরেজি)", "Welcome Message (En)")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.contactPhone,
                    onValueChange = { draft = draft.copy(contactPhone = it) },
                    label = { Text(AppStrings.pick(lang, "যোগাযোগ ফোন", "Contact Phone")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.contactEmail,
                    onValueChange = { draft = draft.copy(contactEmail = it) },
                    label = { Text(AppStrings.pick(lang, "যোগাযোগ ইমেইল", "Contact Email")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "থিম রঙ ও সেকশন নিয়ন্ত্রণ", "Colors & Section Visibility"), style = MaterialTheme.typography.titleMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    colorPresets.forEach { (prim, acc) ->
                        val selected = draft.primaryColorHex.equals(prim, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(parseBrandColor(prim, Color(0xFF0D4A3E)))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) parseBrandColor(acc, Color(0xFFC9962B)) else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { draft = draft.copy(primaryColorHex = prim, accentColorHex = acc) }
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(AppStrings.pick(lang, "পরিসংখ্যান সেকশন দেখান", "Show Statistics Section"))
                    Switch(checked = draft.showStatsSection, onCheckedChange = { draft = draft.copy(showStatsSection = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(AppStrings.pick(lang, "দ্রুত কার্যক্রম সেকশন", "Show Quick Actions"))
                    Switch(checked = draft.showQuickActionsSection, onCheckedChange = { draft = draft.copy(showQuickActionsSection = it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(AppStrings.pick(lang, "গোপনীয়তা নোটিশ দেখান", "Show Privacy Notice"))
                    Switch(checked = draft.showPrivacyNotice, onCheckedChange = { draft = draft.copy(showPrivacyNotice = it) })
                }

                Text(AppStrings.pick(lang, "বাধ্যতামূলক প্রোফাইল ফিল্ড নির্বাচন", "Mandatory Profile Fields"), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    optionalFieldCatalog.forEach { (key, label) ->
                        val selected = key in draft.mandatoryFields
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) draft.mandatoryFields - key else draft.mandatoryFields + key
                                draft = draft.copy(mandatoryFields = next)
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        Button(
            onClick = { onSaveSettings(draft) },
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_website_settings_button")
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(AppStrings.pick(lang, "সেটিংস সংরক্ষণ করুন", "Save Settings"))
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BackupAndSecurityTab(
    actor: PimsAccount,
    allAccounts: List<PimsAccount>,
    settings: WebsiteSettings,
    lang: AppLang,
    bengaliFont: BengaliFontOption,
    onChangeBengaliFont: (BengaliFontOption) -> Unit,
    onChangeOwnPassword: (String, String) -> Unit,
    onSignOutGoogle: () -> Unit,
    onReportBanner: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var showChangePassword by remember { mutableStateOf(false) }
    var showPdfSelector by remember { mutableStateOf(false) }
    val authorizedAccounts = remember(actor, allAccounts) {
        val role = actor.roleEnum()
        allAccounts.filter { role.canViewAccount(it, actor.personalId, actor.assignedRecordIds) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BengaliFontSelectorCard(
            selectedFont = bengaliFont,
            lang = lang,
            onSelectFont = onChangeBengaliFont,
            testTagPrefix = "settings_font"
        )

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "ডেটা রপ্তানি ও ব্যাকআপ", "Data Export & Backup"), style = MaterialTheme.typography.titleMedium)
                Text(
                    AppStrings.pick(
                        lang,
                        "সমস্ত রপ্তানি থেকে পাসওয়ার্ড হ্যাশ, সল্ট এবং লকআউট তথ্য স্বয়ংক্রিয়ভাবে বাদ দেওয়া হয়।",
                        "All exports automatically exclude password hashes, salts, and sensitive session tokens."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showPdfSelector = true },
                    enabled = authorizedAccounts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.pick(lang, "সকল অনুমোদিত প্রোফাইল PDF", "Export All Authorized PDF"))
                }

                OutlinedButton(
                    onClick = {
                        PdfAndExportEngine.exportAccountsCsv(context, authorizedAccounts).onSuccess { file ->
                            PdfAndExportEngine.shareExportFile(context, file, "text/csv", "Export CSV")
                        }.onFailure { onReportBanner(true, it.message ?: "CSV error") }
                    },
                    enabled = authorizedAccounts.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.TableView, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.pick(lang, "CSV স্প্রেডশিট রপ্তানি", "Export CSV"))
                }

                if (actor.roleEnum().canFullBackup()) {
                    OutlinedButton(
                        onClick = {
                            PdfAndExportEngine.exportStructuredBackupJson(context, authorizedAccounts, settings).onSuccess { file ->
                                PdfAndExportEngine.shareExportFile(context, file, "application/json", "Export JSON Backup")
                            }.onFailure { onReportBanner(true, it.message ?: "JSON backup error") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.pick(lang, "পূর্ণ JSON ব্যাকআপ (সুপার অ্যাডমিন)", "Full JSON Backup (Super Admin)"))
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "নিরাপত্তা ও অ্যাকাউন্ট", "Security & Workspace"), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showChangePassword = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Key, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.pick(lang, "নিজের পাসওয়ার্ড পরিবর্তন", "Change My Password"))
                }
                OutlinedButton(onClick = onSignOutGoogle, modifier = Modifier.fillMaxWidth()) {
                    Text(AppStrings.pick(lang, "Google ওয়ার্কস্পেস পরিবর্তন / সাইন আউট", "Switch Google Workspace / Sign Out"))
                }
            }
        }
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            lang = lang,
            onDismiss = { showChangePassword = false },
            onSubmit = { cur, next ->
                showChangePassword = false
                onChangeOwnPassword(cur, next)
            }
        )
    }

    if (showPdfSelector) {
        PdfFieldSelectorDialog(
            lang = lang,
            onDismiss = { showPdfSelector = false },
            onConfirm = { selection ->
                showPdfSelector = false
                PdfAndExportEngine.generateProfilesPdf(
                    context = context,
                    accounts = authorizedAccounts,
                    settings = settings,
                    selection = selection,
                    lang = lang,
                    fileNamePrefix = "pims_all_authorized",
                    bengaliFontResId = bengaliFont.fontResId
                ).onSuccess { file ->
                    PdfAndExportEngine.shareExportFile(context, file, "application/pdf", "Share All Profiles PDF")
                }.onFailure {
                    onReportBanner(true, it.message ?: "PDF error")
                }
            }
        )
    }
}

@Composable
private fun AuditLogsTab(
    logs: List<ActivityLog>,
    lang: AppLang
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                Text(
                    AppStrings.pick(lang, "এখনো কোনো অ্যাক্টিভিটি লগ নেই।", "No audit logs yet."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs, key = { it.id }) { log ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${log.actorPersonalId} (${log.actionType})", style = MaterialTheme.typography.labelLarge)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (log.status == "SUCCESS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(log.status, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (lang == AppLang.BN) log.summaryBn else log.summaryEn.ifBlank { log.summaryBn },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
