package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.export.PdfAndExportEngine
import com.example.data.export.PdfFieldSelection
import com.example.data.model.EducationalInstitution
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.model.OrgPosition
import com.example.data.model.PimsAccount
import com.example.data.model.PimsRole
import com.example.data.model.WebsiteSettings
import com.example.ui.components.CategoryFilterChipRow
import com.example.ui.components.PdfFieldSelectorDialog
import com.example.ui.components.PersonalIdChip
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileSearchBar
import com.example.ui.components.SearchCategoryOption
import com.example.ui.components.SearchablePickerField
import com.example.ui.components.profileDocumentCategoryOptions
import com.example.ui.viewmodel.SearchFilterState
import com.example.ui.viewmodel.UserSortOrder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsersDirectoryScreen(
    activeAccount: PimsAccount,
    filteredAccounts: List<PimsAccount>,
    filterState: SearchFilterState,
    locations: List<LocationNode>,
    institutions: List<EducationalInstitution>,
    orgPositions: List<OrgPosition>,
    settings: WebsiteSettings,
    lang: AppLang,
    onUpdateFilter: ((SearchFilterState) -> SearchFilterState) -> Unit,
    onCreateUser: () -> Unit,
    onSelectAccount: (PimsAccount) -> Unit,
    onReportBanner: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var showPdfSelector by remember { mutableStateOf(false) }

    val totalPages = ((filteredAccounts.size + filterState.pageSize - 1) / filterState.pageSize).coerceAtLeast(1)
    val safePage = filterState.pageIndex.coerceIn(0, totalPages - 1)
    val pageItems = remember(filteredAccounts, safePage, filterState.pageSize) {
        filteredAccounts.drop(safePage * filterState.pageSize).take(filterState.pageSize)
    }

    Scaffold(
        floatingActionButton = {
            if (activeAccount.roleEnum().canCreateAccounts()) {
                FloatingActionButton(
                    onClick = onCreateUser,
                    modifier = Modifier.testTag("fab_create_user")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = AppStrings.pick(lang, "নতুন ব্যবহারকারী", "Create User"))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            val categoryOptions = remember(lang) { profileDocumentCategoryOptions(lang) }
            val activeFilterCount = filterState.activeAdvancedFilterCount()
            val hasAnyFilter = filterState.query.isNotBlank() ||
                filterState.categoryFilter.isNotBlank() ||
                activeFilterCount > 0

            ProfileSearchBar(
                query = filterState.query,
                onQueryChange = { q -> onUpdateFilter { it.copy(query = q, pageIndex = 0) } },
                placeholder = AppStrings.pick(
                    lang,
                    "নাম, আইডি বা ক্যাটাগরি দিয়ে খুঁজুন...",
                    "Search by name, ID, or category..."
                ),
                lang = lang,
                testTag = "directory_search_input",
                onToggleFilters = { showAdvancedFilters = !showAdvancedFilters },
                filtersExpanded = showAdvancedFilters,
                activeFilterCount = activeFilterCount
            )
            Spacer(Modifier.height(8.dp))
            CategoryFilterChipRow(
                categories = categoryOptions,
                selectedKey = filterState.categoryFilter,
                onSelectCategory = { key ->
                    onUpdateFilter { it.copy(categoryFilter = key, pageIndex = 0) }
                },
                testTagPrefix = "directory_category"
            )

            AnimatedVisibility(visible = showAdvancedFilters) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            AppStrings.pick(lang, "উন্নত অনুসন্ধান ও ফিল্টার", "Advanced Search Filters"),
                            style = MaterialTheme.typography.titleSmall
                        )

                        val divisions = remember(locations) { locations.filter { it.level == LocationLevel.DIVISION.name } }
                        val districts = remember(locations, filterState.divisionId) {
                            locations.filter { it.level == LocationLevel.DISTRICT.name && it.parentId == filterState.divisionId }
                        }
                        val upazilas = remember(locations, filterState.districtId) {
                            locations.filter { it.level == LocationLevel.UPAZILA.name && it.parentId == filterState.districtId }
                        }
                        val unions = remember(locations, filterState.upazilaId) {
                            locations.filter { it.level == LocationLevel.UNION.name && it.parentId == filterState.upazilaId }
                                .sortedBy { it.code.ifBlank { it.nameBn } }
                        }
                        val wards = remember(locations, filterState.unionId) {
                            locations.filter { it.level == LocationLevel.WARD.name && it.parentId == filterState.unionId }
                                .sortedBy { it.code.toIntOrNull() ?: 99 }
                        }

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "বিভাগ", "Division"),
                            selectedLabel = divisions.firstOrNull { it.id == filterState.divisionId }?.displayName(lang == AppLang.BN).orEmpty(),
                            items = divisions,
                            itemLabel = { it.displayName(lang == AppLang.BN) },
                            onSelect = { div ->
                                onUpdateFilter {
                                    it.copy(divisionId = div.id, districtId = "", upazilaId = "", unionId = "", wardId = "", pageIndex = 0)
                                }
                            },
                            onClear = {
                                onUpdateFilter {
                                    it.copy(divisionId = "", districtId = "", upazilaId = "", unionId = "", wardId = "", pageIndex = 0)
                                }
                            }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "জেলা", "District"),
                            selectedLabel = districts.firstOrNull { it.id == filterState.districtId }?.displayName(lang == AppLang.BN).orEmpty(),
                            items = districts,
                            itemLabel = { it.displayName(lang == AppLang.BN) },
                            enabled = filterState.divisionId.isNotBlank(),
                            onSelect = { dist ->
                                onUpdateFilter { it.copy(districtId = dist.id, upazilaId = "", unionId = "", wardId = "", pageIndex = 0) }
                            },
                            onClear = {
                                onUpdateFilter { it.copy(districtId = "", upazilaId = "", unionId = "", wardId = "", pageIndex = 0) }
                            }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "উপজেলা", "Upazila"),
                            selectedLabel = upazilas.firstOrNull { it.id == filterState.upazilaId }?.displayName(lang == AppLang.BN).orEmpty(),
                            items = upazilas,
                            itemLabel = { it.displayName(lang == AppLang.BN) },
                            enabled = filterState.districtId.isNotBlank(),
                            onSelect = { upz ->
                                onUpdateFilter { it.copy(upazilaId = upz.id, unionId = "", wardId = "", pageIndex = 0) }
                            },
                            onClear = {
                                onUpdateFilter { it.copy(upazilaId = "", unionId = "", wardId = "", pageIndex = 0) }
                            }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "ইউনিয়ন", "Union"),
                            selectedLabel = unions.firstOrNull { it.id == filterState.unionId }?.displayName(lang == AppLang.BN).orEmpty(),
                            items = unions,
                            itemLabel = { it.displayName(lang == AppLang.BN) },
                            enabled = filterState.upazilaId.isNotBlank(),
                            onSelect = { un ->
                                onUpdateFilter { it.copy(unionId = un.id, wardId = "", pageIndex = 0) }
                            },
                            onClear = {
                                onUpdateFilter { it.copy(unionId = "", wardId = "", pageIndex = 0) }
                            }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "ওয়ার্ড", "Ward"),
                            selectedLabel = wards.firstOrNull { it.id == filterState.wardId }?.displayName(lang == AppLang.BN).orEmpty(),
                            items = wards,
                            itemLabel = { it.displayName(lang == AppLang.BN) },
                            enabled = filterState.unionId.isNotBlank(),
                            onSelect = { w ->
                                onUpdateFilter { it.copy(wardId = w.id, pageIndex = 0) }
                            },
                            onClear = {
                                onUpdateFilter { it.copy(wardId = "", pageIndex = 0) }
                            }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group"),
                            selectedLabel = filterState.bloodGroup,
                            items = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"),
                            itemLabel = { it },
                            onSelect = { bg -> onUpdateFilter { it.copy(bloodGroup = bg, pageIndex = 0) } },
                            onClear = { onUpdateFilter { it.copy(bloodGroup = "", pageIndex = 0) } }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান", "Institution"),
                            selectedLabel = institutions.firstOrNull { it.id == filterState.institutionId }?.nameBn.orEmpty(),
                            items = institutions,
                            itemLabel = { it.nameBn },
                            itemSubtitle = { it.locationLabel },
                            onSelect = { inst -> onUpdateFilter { it.copy(institutionId = inst.id, pageIndex = 0) } },
                            onClear = { onUpdateFilter { it.copy(institutionId = "", pageIndex = 0) } }
                        )

                        SearchablePickerField(
                            label = AppStrings.pick(lang, "সাংগঠনিক পদবী", "Designation"),
                            selectedLabel = filterState.orgDesignation,
                            items = orgPositions.map { it.designationBn }.distinct(),
                            itemLabel = { it },
                            onSelect = { des -> onUpdateFilter { it.copy(orgDesignation = des, pageIndex = 0) } },
                            onClear = { onUpdateFilter { it.copy(orgDesignation = "", pageIndex = 0) } }
                        )

                        OutlinedTextField(
                            value = filterState.villageQuery,
                            onValueChange = { v -> onUpdateFilter { it.copy(villageQuery = v, pageIndex = 0) } },
                            label = { Text(AppStrings.pick(lang, "গ্রাম / এলাকা", "Village / Area")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = filterState.activeOnly == true,
                                onClick = {
                                    onUpdateFilter { it.copy(activeOnly = if (it.activeOnly == true) null else true, pageIndex = 0) }
                                },
                                label = { Text(AppStrings.pick(lang, "শুধু সক্রিয়", "Active Only")) }
                            )
                            FilterChip(
                                selected = filterState.sortOrder == UserSortOrder.NAME_ASC,
                                onClick = {
                                    onUpdateFilter {
                                        it.copy(sortOrder = if (it.sortOrder == UserSortOrder.NAME_ASC) UserSortOrder.NEWEST else UserSortOrder.NAME_ASC)
                                    }
                                },
                                label = { Text(AppStrings.pick(lang, "নাম অনুযায়ী", "Sort Name")) }
                            )
                            FilterChip(
                                selected = filterState.sortOrder == UserSortOrder.PERSONAL_ID,
                                onClick = {
                                    onUpdateFilter {
                                        it.copy(sortOrder = if (it.sortOrder == UserSortOrder.PERSONAL_ID) UserSortOrder.NEWEST else UserSortOrder.PERSONAL_ID)
                                    }
                                },
                                label = { Text(AppStrings.pick(lang, "আইডি অনুযায়ী", "Sort ID")) }
                            )
                            TextButton(onClick = { onUpdateFilter { SearchFilterState() } }) {
                                Text(AppStrings.pick(lang, "ফিল্টার মুছুন", "Reset"))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = AppStrings.pick(lang, "মোট ফলাফল: ${filteredAccounts.size}", "Results: ${filteredAccounts.size}"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (hasAnyFilter) {
                        TextButton(
                            onClick = { onUpdateFilter { SearchFilterState() } },
                            modifier = Modifier.testTag("directory_clear_all_filters")
                        ) {
                            Text(AppStrings.pick(lang, "সব মুছুন", "Clear"))
                        }
                    }
                }
                if (activeAccount.roleEnum().canExportAll() && filteredAccounts.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { showPdfSelector = true }) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("PDF")
                        }
                        OutlinedButton(onClick = {
                            PdfAndExportEngine.exportAccountsCsv(context, filteredAccounts).onSuccess { file ->
                                PdfAndExportEngine.shareExportFile(context, file, "text/csv", "Export CSV")
                            }.onFailure {
                                onReportBanner(true, it.message ?: "CSV Export Failed")
                            }
                        }) {
                            Icon(Icons.Filled.TableView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV")
                        }
                    }
                }
            }

            if (pageItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            AppStrings.pick(lang, "কোনো ব্যবহারকারীর রেকর্ড পাওয়া যায়নি।", "No user records found."),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasAnyFilter) {
                            OutlinedButton(onClick = { onUpdateFilter { SearchFilterState() } }) {
                                Text(AppStrings.pick(lang, "ফিল্টার রিসেট করুন", "Reset Filters"))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(pageItems, key = { it.id }) { account ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAccount(account) }
                                .testTag("user_card_${account.personalId}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileAvatar(account.photoBase64, account.fullNameBn, sizeDp = 50)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(account.fullNameBn, style = MaterialTheme.typography.titleMedium)
                                    if (account.fullNameEn.isNotBlank()) {
                                        Text(account.fullNameEn, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        PersonalIdChip(account.personalId)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (account.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = if (account.active) AppStrings.pick(lang, "সক্রিয়", "Active") else AppStrings.pick(lang, "নিষ্ক্রিয়", "Inactive"),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    val sub = listOf(
                                        AppStrings.roleLabel(lang, account.role),
                                        account.districtName,
                                        account.bloodGroup
                                    ).filter { it.isNotBlank() }.joinToString(" • ")
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        if (totalPages > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { onUpdateFilter { it.copy(pageIndex = (safePage - 1).coerceAtLeast(0)) } },
                                    enabled = safePage > 0
                                ) { Text(AppStrings.pick(lang, "পূর্ববর্তী", "Prev")) }

                                Text("${safePage + 1} / $totalPages", style = MaterialTheme.typography.labelLarge)

                                OutlinedButton(
                                    onClick = { onUpdateFilter { it.copy(pageIndex = (safePage + 1).coerceAtMost(totalPages - 1)) } },
                                    enabled = safePage < totalPages - 1
                                ) { Text(AppStrings.pick(lang, "পরবর্তী", "Next")) }
                            }
                        }
                        Spacer(Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showPdfSelector) {
        PdfFieldSelectorDialog(
            lang = lang,
            onDismiss = { showPdfSelector = false },
            onConfirm = { selection ->
                showPdfSelector = false
                PdfAndExportEngine.generateProfilesPdf(
                    context = context,
                    accounts = filteredAccounts,
                    settings = settings,
                    selection = selection,
                    lang = lang,
                    fileNamePrefix = "pims_directory"
                ).onSuccess { file ->
                    PdfAndExportEngine.shareExportFile(context, file, "application/pdf", "Share Directory PDF")
                }.onFailure {
                    onReportBanner(true, it.message ?: "PDF generation failed")
                }
            }
        )
    }
}

@Composable
fun UserDetailScreen(
    actor: PimsAccount,
    target: PimsAccount,
    settings: WebsiteSettings,
    lang: AppLang,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onResetPassword: (String) -> Unit,
    onDelete: () -> Unit,
    onReportBanner: (Boolean, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var printMode by remember { mutableStateOf(false) }

    val canEdit = actor.roleEnum().canEditAccount(target, actor.personalId, actor.assignedRecordIds)
    val isAdmin = actor.roleEnum().isAdminTier()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                AppStrings.pick(lang, "ব্যবহারকারীর বিস্তারিত প্রোফাইল", "User Profile Details"),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (canEdit) {
                IconButton(onClick = onEdit, modifier = Modifier.testTag("detail_edit_button")) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(target.photoBase64, target.fullNameBn, sizeDp = 72)
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(target.fullNameBn, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (target.fullNameEn.isNotBlank()) {
                        Text(target.fullNameEn, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PersonalIdChip(target.personalId)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                AppStrings.roleLabel(lang, target.role),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { printMode = false; showPdfDialog = true },
                modifier = Modifier.weight(1f).testTag("detail_pdf_button")
            ) {
                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.pick(lang, "PDF ডাউনলোড", "Download PDF"))
            }
            OutlinedButton(
                onClick = { printMode = true; showPdfDialog = true },
                modifier = Modifier.weight(1f).testTag("detail_print_button")
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(AppStrings.pick(lang, "প্রিন্ট", "Print"))
            }
        }

        var detailQuery by remember(target.id) { mutableStateOf("") }
        var detailCategory by remember(target.id) { mutableStateOf("") }
        val detailCategories = remember(lang) {
            listOf(
                SearchCategoryOption("", AppStrings.pick(lang, "সকল তথ্য", "All")),
                SearchCategoryOption("BASIC", AppStrings.pick(lang, "মৌলিক", "Basic")),
                SearchCategoryOption("EDU", AppStrings.pick(lang, "শিক্ষা", "Education")),
                SearchCategoryOption("GOALS", AppStrings.pick(lang, "লক্ষ্য ও দক্ষতা", "Goals")),
                SearchCategoryOption("ORG", AppStrings.pick(lang, "সাংগঠনিক", "Org")),
                SearchCategoryOption("ADDR", AppStrings.pick(lang, "ঠিকানা", "Address"))
            )
        }

        ProfileSearchBar(
            query = detailQuery,
            onQueryChange = { detailQuery = it },
            placeholder = AppStrings.pick(lang, "প্রোফাইলের তথ্য খুঁজুন (নাম, মোবাইল, জেলা...)", "Filter profile details..."),
            lang = lang,
            testTag = "detail_profile_search"
        )
        CategoryFilterChipRow(
            categories = detailCategories,
            selectedKey = detailCategory,
            onSelectCategory = { detailCategory = it },
            testTagPrefix = "detail_category"
        )

        val sections = listOf(
            Triple(
                "BASIC",
                AppStrings.pick(lang, "ক. মৌলিক তথ্য", "A. Basic Information"),
                listOf(
                    AppStrings.pick(lang, "পিতার নাম", "Father's Name") to target.fatherName,
                    AppStrings.pick(lang, "মাতার নাম", "Mother's Name") to target.motherName,
                    AppStrings.pick(lang, "জন্ম তারিখ", "Date of Birth") to target.dateOfBirth,
                    AppStrings.pick(lang, "লিঙ্গ", "Gender") to target.gender,
                    AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group") to target.bloodGroup,
                    AppStrings.pick(lang, "মোবাইল", "Mobile") to target.mobileNumber,
                    AppStrings.pick(lang, "ইমেইল", "Email") to target.emailAddress
                )
            ),
            Triple(
                "EDU",
                AppStrings.pick(lang, "খ. শিক্ষাগত তথ্য", "B. Educational Information"),
                listOf(
                    AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান", "Institution") to target.institutionName,
                    AppStrings.pick(lang, "ধরন", "Type") to if (target.institutionType.isNotBlank()) AppStrings.institutionTypeLabel(lang, target.institutionType) else "",
                    AppStrings.pick(lang, "শ্রেণি / বর্ষ", "Class / Year") to target.currentClassOrYear,
                    AppStrings.pick(lang, "যোগ্যতা", "Qualification") to target.educationalQualification,
                    AppStrings.pick(lang, "বিভাগ / বিষয়", "Department / Subject") to target.departmentOrSubject,
                    AppStrings.pick(lang, "একাডেমিক তথ্য", "Academic Info") to target.academicInfo
                )
            ),
            Triple(
                "GOALS",
                AppStrings.pick(lang, "গ. ব্যক্তিগত লক্ষ্য ও দক্ষতা", "C. Personal Goals & Skills"),
                listOf(
                    AppStrings.pick(lang, "ক্যারিয়ার লক্ষ্য", "Career Goal") to target.careerGoal,
                    AppStrings.pick(lang, "দক্ষতা", "Skills") to target.skills,
                    AppStrings.pick(lang, "আগ্রহ", "Interests") to target.interests,
                    AppStrings.pick(lang, "ব্যক্তিগত পরিচিতি", "Personal Intro") to target.personalIntro
                )
            ),
            Triple(
                "ORG",
                AppStrings.pick(lang, "ঘ. সাংগঠনিক তথ্য", "D. Organizational Information"),
                listOf(
                    AppStrings.pick(lang, "স্তর", "Level") to target.orgLevel,
                    AppStrings.pick(lang, "পদবী", "Designation") to target.orgDesignation,
                    AppStrings.pick(lang, "যোগদানের তারিখ", "Joining Date") to target.orgJoiningDate,
                    AppStrings.pick(lang, "অবস্থা", "Status") to target.orgStatus,
                    AppStrings.pick(lang, "অতিরিক্ত তথ্য", "Additional Info") to target.orgAdditionalInfo
                )
            ),
            Triple(
                "ADDR",
                AppStrings.pick(lang, "ঙ. ঠিকানা", "E. Address"),
                listOf(
                    AppStrings.pick(lang, "বিভাগ", "Division") to target.divisionName,
                    AppStrings.pick(lang, "জেলা", "District") to target.districtName,
                    AppStrings.pick(lang, "উপজেলা", "Upazila") to target.upazilaName,
                    AppStrings.pick(lang, "ইউনিয়ন", "Union") to target.unionName,
                    AppStrings.pick(lang, "ওয়ার্ড", "Ward") to target.wardName,
                    AppStrings.pick(lang, "গ্রাম / এলাকা", "Village / Area") to target.villageName
                )
            )
        )

        val dq = detailQuery.trim().lowercase()
        val matchingSections = sections.mapNotNull { (catKey, title, rows) ->
            if (detailCategory.isNotBlank() && detailCategory != catKey) return@mapNotNull null
            val filteredRows = if (dq.isBlank()) {
                rows
            } else {
                val titleMatches = title.lowercase().contains(dq)
                if (titleMatches) rows
                else rows.filter { (label, value) ->
                    value.isNotBlank() && (label.lowercase().contains(dq) || value.lowercase().contains(dq))
                }
            }
            if (dq.isNotBlank() && filteredRows.none { it.second.isNotBlank() }) null
            else Triple(catKey, title, filteredRows)
        }

        if (matchingSections.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text(
                    text = AppStrings.pick(lang, "কোনো মিল থাকা তথ্য পাওয়া যায়নি।", "No matching profile fields found."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        } else {
            matchingSections.forEach { (_, title, rows) ->
                ProfileSectionCard(title = title, rows = rows)
            }
        }

        if (isAdmin && target.personalId != actor.personalId) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        AppStrings.pick(lang, "অ্যাডমিন নিয়ন্ত্রণ", "Admin Controls"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.pick(lang, "অ্যাকাউন্ট সক্রিয় অবস্থা", "Account Active"))
                        Switch(checked = target.active, onCheckedChange = { onToggleActive() })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStrings.pick(lang, "পাসওয়ার্ড রিসেট", "Reset Password"))
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStrings.pick(lang, "মুছুন", "Delete"))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(AppStrings.pick(lang, "প্রোফাইল মুছে ফেলার নিশ্চিতকরণ", "Confirm Delete")) },
            text = {
                Text(
                    AppStrings.pick(
                        lang,
                        "আপনি কি নিশ্চিতভাবে ${target.fullNameBn} (${target.personalId})-এর প্রোফাইল মুছে ফেলতে চান?",
                        "Are you sure you want to permanently delete ${target.personalId}?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(AppStrings.pick(lang, "হ্যাঁ, মুছুন", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) }
            }
        )
    }

    if (showResetDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(AppStrings.pick(lang, "নতুন প্রাথমিক পাসওয়ার্ড সেট করুন", "Set New Initial Password")) },
            text = {
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text(AppStrings.pick(lang, "নতুন পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর)", "New Password (min 8 chars)")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPass.length >= 8) {
                            showResetDialog = false
                            onResetPassword(newPass)
                        }
                    },
                    enabled = newPass.length >= 8
                ) { Text(AppStrings.pick(lang, "রিসেট করুন", "Reset")) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) }
            }
        )
    }

    if (showPdfDialog) {
        PdfFieldSelectorDialog(
            lang = lang,
            onDismiss = { showPdfDialog = false },
            onConfirm = { selection: PdfFieldSelection ->
                showPdfDialog = false
                PdfAndExportEngine.generateProfilesPdf(
                    context = context,
                    accounts = listOf(target),
                    settings = settings,
                    selection = selection,
                    lang = lang,
                    fileNamePrefix = "profile_${target.personalId}"
                ).onSuccess { file ->
                    if (printMode) {
                        PdfAndExportEngine.printPdfFile(context, file, "Profile_${target.personalId}")
                    } else {
                        PdfAndExportEngine.shareExportFile(context, file, "application/pdf", "Share Profile PDF")
                    }
                }.onFailure {
                    onReportBanner(true, it.message ?: "PDF error")
                }
            }
        )
    }
}

@Composable
fun ProfileSectionCard(
    title: String,
    rows: List<Pair<String, String>>
) {
    val visible = rows.filter { it.second.isNotBlank() }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visible.forEachIndexed { idx, (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
                    }
                    if (idx < visible.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
        }
    }
}
