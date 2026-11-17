package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.geo.GeoVerificationStatus
import com.example.data.geo.MergedGeoNode
import com.example.data.model.EducationalInstitution
import com.example.data.model.InstitutionType
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.model.OrgPosition
import com.example.data.model.PimsAccount
import com.example.data.model.PimsRole
import com.example.data.model.WebsiteSettings
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.SearchablePickerField
import kotlinx.coroutines.launch

@Composable
fun UserFormScreen(
    actor: PimsAccount,
    existing: PimsAccount?,
    locations: List<LocationNode>,
    mergedGeoNodes: List<MergedGeoNode> = emptyList(),
    institutions: List<EducationalInstitution>,
    orgPositions: List<OrgPosition>,
    settings: WebsiteSettings,
    lang: AppLang,
    onGenerateId: suspend (String) -> Result<String>,
    onQuickAddInstitution: (EducationalInstitution) -> Unit,
    onSave: (isNew: Boolean, draft: PimsAccount, initialPassword: String) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }
    val isNew = existing == null
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var draft by remember(existing) { mutableStateOf(existing ?: PimsAccount()) }
    var initialPassword by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showQuickInstDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isNew) {
        if (isNew && draft.personalId.isBlank()) {
            onGenerateId("BD").onSuccess { generated ->
                draft = draft.copy(personalId = generated)
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = PdfAndExportEngine.compressUriToBase64(context, uri, maxDim = 256, maxChars = 140_000)
            if (encoded != null) {
                draft = draft.copy(photoBase64 = encoded)
            }
        }
    }

    val isMandatory: (String) -> Boolean = { key -> key in settings.mandatoryFields }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = if (isNew) AppStrings.pick(lang, "নতুন ব্যবহারকারী তৈরি", "Create User Account")
                else AppStrings.pick(lang, "প্রোফাইল সম্পাদনা", "Edit Profile"),
                style = MaterialTheme.typography.titleLarge
            )
        }

        // Photo & Personal ID card
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileAvatar(draft.photoBase64, draft.fullNameBn, sizeDp = 64)
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.testTag("pick_profile_photo_button")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(AppStrings.pick(lang, "ছবি নির্বাচন করুন", "Select Photo"))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.personalId,
                        onValueChange = { if (isNew) draft = draft.copy(personalId = it.uppercase()) },
                        enabled = isNew,
                        label = { Text(AppStrings.pick(lang, "পার্সোনাল আইডি (অপরিবর্তনযোগ্য)", "Unique Personal ID")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("form_personal_id_input")
                    )
                    if (isNew) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                onGenerateId("BD").onSuccess { draft = draft.copy(personalId = it) }
                            }
                        }) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (isNew) {
                    OutlinedTextField(
                        value = initialPassword,
                        onValueChange = { initialPassword = it },
                        label = { Text(AppStrings.pick(lang, "প্রাথমিক পাসওয়ার্ড (কমপক্ষে ৮ অক্ষর) *", "Initial Password *")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("form_initial_password_input")
                    )
                }

                if (actor.roleEnum().isAdminTier()) {
                    val allowedRoles = if (actor.roleEnum().canManageAdmins()) {
                        PimsRole.entries
                    } else {
                        listOf(PimsRole.EDITOR, PimsRole.VIEWER, PimsRole.NORMAL_USER)
                    }
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "ব্যবহারকারীর রোল", "User Role"),
                        selectedLabel = AppStrings.roleLabel(lang, draft.role),
                        items = allowedRoles,
                        itemLabel = { AppStrings.roleLabel(lang, it.name) },
                        onSelect = { role -> draft = draft.copy(role = role.name) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.pick(lang, "ব্যবহারকারী নিজে প্রোফাইল সম্পাদনা করতে পারবেন", "Allow self profile editing"), style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = draft.canEditSelfProfile,
                            onCheckedChange = { draft = draft.copy(canEditSelfProfile = it) }
                        )
                    }
                }
            }
        }

        // Section A: Basic Information
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "ক. মৌলিক তথ্য", "A. Basic Information"), style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = draft.fullNameBn,
                    onValueChange = { draft = draft.copy(fullNameBn = it) },
                    label = { Text(AppStrings.pick(lang, "পূর্ণ নাম (বাংলা) *", "Full Name (Bangla) *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("form_fullname_bn_input")
                )
                OutlinedTextField(
                    value = draft.fullNameEn,
                    onValueChange = { draft = draft.copy(fullNameEn = it) },
                    label = { Text(AppStrings.pick(lang, "পূর্ণ নাম (ইংরেজি)", "Full Name (English)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.fatherName,
                    onValueChange = { draft = draft.copy(fatherName = it) },
                    label = { Text(AppStrings.pick(lang, "পিতার নাম", "Father's Name") + if (isMandatory("fatherName")) " *" else "") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.motherName,
                    onValueChange = { draft = draft.copy(motherName = it) },
                    label = { Text(AppStrings.pick(lang, "মাতার নাম", "Mother's Name") + if (isMandatory("motherName")) " *" else "") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.dateOfBirth,
                        onValueChange = { draft = draft.copy(dateOfBirth = it) },
                        label = { Text(AppStrings.pick(lang, "জন্ম তারিখ", "Date of Birth")) },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        SearchablePickerField(
                            label = AppStrings.pick(lang, "লিঙ্গ", "Gender"),
                            selectedLabel = draft.gender,
                            items = listOf("পুরুষ", "মহিলা", "অন্যান্য"),
                            itemLabel = { it },
                            onSelect = { draft = draft.copy(gender = it) }
                        )
                    }
                }
                SearchablePickerField(
                    label = AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group") + if (isMandatory("bloodGroup")) " *" else "",
                    selectedLabel = draft.bloodGroup,
                    items = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"),
                    itemLabel = { it },
                    onSelect = { draft = draft.copy(bloodGroup = it) },
                    onClear = { draft = draft.copy(bloodGroup = "") }
                )
                OutlinedTextField(
                    value = draft.mobileNumber,
                    onValueChange = { draft = draft.copy(mobileNumber = it) },
                    label = { Text(AppStrings.pick(lang, "মোবাইল নম্বর", "Mobile Number") + if (isMandatory("mobileNumber")) " *" else "") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("form_mobile_input")
                )
                OutlinedTextField(
                    value = draft.emailAddress,
                    onValueChange = { draft = draft.copy(emailAddress = it) },
                    label = { Text(AppStrings.pick(lang, "ইমেইল ঠিকানা", "Email Address")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section E: Cascading Bangladesh Address (placed before Education so institution filters by location!)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "ঙ. বাংলাদেশ ঠিকানা (ধাপে ধাপে নির্বাচন)", "E. Cascading Bangladesh Address"), style = MaterialTheme.typography.titleMedium)

                val mergedById = remember(mergedGeoNodes) { mergedGeoNodes.associateBy { it.node.id } }
                val divisions = remember(locations) { locations.filter { it.level == LocationLevel.DIVISION.name }.sortedBy { it.nameBn } }
                val districts = remember(locations, draft.divisionId) {
                    locations.filter { it.level == LocationLevel.DISTRICT.name && it.parentId == draft.divisionId }.sortedBy { it.nameBn }
                }
                val upazilas = remember(locations, draft.districtId) {
                    locations.filter { it.level == LocationLevel.UPAZILA.name && it.parentId == draft.districtId }.sortedBy { it.nameBn }
                }
                val unions = remember(locations, draft.upazilaId) {
                    locations.filter { it.level == LocationLevel.UNION.name && it.parentId == draft.upazilaId }.sortedBy { it.code.ifBlank { it.nameBn } }
                }
                val wards = remember(locations, draft.unionId) {
                    locations.filter { it.level == LocationLevel.WARD.name && it.parentId == draft.unionId }.sortedBy { it.code.toIntOrNull() ?: 99 }
                }
                val unionVillages = remember(locations, mergedGeoNodes, draft.unionId) {
                    if (draft.unionId.isBlank()) emptyList()
                    else if (mergedGeoNodes.isNotEmpty()) {
                        mergedGeoNodes
                            .filter { it.node.level == LocationLevel.VILLAGE.name && it.unionId == draft.unionId }
                            .sortedBy { it.node.nameBn }
                    } else {
                        val wardIds = wards.map { it.id }.toSet()
                        locations.filter {
                            it.level == LocationLevel.VILLAGE.name &&
                                (it.parentId == draft.unionId || it.parentId in wardIds)
                        }.map {
                            MergedGeoNode(
                                node = it,
                                isOfficialReference = false,
                                isAdminOverridden = true,
                                reference = null,
                                unionId = draft.unionId,
                                effectiveWardIds = if (it.parentId in wardIds) listOf(it.parentId) else emptyList(),
                                effectiveStatus = GeoVerificationStatus.VERIFIED
                            )
                        }.sortedBy { it.node.nameBn }
                    }
                }
                val wardMappedVillages = remember(unionVillages, draft.wardId) {
                    if (draft.wardId.isBlank()) emptyList()
                    else unionVillages.filter { v ->
                        draft.wardId in v.effectiveWardIds || v.node.parentId == draft.wardId
                    }
                }
                val isWardMappingUnverified = draft.wardId.isNotBlank() && wardMappedVillages.isEmpty()
                val selectableVillages = if (draft.wardId.isNotBlank() && wardMappedVillages.isNotEmpty()) {
                    wardMappedVillages
                } else {
                    unionVillages
                }

                SearchablePickerField(
                    label = AppStrings.pick(lang, "বিভাগ", "Division") + if (isMandatory("divisionName")) " *" else "",
                    selectedLabel = draft.divisionName,
                    items = divisions,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    onSelect = { div ->
                        draft = draft.copy(
                            divisionId = div.id, divisionName = div.nameBn,
                            districtId = "", districtName = "",
                            upazilaId = "", upazilaName = "",
                            unionId = "", unionName = "",
                            wardId = "", wardName = "",
                            villageName = "",
                            institutionId = "", institutionName = ""
                        )
                    },
                    onClear = {
                        draft = draft.copy(
                            divisionId = "", divisionName = "",
                            districtId = "", districtName = "",
                            upazilaId = "", upazilaName = "",
                            unionId = "", unionName = "",
                            wardId = "", wardName = "",
                            villageName = ""
                        )
                    },
                    testTag = "picker_division"
                )

                SearchablePickerField(
                    label = AppStrings.pick(lang, "জেলা", "District") + if (isMandatory("districtName")) " *" else "",
                    selectedLabel = draft.districtName,
                    items = districts,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    enabled = draft.divisionId.isNotBlank(),
                    onSelect = { dist ->
                        draft = draft.copy(
                            districtId = dist.id, districtName = dist.nameBn,
                            upazilaId = "", upazilaName = "",
                            unionId = "", unionName = "",
                            wardId = "", wardName = "",
                            villageName = "",
                            institutionId = "", institutionName = ""
                        )
                    },
                    onClear = {
                        draft = draft.copy(
                            districtId = "", districtName = "",
                            upazilaId = "", upazilaName = "",
                            unionId = "", unionName = "",
                            wardId = "", wardName = "",
                            villageName = ""
                        )
                    },
                    testTag = "picker_district"
                )

                SearchablePickerField(
                    label = AppStrings.pick(lang, "উপজেলা / থানা", "Upazila"),
                    selectedLabel = draft.upazilaName,
                    items = upazilas,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    enabled = draft.districtId.isNotBlank(),
                    emptyMessage = AppStrings.pick(
                        lang,
                        "এই জেলায় কোনো উপজেলা এখনো যুক্ত নেই। লোকেশন ট্যাব থেকে যুক্ত করুন।",
                        "No upazilas added for this district yet. Add via Locations tab."
                    ),
                    onSelect = { upz ->
                        draft = draft.copy(
                            upazilaId = upz.id, upazilaName = upz.nameBn,
                            unionId = "", unionName = "",
                            wardId = "", wardName = "",
                            villageName = ""
                        )
                    },
                    onClear = {
                        draft = draft.copy(upazilaId = "", upazilaName = "", unionId = "", unionName = "", wardId = "", wardName = "", villageName = "")
                    },
                    testTag = "picker_upazila"
                )

                SearchablePickerField(
                    label = AppStrings.pick(lang, "ইউনিয়ন / পৌরসভা", "Union"),
                    selectedLabel = draft.unionName,
                    items = unions,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    itemSubtitle = { u ->
                        val status = mergedById[u.id]?.effectiveStatus
                        if (status != null) status.label(lang) else ""
                    },
                    itemSearchKeywords = { u ->
                        mergedById[u.id]?.aliasesBn?.joinToString(" ").orEmpty()
                    },
                    enabled = draft.upazilaId.isNotBlank(),
                    emptyMessage = AppStrings.pick(lang, "এই উপজেলায় কোনো ইউনিয়ন যুক্ত নেই।", "No unions added yet."),
                    onSelect = { un ->
                        draft = draft.copy(unionId = un.id, unionName = un.nameBn, wardId = "", wardName = "", villageName = "")
                    },
                    onClear = { draft = draft.copy(unionId = "", unionName = "", wardId = "", wardName = "", villageName = "") },
                    testTag = "picker_union"
                )

                SearchablePickerField(
                    label = AppStrings.pick(lang, "ওয়ার্ড (১–৯)", "Ward (1-9)"),
                    selectedLabel = draft.wardName,
                    items = wards,
                    itemLabel = { it.displayName(lang == AppLang.BN) },
                    itemSubtitle = { w ->
                        mergedById[w.id]?.effectiveStatus?.label(lang).orEmpty()
                    },
                    enabled = draft.unionId.isNotBlank(),
                    emptyMessage = AppStrings.pick(lang, "এই ইউনিয়নে কোনো ওয়ার্ড যুক্ত নেই।", "No wards added yet."),
                    onSelect = { w ->
                        val nextMapped = unionVillages.filter { v ->
                            w.id in v.effectiveWardIds || v.node.parentId == w.id
                        }
                        val autoVillage = if (nextMapped.size == 1) nextMapped.first().node.nameBn else ""
                        draft = draft.copy(wardId = w.id, wardName = w.nameBn, villageName = autoVillage)
                    },
                    onClear = { draft = draft.copy(wardId = "", wardName = "", villageName = "") },
                    testTag = "picker_ward"
                )

                if (isWardMappingUnverified) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("ward_mapping_unverified_notice")
                    ) {
                        Text(
                            text = AppStrings.pick(
                                lang,
                                "ওয়ার্ড-গ্রাম ম্যাপিং: যাচাইাধীন • এই ওয়ার্ডের নির্দিষ্ট গ্রাম দাপ্তরিকভাবে যাচাইাধীন। নিচের ইউনিয়নভিত্তিক তালিকা থেকে গ্রাম খুঁজুন বা ম্যানুয়ালি লিখুন।",
                                "Ward-to-village mapping: Awaiting Verification (যাচাইাধীন). You can pick from the Union's village directory below or enter manually."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (selectableVillages.isNotEmpty()) {
                    SearchablePickerField(
                        label = if (isWardMappingUnverified || draft.wardId.isBlank()) {
                            AppStrings.pick(lang, "গ্রাম নির্বাচন করুন (ইউনিয়ন তালিকা)", "Select Village (Union Directory)")
                        } else {
                            AppStrings.pick(lang, "গ্রাম নির্বাচন করুন (যাচাইকৃত ওয়ার্ড)", "Select Village (Verified Ward)")
                        },
                        selectedLabel = draft.villageName,
                        items = selectableVillages,
                        itemLabel = { it.node.displayName(lang == AppLang.BN) },
                        itemSubtitle = { v ->
                            v.effectiveStatus.label(lang)
                        },
                        itemSearchKeywords = { v -> v.aliasesBn.joinToString(" ") },
                        onSelect = { v -> draft = draft.copy(villageName = v.node.nameBn) },
                        onClear = { draft = draft.copy(villageName = "") },
                        testTag = "picker_village"
                    )
                }

                OutlinedTextField(
                    value = draft.villageName,
                    onValueChange = { draft = draft.copy(villageName = it) },
                    label = { Text(AppStrings.pick(lang, "গ্রাম / মহল্লা / রোড (ম্যানুয়াল বা তালিকা)", "Village / Area")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("village_manual_input")
                )

                val fullAddr = draft.fullAddressLabel()
                if (fullAddr.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("selected_address_preview")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = AppStrings.pick(lang, "নির্বাচিত পূর্ণ ঠিকানা:", "Selected Full Address:"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = fullAddr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Section B: Location-linked Educational Information
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "খ. শিক্ষাগত তথ্য (এলাকাভিত্তিক প্রতিষ্ঠান)", "B. Educational Information"), style = MaterialTheme.typography.titleMedium)

                val areaInstitutions = remember(institutions, draft.divisionId, draft.districtId, draft.upazilaId, draft.unionId, draft.wardId) {
                    institutions.filter { inst ->
                        (draft.divisionId.isBlank() || inst.divisionId.isBlank() || inst.divisionId == draft.divisionId) &&
                            (draft.districtId.isBlank() || inst.districtId.isBlank() || inst.districtId == draft.districtId) &&
                            (draft.upazilaId.isBlank() || inst.upazilaId.isBlank() || inst.upazilaId == draft.upazilaId) &&
                            (draft.unionId.isBlank() || inst.unionId.isBlank() || inst.unionId == draft.unionId) &&
                            (draft.wardId.isBlank() || inst.wardId.isBlank() || inst.wardId == draft.wardId)
                    }
                }

                SearchablePickerField(
                    label = AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান নির্বাচন", "Educational Institution"),
                    selectedLabel = draft.institutionName,
                    items = areaInstitutions,
                    itemLabel = { it.nameBn },
                    itemSubtitle = { "${AppStrings.institutionTypeLabel(lang, it.institutionType)} • ${it.locationLabel}" },
                    emptyMessage = AppStrings.pick(
                        lang,
                        "নির্বাচিত এলাকায় কোনো প্রতিষ্ঠান পাওয়া যায়নি। নিচের বাটন থেকে যোগ করুন।",
                        "No institution found in selected area. Use button below to add."
                    ),
                    onSelect = { inst ->
                        draft = draft.copy(
                            institutionId = inst.id,
                            institutionName = inst.nameBn,
                            institutionType = inst.institutionType
                        )
                    },
                    onClear = {
                        draft = draft.copy(institutionId = "", institutionName = "", institutionType = "")
                    }
                )

                if (actor.roleEnum().canManageMasterDatasets()) {
                    OutlinedButton(onClick = { showQuickInstDialog = true }) {
                        Icon(Icons.Filled.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(AppStrings.pick(lang, "এই এলাকায় নতুন প্রতিষ্ঠান যোগ করুন", "Add Institution in Area"))
                    }
                }

                OutlinedTextField(
                    value = draft.currentClassOrYear,
                    onValueChange = { draft = draft.copy(currentClassOrYear = it) },
                    label = { Text(AppStrings.pick(lang, "বর্তমান শ্রেণি / বর্ষ", "Current Class / Year")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.educationalQualification,
                    onValueChange = { draft = draft.copy(educationalQualification = it) },
                    label = { Text(AppStrings.pick(lang, "শিক্ষাগত যোগ্যতা", "Educational Qualification")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.departmentOrSubject,
                    onValueChange = { draft = draft.copy(departmentOrSubject = it) },
                    label = { Text(AppStrings.pick(lang, "বিভাগ / বিষয়", "Department / Subject")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.academicInfo,
                    onValueChange = { draft = draft.copy(academicInfo = it) },
                    label = { Text(AppStrings.pick(lang, "অতিরিক্ত একাডেমিক তথ্য", "Academic Information")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section C: Personal Goals
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "গ. ব্যক্তিগত লক্ষ্য ও দক্ষতা", "C. Personal Goals & Skills"), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = draft.careerGoal,
                    onValueChange = { draft = draft.copy(careerGoal = it) },
                    label = { Text(AppStrings.pick(lang, "ভবিষ্যৎ ক্যারিয়ার লক্ষ্য", "Future Career Goal")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.skills,
                    onValueChange = { draft = draft.copy(skills = it) },
                    label = { Text(AppStrings.pick(lang, "দক্ষতা (Skills)", "Skills")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.interests,
                    onValueChange = { draft = draft.copy(interests = it) },
                    label = { Text(AppStrings.pick(lang, "আগ্রহ (Interests)", "Interests")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.personalIntro,
                    onValueChange = { draft = draft.copy(personalIntro = it) },
                    label = { Text(AppStrings.pick(lang, "ব্যক্তিগত পরিচিতি", "Personal Introduction")) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section D: Organizational Information
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.pick(lang, "ঘ. সাংগঠনিক তথ্য", "D. Organizational Information"), style = MaterialTheme.typography.titleMedium)

                SearchablePickerField(
                    label = AppStrings.pick(lang, "তালিকা থেকে পদবী নির্বাচন", "Select Org Position"),
                    selectedLabel = draft.orgDesignation,
                    items = orgPositions,
                    itemLabel = { "${it.levelNameBn} - ${it.designationBn}" },
                    onSelect = { pos ->
                        draft = draft.copy(orgLevel = pos.levelNameBn, orgDesignation = pos.designationBn)
                    }
                )
                OutlinedTextField(
                    value = draft.orgLevel,
                    onValueChange = { draft = draft.copy(orgLevel = it) },
                    label = { Text(AppStrings.pick(lang, "সাংগঠনিক স্তর", "Organizational Level")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.orgDesignation,
                    onValueChange = { draft = draft.copy(orgDesignation = it) },
                    label = { Text(AppStrings.pick(lang, "সাংগঠনিক পদবী", "Organizational Designation")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.orgJoiningDate,
                    onValueChange = { draft = draft.copy(orgJoiningDate = it) },
                    label = { Text(AppStrings.pick(lang, "যোগদানের তারিখ", "Joining Date")) },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.orgStatus,
                    onValueChange = { draft = draft.copy(orgStatus = it) },
                    label = { Text(AppStrings.pick(lang, "সাংগঠনিক অবস্থা", "Organizational Status")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.orgAdditionalInfo,
                    onValueChange = { draft = draft.copy(orgAdditionalInfo = it) },
                    label = { Text(AppStrings.pick(lang, "অতিরিক্ত সাংগঠনিক তথ্য", "Additional Info")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (validationError != null) {
            Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                val missing = mutableListOf<String>()
                if (draft.fullNameBn.isBlank()) missing.add(AppStrings.pick(lang, "পূর্ণ নাম (বাংলা)", "Full Name (Bangla)"))
                if (isNew && initialPassword.length < 8) missing.add(AppStrings.pick(lang, "প্রাথমিক পাসওয়ার্ড (৮+ অক্ষর)", "Initial Password (8+ chars)"))
                if (isMandatory("mobileNumber") && draft.mobileNumber.isBlank()) missing.add(AppStrings.pick(lang, "মোবাইল নম্বর", "Mobile Number"))
                if (isMandatory("fatherName") && draft.fatherName.isBlank()) missing.add(AppStrings.pick(lang, "পিতার নাম", "Father's Name"))
                if (isMandatory("motherName") && draft.motherName.isBlank()) missing.add(AppStrings.pick(lang, "মাতার নাম", "Mother's Name"))
                if (isMandatory("bloodGroup") && draft.bloodGroup.isBlank()) missing.add(AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group"))
                if (isMandatory("divisionName") && draft.divisionName.isBlank()) missing.add(AppStrings.pick(lang, "বিভাগ", "Division"))
                if (isMandatory("districtName") && draft.districtName.isBlank()) missing.add(AppStrings.pick(lang, "জেলা", "District"))

                if (missing.isNotEmpty()) {
                    validationError = AppStrings.pick(lang, "আবশ্যক তথ্য দিন: ", "Required: ") + missing.joinToString(", ")
                } else {
                    validationError = null
                    onSave(isNew, draft, initialPassword)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_user_form_button")
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(AppStrings.pick(lang, "তথ্য সংরক্ষণ করুন", "Save Profile"))
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showQuickInstDialog) {
        var instNameBn by remember { mutableStateOf("") }
        var instNameEn by remember { mutableStateOf("") }
        var instType by remember { mutableStateOf(InstitutionType.SCHOOL) }
        var eiin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickInstDialog = false },
            title = { Text(AppStrings.pick(lang, "নতুন শিক্ষা প্রতিষ্ঠান যোগ", "Add Educational Institution")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = instNameBn,
                        onValueChange = { instNameBn = it },
                        label = { Text(AppStrings.pick(lang, "প্রতিষ্ঠানের নাম (বাংলা) *", "Name (Bangla) *")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = instNameEn,
                        onValueChange = { instNameEn = it },
                        label = { Text(AppStrings.pick(lang, "নাম (ইংরেজি)", "Name (English)")) },
                        singleLine = true
                    )
                    SearchablePickerField(
                        label = AppStrings.pick(lang, "ধরন", "Type"),
                        selectedLabel = AppStrings.institutionTypeLabel(lang, instType.name),
                        items = InstitutionType.entries,
                        itemLabel = { AppStrings.institutionTypeLabel(lang, it.name) },
                        onSelect = { instType = it }
                    )
                    OutlinedTextField(
                        value = eiin,
                        onValueChange = { eiin = it },
                        label = { Text("EIIN") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (instNameBn.isNotBlank()) {
                            val label = listOf(draft.divisionName, draft.districtName, draft.upazilaName)
                                .filter { it.isNotBlank() }
                                .joinToString(" > ")
                            val newInst = EducationalInstitution(
                                nameBn = instNameBn.trim(),
                                nameEn = instNameEn.trim(),
                                institutionType = instType.name,
                                eiinCode = eiin.trim(),
                                divisionId = draft.divisionId,
                                districtId = draft.districtId,
                                upazilaId = draft.upazilaId,
                                unionId = draft.unionId,
                                wardId = draft.wardId,
                                locationLabel = label
                            )
                            onQuickAddInstitution(newInst)
                            draft = draft.copy(institutionName = newInst.nameBn, institutionType = newInst.institutionType)
                            showQuickInstDialog = false
                        }
                    }
                ) { Text(AppStrings.pick(lang, "যোগ করুন", "Add")) }
            },
            dismissButton = {
                TextButton(onClick = { showQuickInstDialog = false }) { Text(AppStrings.pick(lang, "বাতিল", "Cancel")) }
            }
        )
    }
}
