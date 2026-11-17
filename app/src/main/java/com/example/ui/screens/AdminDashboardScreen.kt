package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.model.PimsAccount
import com.example.data.model.UiState
import com.example.data.model.WebsiteSettings
import com.example.ui.components.CategoryFilterChipRow
import com.example.ui.components.PersonalIdChip
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileSearchBar
import com.example.ui.components.StatBarCard
import com.example.ui.components.profileDocumentCategoryOptions
import com.example.ui.viewmodel.DashboardStatistics

@Composable
fun AdminDashboardScreen(
    stats: DashboardStatistics,
    accountsState: UiState<List<PimsAccount>>,
    settings: WebsiteSettings,
    activeAccount: PimsAccount,
    lang: AppLang,
    onSearchProfiles: (query: String, category: String) -> List<PimsAccount>,
    onOpenDirectoryWithFilter: (query: String, category: String) -> Unit,
    onCreateUser: () -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenExport: () -> Unit,
    onViewProfile: (PimsAccount) -> Unit,
    onRetry: () -> Unit
) {
    var quickQuery by rememberSaveable { mutableStateOf("") }
    var quickCategory by rememberSaveable { mutableStateOf("") }
    val categoryOptions = remember(lang) { profileDocumentCategoryOptions(lang) }
    val isFiltering = quickQuery.isNotBlank() || quickCategory.isNotBlank()
    val quickMatches = remember(quickQuery, quickCategory, stats.totalUsers, stats.recentProfiles) {
        if (!isFiltering) emptyList() else onSearchProfiles(quickQuery, quickCategory)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "স্বাগতম, ${activeAccount.fullNameBn}",
                            "Welcome, ${activeAccount.fullNameEn.ifBlank { activeAccount.fullNameBn }}"
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
        }

        when (accountsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = AppStrings.pick(lang, "তথ্য লোড হচ্ছে...", "Loading workspace records..."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = accountsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(onClick = onRetry) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(AppStrings.pick(lang, "পুনরায় চেষ্টা করুন", "Retry"))
                            }
                        }
                    }
                }
            }
            is UiState.Success -> Unit
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileSearchBar(
                    query = quickQuery,
                    onQueryChange = { quickQuery = it },
                    placeholder = AppStrings.pick(
                        lang,
                        "প্রোফাইল খুঁজুন (নাম, আইডি বা ক্যাটাগরি)...",
                        "Quick search profiles by name or category..."
                    ),
                    lang = lang,
                    testTag = "dashboard_profile_search",
                    onSearchSubmit = { onOpenDirectoryWithFilter(quickQuery, quickCategory) }
                )
                CategoryFilterChipRow(
                    categories = categoryOptions,
                    selectedKey = quickCategory,
                    onSelectCategory = { quickCategory = it },
                    testTagPrefix = "dashboard_category"
                )
            }
        }

        if (isFiltering) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "মিলে যাওয়া প্রোফাইল: ${quickMatches.size}",
                            "Matching Profiles: ${quickMatches.size}"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                quickQuery = ""
                                quickCategory = ""
                            }
                        ) {
                            Text(AppStrings.pick(lang, "মুছুন", "Clear"))
                        }
                        TextButton(
                            onClick = { onOpenDirectoryWithFilter(quickQuery, quickCategory) },
                            modifier = Modifier.testTag("dashboard_open_filtered_directory")
                        ) {
                            Text(AppStrings.pick(lang, "ডিরেক্টরিতে দেখুন", "Directory"))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (quickMatches.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(
                            text = AppStrings.pick(
                                lang,
                                "এই নাম বা ক্যাটাগরিতে কোনো প্রোফাইল পাওয়া যায়নি।",
                                "No profiles matched that name or category."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }
            } else {
                items(quickMatches.take(6), key = { "quick_${it.id}" }) { profile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewProfile(profile) }
                            .testTag("dashboard_quick_result_${profile.personalId}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatar(profile.photoBase64, profile.fullNameBn, sizeDp = 46)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(profile.fullNameBn, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = listOf(
                                        AppStrings.roleLabel(lang, profile.role),
                                        profile.orgDesignation,
                                        profile.districtName
                                    ).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PersonalIdChip(profile.personalId)
                        }
                    }
                }
            }
        }

        if (settings.showStatsSection) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = AppStrings.pick(lang, "মোট ব্যবহারকারী", "Total Users"),
                        value = stats.totalUsers.toString(),
                        icon = Icons.Filled.Groups,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = AppStrings.pick(lang, "সক্রিয়", "Active"),
                        value = stats.activeUsers.toString(),
                        icon = Icons.Filled.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        title = AppStrings.pick(lang, "নিষ্ক্রিয়", "Inactive"),
                        value = stats.inactiveUsers.toString(),
                        icon = Icons.Filled.PauseCircle,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        title = AppStrings.pick(lang, "মোট অ্যাডমিন", "Total Admins"),
                        value = stats.totalAdmins.toString(),
                        icon = Icons.Filled.AdminPanelSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (settings.showQuickActionsSection) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            AppStrings.pick(lang, "দ্রুত কার্যক্রম", "Quick Actions"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeAccount.roleEnum().canCreateAccounts()) {
                                FilledTonalButton(
                                    onClick = onCreateUser,
                                    modifier = Modifier.weight(1f).testTag("quick_create_user_button")
                                ) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(AppStrings.pick(lang, "নতুন ব্যবহারকারী", "Add User"))
                                }
                            }
                            FilledTonalButton(
                                onClick = onOpenDirectory,
                                modifier = Modifier.weight(1f).testTag("quick_search_users_button")
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(AppStrings.pick(lang, "উন্নত অনুসন্ধান", "Search"))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onOpenLocations,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(AppStrings.pick(lang, "লোকেশন ও প্রতিষ্ঠান", "Geo & Schools"))
                            }
                            FilledTonalButton(
                                onClick = onOpenExport,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(AppStrings.pick(lang, "রিপোর্ট ও ব্যাকআপ", "Export/Backup"))
                            }
                        }
                    }
                }
            }
        }

        if (settings.showStatsSection) {
            item {
                StatBarCard(
                    title = AppStrings.pick(lang, "জেলা ভিত্তিক পরিসংখ্যান", "District-wise Statistics"),
                    entries = stats.districtCounts,
                    emptyText = AppStrings.pick(lang, "কোনো জেলার তথ্য এখনো নেই", "No district data yet")
                )
            }
            item {
                StatBarCard(
                    title = AppStrings.pick(lang, "উপজেলা ভিত্তিক পরিসংখ্যান", "Upazila-wise Statistics"),
                    entries = stats.upazilaCounts,
                    emptyText = AppStrings.pick(lang, "কোনো উপজেলার তথ্য এখনো নেই", "No upazila data yet")
                )
            }
            item {
                StatBarCard(
                    title = AppStrings.pick(lang, "রক্তের গ্রুপ পরিসংখ্যান", "Blood Group Statistics"),
                    entries = stats.bloodGroupCounts,
                    emptyText = AppStrings.pick(lang, "রক্তের গ্রুপের তথ্য নেই", "No blood group data yet")
                )
            }
            item {
                StatBarCard(
                    title = AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান ভিত্তিক পরিসংখ্যান", "Institution Statistics"),
                    entries = stats.institutionCounts,
                    emptyText = AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠানের তথ্য নেই", "No institution data yet")
                )
            }
            item {
                StatBarCard(
                    title = AppStrings.pick(lang, "সাংগঠনিক পদবী পরিসংখ্যান", "Organizational Position Statistics"),
                    entries = stats.orgPositionCounts,
                    emptyText = AppStrings.pick(lang, "সাংগঠনিক পদবীর তথ্য নেই", "No position data yet")
                )
            }
        }

        item {
            Text(
                text = AppStrings.pick(lang, "সাম্প্রতিক তৈরি প্রোফাইল", "Recently Created Profiles"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (stats.recentProfiles.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = AppStrings.pick(
                            lang,
                            "এখনো কোনো তথ্য যোগ করা হয়নি।",
                            "No data has been added yet."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        } else {
            items(stats.recentProfiles, key = { it.id }) { profile ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewProfile(profile) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(profile.photoBase64, profile.fullNameBn, sizeDp = 46)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.fullNameBn, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = listOf(profile.orgDesignation, profile.districtName)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • ")
                                    .ifBlank { AppStrings.roleLabel(lang, profile.role) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PersonalIdChip(profile.personalId)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = settings.footerText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
