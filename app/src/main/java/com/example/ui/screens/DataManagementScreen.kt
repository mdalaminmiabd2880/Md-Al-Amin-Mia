package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.geo.BengaliTextSearch
import com.example.data.geo.GeoDatasetMeta
import com.example.data.geo.GeoVerificationStatus
import com.example.data.geo.MergedGeoNode
import com.example.data.geo.UnionGeoSummary
import com.example.data.model.EducationalInstitution
import com.example.data.model.InstitutionType
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.model.OrgPosition
import com.example.data.model.PimsAccount
import com.example.ui.components.ProfileSearchBar
import com.example.ui.components.SearchablePickerField

@Composable
fun DataManagementScreen(
    actor: PimsAccount,
    locations: List<LocationNode>,
    mergedGeoNodes: List<MergedGeoNode> = emptyList(),
    unionGeoSummaries: List<UnionGeoSummary> = emptyList(),
    geoMeta: GeoDatasetMeta = GeoDatasetMeta(),
    institutions: List<EducationalInstitution>,
    orgPositions: List<OrgPosition>,
    lang: AppLang,
    onSaveLocation: (LocationNode) -> Unit,
    onAssignVillageToWard: (LocationNode, String, String) -> Unit = { _, _, _ -> },
    onExportGeoData: (asCsv: Boolean) -> Unit = {},
    onDeleteLocation: (String) -> Unit,
    onImportLocationsCsv: (String) -> Unit,
    onSaveInstitution: (EducationalInstitution) -> Unit,
    onDeleteInstitution: (String) -> Unit,
    onImportInstitutionsCsv: (String) -> Unit,
    onSaveOrgPosition: (OrgPosition) -> Unit,
    onDeleteOrgPosition: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val canManage = actor.roleEnum().canManageMasterDatasets()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(AppStrings.pick(lang, "লোকেশন", "Locations")) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(AppStrings.pick(lang, "প্রতিষ্ঠান", "Institutions")) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(AppStrings.pick(lang, "পদবী", "Positions")) }
            )
        }

        when (selectedTab) {
            0 -> LocationsManagerTab(
                canManage = canManage,
                locations = locations,
                mergedGeoNodes = mergedGeoNodes,
                unionGeoSummaries = unionGeoSummaries,
                geoMeta = geoMeta,
                lang = lang,
                onSaveLocation = onSaveLocation,
                onAssignVillageToWard = onAssignVillageToWard,
                onExportGeoData = onExportGeoData,
                onDeleteLocation = onDeleteLocation,
                onImportCsv = onImportLocationsCsv
            )
            1 -> InstitutionsManagerTab(
                canManage = canManage,
                locations = locations,
                institutions = institutions,
                lang = lang,
                onSaveInstitution = onSaveInstitution,
                onDeleteInstitution = onDeleteInstitution,
                onImportCsv = onImportInstitutionsCsv
            )
            2 -> OrgPositionsManagerTab(
                canManage = canManage,
                positions = orgPositions,
                lang = lang,
                onSavePosition = onSaveOrgPosition,
                onDeletePosition = onDeleteOrgPosition
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LocationsManagerTab(
    canManage: Boolean,
    locations: List<LocationNode>,
    mergedGeoNodes: List<MergedGeoNode>,
    unionGeoSummaries: List<UnionGeoSummary>,
    geoMeta: GeoDatasetMeta,
    lang: AppLang,
    onSaveLocation: (LocationNode) -> Unit,
    onAssignVillageToWard: (LocationNode, String, String) -> Unit,
    onExportGeoData: (asCsv: Boolean) -> Unit,
    onDeleteLocation: (String) -> Unit,
    onImportCsv: (String) -> Unit
) {
    var selectedLevel by remember { mutableStateOf(LocationLevel.UNION) }
    var selectedParentDivId by remember { mutableStateOf(geoMeta.divisionId) }
    var selectedParentDistId by remember { mutableStateOf(geoMeta.districtId) }
    var selectedParentUpzId by remember { mutableStateOf(geoMeta.upazilaId) }
    var selectedParentUnionId by remember { mutableStateOf("") }
    var selectedParentWardId by remember { mutableStateOf("") }
    var geoSearchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<GeoVerificationStatus?>(null) }
    var showDiscrepancyDetails by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<MergedGeoNode?>(null) }
    var assigningVillage by remember { mutableStateOf<MergedGeoNode?>(null) }
    var showCsvDialog by remember { mutableStateOf(false) }

    val effectiveMerged = remember(mergedGeoNodes, locations) {
        if (mergedGeoNodes.isNotEmpty()) mergedGeoNodes
        else locations.map {
            MergedGeoNode(
                node = it,
                isOfficialReference = false,
                isAdminOverridden = true,
                reference = null,
                unionId = "",
                effectiveWardIds = emptyList(),
                effectiveStatus = GeoVerificationStatus.VERIFIED
            )
        }
    }

    val divisions = remember(locations) { locations.filter { it.level == LocationLevel.DIVISION.name }.sortedBy { it.nameBn } }
    val districts = remember(locations, selectedParentDivId) {
        locations.filter { it.level == LocationLevel.DISTRICT.name && (selectedParentDivId.isBlank() || it.parentId == selectedParentDivId) }
            .sortedBy { it.nameBn }
    }
    val upazilas = remember(locations, selectedParentDistId) {
        locations.filter { it.level == LocationLevel.UPAZILA.name && (selectedParentDistId.isBlank() || it.parentId == selectedParentDistId) }
            .sortedBy { it.nameBn }
    }
    val unions = remember(locations, selectedParentUpzId) {
        locations.filter { it.level == LocationLevel.UNION.name && (selectedParentUpzId.isBlank() || it.parentId == selectedParentUpzId) }
            .sortedBy { it.code.ifBlank { it.nameBn } }
    }
    val wards = remember(locations, selectedParentUnionId) {
        locations.filter { it.level == LocationLevel.WARD.name && (selectedParentUnionId.isBlank() || it.parentId == selectedParentUnionId) }
            .sortedBy { it.code.toIntOrNull() ?: 99 }
    }

    val collectedVillagesCount = remember(effectiveMerged) {
        effectiveMerged.count { it.node.level == LocationLevel.VILLAGE.name }
    }
    val unverifiedVillageCount = remember(effectiveMerged) {
        effectiveMerged.count {
            it.node.level == LocationLevel.VILLAGE.name &&
                it.effectiveStatus != GeoVerificationStatus.VERIFIED
        }
    }

    val displayed = remember(
        effectiveMerged,
        selectedLevel,
        selectedParentDivId,
        selectedParentDistId,
        selectedParentUpzId,
        selectedParentUnionId,
        selectedParentWardId,
        geoSearchQuery,
        statusFilter
    ) {
        effectiveMerged.filter { item ->
            val node = item.node
            if (node.level != selectedLevel.name) return@filter false
            val matchesParent = when (selectedLevel) {
                LocationLevel.DIVISION -> true
                LocationLevel.DISTRICT -> selectedParentDivId.isBlank() || node.parentId == selectedParentDivId
                LocationLevel.UPAZILA -> selectedParentDistId.isBlank() || node.parentId == selectedParentDistId
                LocationLevel.UNION -> selectedParentUpzId.isBlank() || node.parentId == selectedParentUpzId
                LocationLevel.WARD -> selectedParentUnionId.isBlank() || node.parentId == selectedParentUnionId
                LocationLevel.VILLAGE -> {
                    val matchesUnion = selectedParentUnionId.isBlank() || item.unionId == selectedParentUnionId
                    val matchesWard = selectedParentWardId.isBlank() ||
                        selectedParentWardId in item.effectiveWardIds ||
                        node.parentId == selectedParentWardId
                    matchesUnion && matchesWard
                }
            }
            val matchesStatus = statusFilter == null || item.effectiveStatus == statusFilter
            val matchesSearch = BengaliTextSearch.matches(
                geoSearchQuery,
                node.nameBn,
                node.nameEn,
                node.code,
                item.aliasesBn.joinToString(" "),
                item.noteBn
            )
            matchesParent && matchesStatus && matchesSearch
        }.sortedBy {
            if (selectedLevel == LocationLevel.UNION || selectedLevel == LocationLevel.WARD) {
                it.node.code.padStart(4, '0')
            } else {
                it.node.nameBn
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp).testTag("locations_lazy_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().testTag("sundarganj_geo_summary_card")
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                AppStrings.pick(
                                    lang,
                                    "সুন্দরগঞ্জ ভূগোল ডেটাবেস (v${geoMeta.version})",
                                    "Sundarganj Geo Database (v${geoMeta.version})"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                AppStrings.pick(
                                    lang,
                                    "ইউনিয়ন: ${geoMeta.totalUnions} • ওয়ার্ড: ${geoMeta.totalWards} • সংগৃহীত গ্রাম: $collectedVillagesCount (উপজেলা তথ্য: ${geoMeta.upazilaVillageCountOfficial}টি)",
                                    "Unions: ${geoMeta.totalUnions} • Wards: ${geoMeta.totalWards} • Collected Villages: $collectedVillagesCount (Official Portal: ${geoMeta.upazilaVillageCountOfficial})"
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                AppStrings.pick(
                                    lang,
                                    "যাচাইাধীন / আংশিক গ্রাম এন্ট্রি: $unverifiedVillageCount",
                                    "Villages awaiting ward verification / partial: $unverifiedVillageCount"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showDiscrepancyDetails = !showDiscrepancyDetails }) {
                            Icon(
                                imageVector = if (showDiscrepancyDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = AppStrings.pick(lang, "অসঙ্গতি ও উৎস", "Sources & Notes")
                            )
                        }
                    }

                    AnimatedVisibility(visible = showDiscrepancyDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = AppStrings.pick(lang, geoMeta.discrepancyAnalysisBn, geoMeta.discrepancyAnalysisEn),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (unionGeoSummaries.isNotEmpty()) {
                                val summaryLine = unionGeoSummaries.joinToString(" | ") { s ->
                                    "${s.unionNode.nameBn}: ${s.collectedVillageCount}/${s.reference.officialVillageCount ?: "?"} (যাচাইকৃত: ${s.verifiedVillageCount})"
                                }
                                Text(
                                    text = summaryLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (geoMeta.sources.isNotEmpty()) {
                                Text(
                                    text = AppStrings.pick(lang, "প্রামাণিক উৎসসমূহ:", "Official Sources:") +
                                        " " + geoMeta.sources.take(5).joinToString(", ") { it.url } +
                                        if (geoMeta.sources.size > 5) " (+${geoMeta.sources.size - 5} UP portals)" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            ProfileSearchBar(
                query = geoSearchQuery,
                onQueryChange = { geoSearchQuery = it },
                placeholder = AppStrings.pick(
                    lang,
                    "ইউনিয়ন, ওয়ার্ড বা গ্রাম খুঁজুন (বাংলা / English)...",
                    "Search union, ward, or village..."
                ),
                lang = lang,
                testTag = "geo_search_input"
            )
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text(AppStrings.pick(lang, "সকল", "All")) }
                )
                GeoVerificationStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text(st.label(lang)) }
                    )
                }
            }
        }

        item {
            SearchablePickerField(
                label = AppStrings.pick(lang, "প্রশাসনিক স্তর", "Administrative Level"),
                selectedLabel = AppStrings.locationLevelLabel(lang, selectedLevel.name),
                items = LocationLevel.entries,
                itemLabel = { AppStrings.locationLevelLabel(lang, it.name) },
                onSelect = { selectedLevel = it },
                testTag = "geo_level_picker"
            )
        }

        if (selectedLevel.ordinal >= LocationLevel.DISTRICT.ordinal) {
            item {
                SearchablePickerField(
                    label = AppStrings.pick(lang, "বিভাগ ফিল্টার", "Filter Division"),
                    selectedLabel = divisions.firstOrNull { it.id == selectedParentDivId }?.displayName(lang == AppLang.BN).orEmpty(),
                    items = divisions,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    onSelect = {
                        selectedParentDivId = it.id
                        selectedParentDistId = ""
                        selectedParentUpzId = ""
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    },
                    onClear = {
                        selectedParentDivId = ""
                        selectedParentDistId = ""
                        selectedParentUpzId = ""
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    }
                )
            }
        }
        if (selectedLevel.ordinal >= LocationLevel.UPAZILA.ordinal) {
            item {
                SearchablePickerField(
                    label = AppStrings.pick(lang, "জেলা ফিল্টার", "Filter District"),
                    selectedLabel = districts.firstOrNull { it.id == selectedParentDistId }?.displayName(lang == AppLang.BN).orEmpty(),
                    items = districts,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    onSelect = {
                        selectedParentDistId = it.id
                        selectedParentUpzId = ""
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    },
                    onClear = {
                        selectedParentDistId = ""
                        selectedParentUpzId = ""
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    }
                )
            }
        }
        if (selectedLevel.ordinal >= LocationLevel.UNION.ordinal) {
            item {
                SearchablePickerField(
                    label = AppStrings.pick(lang, "উপজেলা ফিল্টার", "Filter Upazila"),
                    selectedLabel = upazilas.firstOrNull { it.id == selectedParentUpzId }?.displayName(lang == AppLang.BN).orEmpty(),
                    items = upazilas,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    onSelect = {
                        selectedParentUpzId = it.id
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    },
                    onClear = {
                        selectedParentUpzId = ""
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    }
                )
            }
        }
        if (selectedLevel.ordinal >= LocationLevel.WARD.ordinal) {
            item {
                SearchablePickerField(
                    label = AppStrings.pick(lang, "ইউনিয়ন ফিল্টার", "Filter Union"),
                    selectedLabel = unions.firstOrNull { it.id == selectedParentUnionId }?.displayName(lang == AppLang.BN).orEmpty(),
                    items = unions,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    onSelect = {
                        selectedParentUnionId = it.id
                        selectedParentWardId = ""
                    },
                    onClear = {
                        selectedParentUnionId = ""
                        selectedParentWardId = ""
                    }
                )
            }
        }
        if (selectedLevel.ordinal >= LocationLevel.VILLAGE.ordinal) {
            item {
                SearchablePickerField(
                    label = AppStrings.pick(lang, "ওয়ার্ড ফিল্টার", "Filter Ward"),
                    selectedLabel = wards.firstOrNull { it.id == selectedParentWardId }?.displayName(lang == AppLang.BN).orEmpty(),
                    items = wards,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    onSelect = { selectedParentWardId = it.id },
                    onClear = { selectedParentWardId = "" }
                )
            }
        }

        if (canManage) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.weight(1f).testTag("add_location_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStrings.pick(lang, "নতুন যোগ", "Add Location"))
                        }
                        OutlinedButton(
                            onClick = { showCsvDialog = true },
                            modifier = Modifier.weight(1f).testTag("import_locations_csv_button")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV Import")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onExportGeoData(true) },
                            modifier = Modifier.weight(1f).testTag("export_geo_csv_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStrings.pick(lang, "CSV রপ্তানি", "Export CSV"))
                        }
                        OutlinedButton(
                            onClick = { onExportGeoData(false) },
                            modifier = Modifier.weight(1f).testTag("export_geo_json_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStrings.pick(lang, "JSON রপ্তানি", "Export JSON"))
                        }
                    }
                }
            }
        }

        if (displayed.isEmpty()) {
            item {
                Text(
                    AppStrings.pick(
                        lang,
                        "এই ফিল্টারে কোনো লোকেশন পাওয়া যায়নি।",
                        "No location records matched these filters."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(displayed, key = { it.node.id }) { item ->
                val loc = item.node
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(loc.displayName(lang == AppLang.BN), style = MaterialTheme.typography.titleSmall)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GeoBadge(
                                        text = item.effectiveStatus.label(lang),
                                        verified = item.effectiveStatus == GeoVerificationStatus.VERIFIED
                                    )
                                    if (item.isOfficialReference) {
                                        GeoBadge(
                                            text = AppStrings.pick(lang, "রেফারেন্স", "Official Ref"),
                                            verified = true
                                        )
                                    }
                                    if (item.isAdminOverridden) {
                                        GeoBadge(
                                            text = AppStrings.pick(lang, "অ্যাডমিন অনুমোদিত", "Admin Saved"),
                                            verified = true
                                        )
                                    }
                                    if (loc.code.isNotBlank()) {
                                        Text(
                                            "Code: ${loc.code}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (canManage) {
                                IconButton(onClick = { editingNode = item }) {
                                    Icon(Icons.Filled.Edit, contentDescription = AppStrings.pick(lang, "সম্পাদনা", "Edit"))
                                }
                                if (loc.level == LocationLevel.VILLAGE.name) {
                                    IconButton(onClick = { assigningVillage = item }) {
                                        Icon(Icons.Filled.Link, contentDescription = AppStrings.pick(lang, "ওয়ার্ড নির্ধারণ", "Assign Ward"))
                                    }
                                }
                                if (!item.isOfficialReference && loc.level != LocationLevel.DIVISION.name) {
                                    IconButton(onClick = { onDeleteLocation(loc.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }

                        if (item.sourceUrl.isNotBlank() || item.lastVerifiedAt.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    item.sourceUrl.takeIf { it.isNotBlank() }?.let { "Source: $it" },
                                    item.lastVerifiedAt.takeIf { it.isNotBlank() }?.let { "Verified: $it" }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        val note = AppStrings.pick(lang, item.noteBn, item.noteEn)
                        if (note.isNotBlank()) {
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var nameBn by remember { mutableStateOf("") }
        var nameEn by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        val parentId = when (selectedLevel) {
            LocationLevel.DIVISION -> ""
            LocationLevel.DISTRICT -> selectedParentDivId
            LocationLevel.UPAZILA -> selectedParentDistId
            LocationLevel.UNION -> selectedParentUpzId
            LocationLevel.WARD -> selectedParentUnionId
            LocationLevel.VILLAGE -> selectedParentWardId.ifBlank { selectedParentUnionId }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("${AppStrings.pick(lang, "নতুন", "Add")} ${AppStrings.locationLevelLabel(lang, selectedLevel.name)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text(AppStrings.pick(lang, "বাংলা নাম *", "Name (Bangla) *")) }, singleLine = true)
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text(AppStrings.pick(lang, "ইংরেজি নাম", "Name (English)")) }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text(AppStrings.pick(lang, "কোড", "Code")) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameBn.isNotBlank()) {
                        onSaveLocation(
                            LocationNode(
                                level = selectedLevel.name,
                                parentId = parentId,
                                nameBn = nameBn.trim(),
                                nameEn = nameEn.trim(),
                                code = code.trim()
                            )
                        )
                        showAddDialog = false
                    }
                }) { Text(AppStrings.pick(lang, "সংরক্ষণ", "Save")) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }

    editingNode?.let { editItem ->
        var nameBn by remember(editItem) { mutableStateOf(editItem.node.nameBn) }
        var nameEn by remember(editItem) { mutableStateOf(editItem.node.nameEn) }
        var code by remember(editItem) { mutableStateOf(editItem.node.code) }

        AlertDialog(
            onDismissRequest = { editingNode = null },
            title = { Text(AppStrings.pick(lang, "বানান ও তথ্য সংশোধন (অ্যাডমিন)", "Edit Spelling & Metadata (Admin)")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text(AppStrings.pick(lang, "বাংলা নাম *", "Name (Bangla) *")) }, singleLine = true)
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text(AppStrings.pick(lang, "ইংরেজি নাম", "Name (English)")) }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text(AppStrings.pick(lang, "কোড", "Code")) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameBn.isNotBlank()) {
                        onSaveLocation(
                            editItem.node.copy(
                                nameBn = nameBn.trim(),
                                nameEn = nameEn.trim(),
                                code = code.trim()
                            )
                        )
                        editingNode = null
                    }
                }) { Text(AppStrings.pick(lang, "সংরক্ষণ", "Save")) }
            },
            dismissButton = { TextButton(onClick = { editingNode = null }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }

    assigningVillage?.let { vilItem ->
        val candidateWards = remember(locations, vilItem.unionId) {
            locations.filter { it.level == LocationLevel.WARD.name && it.parentId == vilItem.unionId }
                .sortedBy { it.code.toIntOrNull() ?: 99 }
        }
        var pickedWard by remember(vilItem) { mutableStateOf<LocationNode?>(null) }

        AlertDialog(
            onDismissRequest = { assigningVillage = null },
            title = { Text(AppStrings.pick(lang, "যাচাইকৃত ওয়ার্ড নির্ধারণ করুন", "Assign Village to Verified Ward")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        AppStrings.pick(
                            lang,
                            "গ্রাম: ${vilItem.node.nameBn}। শুধুমাত্র নিশ্চিত দাপ্তরিক তথ্যের ভিত্তিতে ওয়ার্ড নির্বাচন করুন।",
                            "Village: ${vilItem.node.displayName(false)}. Select ward only after official verification."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "ওয়ার্ড নির্বাচন (১–৯)", "Select Ward (1-9)"),
                        selectedLabel = pickedWard?.displayName(lang == AppLang.BN).orEmpty(),
                        items = candidateWards,
                        itemLabel = { it.displayName(lang == AppLang.BN) },
                        onSelect = { pickedWard = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = pickedWard != null,
                    onClick = {
                        pickedWard?.let { w ->
                            onAssignVillageToWard(vilItem.node, w.id, w.code)
                            assigningVillage = null
                        }
                    }
                ) { Text(AppStrings.pick(lang, "নিশ্চিত করুন", "Confirm")) }
            },
            dismissButton = { TextButton(onClick = { assigningVillage = null }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }

    if (showCsvDialog) {
        var csvText by remember { mutableStateOf("VILLAGE,un_sundarganj_08_dhopadanga,উদাহরণ গ্রাম,Example Village,UNVERIFIED_WARD") }
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text(AppStrings.pick(lang, "লোকেশন CSV আমদানি", "Import Locations CSV")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Format: level,parentNameOrId,nameBn,nameEn,code", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = csvText, onValueChange = { csvText = it }, minLines = 5, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { onImportCsv(csvText); showCsvDialog = false }) {
                    Text(AppStrings.pick(lang, "আমদানি করুন", "Import"))
                }
            },
            dismissButton = { TextButton(onClick = { showCsvDialog = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }
}

@Composable
private fun GeoBadge(text: String, verified: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (verified) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (verified) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (verified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (verified) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun InstitutionsManagerTab(
    canManage: Boolean,
    locations: List<LocationNode>,
    institutions: List<EducationalInstitution>,
    lang: AppLang,
    onSaveInstitution: (EducationalInstitution) -> Unit,
    onDeleteInstitution: (String) -> Unit,
    onImportCsv: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var showCsv by remember { mutableStateOf(false) }

    val filtered = remember(institutions, search) {
        val q = search.trim().lowercase()
        if (q.isBlank()) institutions
        else institutions.filter { it.nameBn.lowercase().contains(q) || it.nameEn.lowercase().contains(q) || it.locationLabel.lowercase().contains(q) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান খুঁজুন", "Search Institutions")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (canManage) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(AppStrings.pick(lang, "নতুন প্রতিষ্ঠান", "Add Institution"))
                    }
                    OutlinedButton(onClick = { showCsv = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CSV Import")
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    AppStrings.pick(lang, "কোনো শিক্ষা প্রতিষ্ঠান এখনো যুক্ত নেই।", "No institutions added yet."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(filtered, key = { it.id }) { inst ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(inst.nameBn, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${AppStrings.institutionTypeLabel(lang, inst.institutionType)} • ${inst.locationLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (canManage) {
                            IconButton(onClick = { onDeleteInstitution(inst.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var nameBn by remember { mutableStateOf("") }
        var nameEn by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(InstitutionType.SCHOOL) }
        var eiin by remember { mutableStateOf("") }
        var div by remember { mutableStateOf<LocationNode?>(null) }
        var dist by remember { mutableStateOf<LocationNode?>(null) }
        var upz by remember { mutableStateOf<LocationNode?>(null) }

        val divisions = remember(locations) { locations.filter { it.level == LocationLevel.DIVISION.name } }
        val districts = remember(locations, div) { locations.filter { it.level == LocationLevel.DISTRICT.name && it.parentId == div?.id } }
        val upazilas = remember(locations, dist) { locations.filter { it.level == LocationLevel.UPAZILA.name && it.parentId == dist?.id } }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান যোগ করুন", "Add Institution")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameBn, onValueChange = { nameBn = it }, label = { Text(AppStrings.pick(lang, "বাংলা নাম *", "Name (Bangla) *")) }, singleLine = true)
                    OutlinedTextField(value = nameEn, onValueChange = { nameEn = it }, label = { Text(AppStrings.pick(lang, "ইংরেজি নাম", "Name (English)")) }, singleLine = true)
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "ধরন", "Type"),
                        selectedLabel = AppStrings.institutionTypeLabel(lang, type.name),
                        items = InstitutionType.entries,
                        itemLabel = { AppStrings.institutionTypeLabel(lang, it.name) },
                        onSelect = { type = it }
                    )
                    OutlinedTextField(value = eiin, onValueChange = { eiin = it }, label = { Text("EIIN") }, singleLine = true)
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "বিভাগ", "Division"),
                        selectedLabel = div?.nameBn.orEmpty(),
                        items = divisions,
                        itemLabel = { it.displayName(lang == AppLang.BN) },
                        onSelect = { div = it; dist = null; upz = null }
                    )
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "জেলা", "District"),
                        selectedLabel = dist?.nameBn.orEmpty(),
                        items = districts,
                        enabled = div != null,
                        itemLabel = { it.displayName(lang == AppLang.BN) },
                        onSelect = { dist = it; upz = null }
                    )
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "উপজেলা", "Upazila"),
                        selectedLabel = upz?.nameBn.orEmpty(),
                        items = upazilas,
                        enabled = dist != null,
                        itemLabel = { it.displayName(lang == AppLang.BN) },
                        onSelect = { upz = it }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameBn.isNotBlank()) {
                        val locLabel = listOfNotNull(div?.nameBn, dist?.nameBn, upz?.nameBn).joinToString(" > ")
                        onSaveInstitution(
                            EducationalInstitution(
                                nameBn = nameBn.trim(),
                                nameEn = nameEn.trim(),
                                institutionType = type.name,
                                eiinCode = eiin.trim(),
                                divisionId = div?.id.orEmpty(),
                                districtId = dist?.id.orEmpty(),
                                upazilaId = upz?.id.orEmpty(),
                                locationLabel = locLabel
                            )
                        )
                        showAdd = false
                    }
                }) { Text(AppStrings.pick(lang, "সংরক্ষণ", "Save")) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }

    if (showCsv) {
        var csvText by remember { mutableStateOf("ঢাকা কলেজ,Dhaka College,COLLEGE,108123,ঢাকা,ঢাকা,") }
        AlertDialog(
            onDismissRequest = { showCsv = false },
            title = { Text(AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান CSV আমদানি", "Import Institutions CSV")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Format: nameBn,nameEn,type,eiin,division,district,upazila", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = csvText, onValueChange = { csvText = it }, minLines = 5, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { onImportCsv(csvText); showCsv = false }) {
                    Text(AppStrings.pick(lang, "আমদানি করুন", "Import"))
                }
            },
            dismissButton = { TextButton(onClick = { showCsv = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }
}

@Composable
private fun OrgPositionsManagerTab(
    canManage: Boolean,
    positions: List<OrgPosition>,
    lang: AppLang,
    onSavePosition: (OrgPosition) -> Unit,
    onDeletePosition: (String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canManage) {
            item {
                Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.pick(lang, "নতুন সাংগঠনিক পদবী", "Add Organizational Position"))
                }
            }
        }
        if (positions.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = AppStrings.pick(lang, "এখনো কোনো তথ্য যোগ করা হয়নি।", "No data has been added yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        } else {
            items(positions, key = { it.id }) { pos ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${pos.designationBn} (${pos.designationEn})", style = MaterialTheme.typography.titleSmall)
                            Text("${pos.levelNameBn} • Rank #${pos.rankOrder}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (canManage) {
                            IconButton(onClick = { onDeletePosition(pos.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var levelBn by remember { mutableStateOf("") }
        var levelEn by remember { mutableStateOf("") }
        var desBn by remember { mutableStateOf("") }
        var desEn by remember { mutableStateOf("") }
        var rank by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(AppStrings.pick(lang, "সাংগঠনিক পদবী যোগ", "Add Position")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = levelBn, onValueChange = { levelBn = it }, label = { Text(AppStrings.pick(lang, "স্তর (বাংলা) *", "Level (Bangla) *")) }, singleLine = true)
                    OutlinedTextField(value = levelEn, onValueChange = { levelEn = it }, label = { Text(AppStrings.pick(lang, "স্তর (ইংরেজি)", "Level (English)")) }, singleLine = true)
                    OutlinedTextField(value = desBn, onValueChange = { desBn = it }, label = { Text(AppStrings.pick(lang, "পদবী (বাংলা) *", "Designation (Bangla) *")) }, singleLine = true)
                    OutlinedTextField(value = desEn, onValueChange = { desEn = it }, label = { Text(AppStrings.pick(lang, "পদবী (ইংরেজি)", "Designation (English)")) }, singleLine = true)
                    OutlinedTextField(value = rank, onValueChange = { rank = it }, label = { Text("Rank Order") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (levelBn.isNotBlank() && desBn.isNotBlank()) {
                        onSavePosition(
                            OrgPosition(
                                levelNameBn = levelBn.trim(),
                                levelNameEn = levelEn.trim(),
                                designationBn = desBn.trim(),
                                designationEn = desEn.trim(),
                                rankOrder = rank.toLongOrNull() ?: 10L
                            )
                        )
                        showAdd = false
                    }
                }) { Text(AppStrings.pick(lang, "সংরক্ষণ", "Save")) }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) } }
        )
    }
}
