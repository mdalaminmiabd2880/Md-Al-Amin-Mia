package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.export.PdfAndExportEngine
import com.example.data.geo.BengaliTextSearch
import com.example.data.geo.GeoDatasetMeta
import com.example.data.geo.MergedGeoNode
import com.example.data.geo.SundarganjGeoRepository
import com.example.data.geo.UnionGeoSummary
import com.example.data.model.ActivityLog
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.EducationalInstitution
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.model.OrgPosition
import com.example.data.model.PimsAccount
import com.example.data.model.PimsRole
import com.example.data.model.UiState
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.model.WebsiteSettings
import com.example.data.model.WorkspaceMeta
import com.example.data.repository.BootstrapOutcome
import com.example.data.repository.PimsAuthResult
import com.example.data.repository.PimsRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardStatistics(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val inactiveUsers: Int = 0,
    val totalAdmins: Int = 0,
    val districtCounts: List<Pair<String, Int>> = emptyList(),
    val upazilaCounts: List<Pair<String, Int>> = emptyList(),
    val bloodGroupCounts: List<Pair<String, Int>> = emptyList(),
    val institutionCounts: List<Pair<String, Int>> = emptyList(),
    val orgPositionCounts: List<Pair<String, Int>> = emptyList(),
    val recentProfiles: List<PimsAccount> = emptyList()
)

enum class UserSortOrder { NEWEST, NAME_ASC, PERSONAL_ID }

data class SearchFilterState(
    val query: String = "",
    val categoryFilter: String = "",
    val bloodGroup: String = "",
    val divisionId: String = "",
    val districtId: String = "",
    val upazilaId: String = "",
    val unionId: String = "",
    val wardId: String = "",
    val villageQuery: String = "",
    val institutionId: String = "",
    val orgDesignation: String = "",
    val roleFilter: String = "",
    val activeOnly: Boolean? = null,
    val sortOrder: UserSortOrder = UserSortOrder.NEWEST,
    val pageIndex: Int = 0,
    val pageSize: Int = 10
) {
    fun activeAdvancedFilterCount(): Int = listOf(
        divisionId.isNotBlank(),
        districtId.isNotBlank(),
        upazilaId.isNotBlank(),
        unionId.isNotBlank(),
        wardId.isNotBlank(),
        villageQuery.isNotBlank(),
        bloodGroup.isNotBlank(),
        institutionId.isNotBlank(),
        orgDesignation.isNotBlank(),
        roleFilter.isNotBlank(),
        activeOnly != null
    ).count { it }
}

class PimsViewModel(
    private val repository: PimsRepository,
    val currentUserId: String,
    private val geoRepository: SundarganjGeoRepository? = null
) : ViewModel() {

    private val _bootstrapDone = MutableStateFlow(false)
    val bootstrapDone: StateFlow<Boolean> = _bootstrapDone.asStateFlow()

    private val _activePimsAccountId = MutableStateFlow<String?>(null)

    private val _statusBanner = MutableStateFlow<Pair<Boolean, String>?>(null) // isError, message
    val statusBanner: StateFlow<Pair<Boolean, String>?> = _statusBanner.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilterState())
    val searchFilter: StateFlow<SearchFilterState> = _searchFilter.asStateFlow()

    private val _diagnostics = MutableStateFlow(AuthPermissionDiagnostics())
    val diagnostics: StateFlow<AuthPermissionDiagnostics> = _diagnostics.asStateFlow()

    private val _retryTrigger = MutableStateFlow(0)

    private var lastBootstrapOutcome: BootstrapOutcome? = null
    private var lastStreamError: Triple<String, String, String>? = null

    init {
        viewModelScope.launch {
            runBootstrapAndDiagnostics(refreshAuth = false)
            _bootstrapDone.value = true
        }
    }

    private suspend fun runBootstrapAndDiagnostics(refreshAuth: Boolean) {
        if (refreshAuth) {
            lastStreamError = null
        }
        val outcome = repository.ensureWorkspaceBootstrapped()
        lastBootstrapOutcome = outcome
        if (!outcome.succeeded) {
            _statusBanner.value = true to outcome.reasonBn
        }
        _diagnostics.value = repository.runReadOnlyAuthPermissionDiagnostics(
            lastBootstrap = lastBootstrapOutcome,
            lastStreamError = lastStreamError,
            refreshAuthUser = refreshAuth
        )
    }

    fun retryBootstrapAndRefresh() {
        viewModelScope.launch {
            _bootstrapDone.value = false
            _diagnostics.value = _diagnostics.value.copy(isLoading = true)
            runBootstrapAndDiagnostics(refreshAuth = true)
            _retryTrigger.value += 1
            _bootstrapDone.value = true
        }
    }

    fun refreshReadOnlyDiagnostics() {
        viewModelScope.launch {
            _diagnostics.value = _diagnostics.value.copy(isLoading = true)
            _diagnostics.value = repository.runReadOnlyAuthPermissionDiagnostics(
                lastBootstrap = lastBootstrapOutcome,
                lastStreamError = lastStreamError,
                refreshAuthUser = true
            )
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val workspaceMeta: StateFlow<WorkspaceMeta?> = _retryTrigger
        .flatMapLatest { repository.observeWorkspaceMeta(currentUserId) }
        .catch { e -> handleStreamException("workspaceMeta", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val websiteSettings: StateFlow<WebsiteSettings> = _retryTrigger
        .flatMapLatest { repository.observeWebsiteSettings(currentUserId) }
        .catch { e -> handleStreamException("websiteSettings", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), WebsiteSettings())

    @OptIn(ExperimentalCoroutinesApi::class)
    val accountsState: StateFlow<UiState<List<PimsAccount>>> = _retryTrigger
        .flatMapLatest {
            repository.observeAccounts(currentUserId)
                .map<List<PimsAccount>, UiState<List<PimsAccount>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence<Throwable>(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val message = if (firestoreError != null) {
                        recordStreamFirestoreError("pims_accounts", firestoreError)
                        repository.explainFirestoreError(firestoreError.code, "users/$currentUserId/pims_accounts").first
                    } else {
                        (error.message ?: "অ্যাকাউন্ট তালিকা লোড করা যায়নি।")
                    }
                    emit(UiState.Error(message))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val firestoreLocations: StateFlow<List<LocationNode>> = _retryTrigger
        .flatMapLatest { repository.observeLocations(currentUserId) }
        .catch { e -> handleStreamException("locations", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val geoDatasetMeta: GeoDatasetMeta
        get() = geoRepository?.meta ?: GeoDatasetMeta()

    val mergedGeoNodes: StateFlow<List<MergedGeoNode>> = firestoreLocations
        .map { fsList ->
            if (geoRepository != null) geoRepository.mergeWithFirestore(fsList)
            else fsList.map {
                MergedGeoNode(
                    node = it,
                    isOfficialReference = false,
                    isAdminOverridden = true,
                    reference = null,
                    unionId = "",
                    effectiveWardIds = emptyList(),
                    effectiveStatus = com.example.data.geo.GeoVerificationStatus.VERIFIED
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            geoRepository?.mergeWithFirestore(emptyList()).orEmpty()
        )

    val locations: StateFlow<List<LocationNode>> = mergedGeoNodes
        .map { list -> list.map { it.node } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            geoRepository?.seedAndReferenceLocations.orEmpty()
        )

    val unionGeoSummaries: StateFlow<List<UnionGeoSummary>> = mergedGeoNodes
        .map { list -> geoRepository?.computeUnionSummaries(list).orEmpty() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            geoRepository?.let { it.computeUnionSummaries(it.mergeWithFirestore(emptyList())) }.orEmpty()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val institutions: StateFlow<List<EducationalInstitution>> = _retryTrigger
        .flatMapLatest { repository.observeInstitutions(currentUserId) }
        .catch { e -> handleStreamException("institutions", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val orgPositions: StateFlow<List<OrgPosition>> = _retryTrigger
        .flatMapLatest { repository.observeOrgPositions(currentUserId) }
        .catch { e -> handleStreamException("orgPositions", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activityLogs: StateFlow<List<ActivityLog>> = _retryTrigger
        .flatMapLatest { repository.observeActivityLogs(currentUserId) }
        .catch { e -> handleStreamException("activityLogs", e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activePimsAccount: StateFlow<PimsAccount?> =
        kotlinx.coroutines.flow.combine(accountsState, _activePimsAccountId) { state, activeId ->
            val list = (state as? UiState.Success)?.data ?: emptyList()
            if (activeId == null) null else list.firstOrNull { it.id == activeId && it.active }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val vaultEntries: StateFlow<List<VaultEntry>> = activePimsAccount
        .flatMapLatest { acc ->
            if (acc == null) flowOf(emptyList())
            else repository.observeVaultEntries(currentUserId, acc.personalId)
                .catch { e -> handleStreamException("vaultEntries", e) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dashboardStats: StateFlow<DashboardStatistics> = accountsState
        .map { state ->
            val accounts = (state as? UiState.Success)?.data ?: emptyList()
            computeStats(accounts)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardStatistics())

    private fun computeStats(accounts: List<PimsAccount>): DashboardStatistics {
        val active = accounts.count { it.active }
        val inactive = accounts.size - active
        val admins = accounts.count { it.roleEnum().isAdminTier() }

        fun topCounts(selector: (PimsAccount) -> String): List<Pair<String, Int>> =
            accounts.mapNotNull { selector(it).trim().takeIf(String::isNotBlank) }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(8)
                .map { it.key to it.value }

        return DashboardStatistics(
            totalUsers = accounts.size,
            activeUsers = active,
            inactiveUsers = inactive,
            totalAdmins = admins,
            districtCounts = topCounts { it.districtName },
            upazilaCounts = topCounts { it.upazilaName },
            bloodGroupCounts = topCounts { it.bloodGroup },
            institutionCounts = topCounts { it.institutionName },
            orgPositionCounts = topCounts { it.orgDesignation },
            recentProfiles = accounts.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }.take(5)
        )
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun reportStatus(isError: Boolean, message: String) {
        _statusBanner.value = isError to message
    }

    fun clearAuthMessage() {
        _authMessage.value = null
    }

    fun updateSearchFilter(transform: (SearchFilterState) -> SearchFilterState) {
        _searchFilter.value = transform(_searchFilter.value)
    }

    fun filterVisibleAccounts(
        accounts: List<PimsAccount>,
        viewer: PimsAccount,
        filterOverride: SearchFilterState? = null
    ): List<PimsAccount> {
        val role = viewer.roleEnum()
        val base = accounts.filter { role.canViewAccount(it, viewer.personalId, viewer.assignedRecordIds) }
        val f = filterOverride ?: _searchFilter.value
        val q = f.query.trim().lowercase()
        val vq = f.villageQuery.trim().lowercase()
        val cat = f.categoryFilter.trim()

        val filtered = base.filter { acc ->
            val matchesQuery = q.isBlank() || listOf(
                acc.fullNameBn,
                acc.fullNameEn,
                acc.personalId,
                acc.fatherName,
                acc.motherName,
                acc.mobileNumber,
                acc.emailAddress,
                acc.role,
                AppStrings.roleLabel(AppLang.BN, acc.role),
                AppStrings.roleLabel(AppLang.EN, acc.role),
                acc.institutionName,
                acc.institutionType,
                if (acc.institutionType.isNotBlank()) AppStrings.institutionTypeLabel(AppLang.BN, acc.institutionType) else "",
                if (acc.institutionType.isNotBlank()) AppStrings.institutionTypeLabel(AppLang.EN, acc.institutionType) else "",
                acc.orgDesignation,
                acc.orgLevel,
                acc.bloodGroup,
                acc.districtName,
                acc.upazilaName,
                acc.unionName,
                acc.wardName,
                acc.villageName,
                acc.departmentOrSubject,
                acc.skills
            ).any { BengaliTextSearch.matches(q, it) }

            val matchesCategory = when {
                cat.isBlank() -> true
                cat.startsWith("ROLE:") -> acc.role == cat.removePrefix("ROLE:")
                cat.startsWith("INST:") -> acc.institutionType == cat.removePrefix("INST:")
                cat == "STATUS:ACTIVE" -> acc.active
                cat == "STATUS:INACTIVE" -> !acc.active
                cat.startsWith("BLOOD:") -> acc.bloodGroup == cat.removePrefix("BLOOD:")
                else -> acc.role == cat || acc.institutionType == cat || acc.orgDesignation == cat
            }

            val matchesBlood = f.bloodGroup.isBlank() || acc.bloodGroup == f.bloodGroup
            val matchesDiv = f.divisionId.isBlank() || acc.divisionId == f.divisionId
            val matchesDist = f.districtId.isBlank() || acc.districtId == f.districtId
            val matchesUpz = f.upazilaId.isBlank() || acc.upazilaId == f.upazilaId
            val matchesUnion = f.unionId.isBlank() || acc.unionId == f.unionId
            val matchesWard = f.wardId.isBlank() || acc.wardId == f.wardId
            val matchesVillage = vq.isBlank() || BengaliTextSearch.matches(vq, acc.villageName)
            val matchesInst = f.institutionId.isBlank() || acc.institutionId == f.institutionId
            val matchesOrg = f.orgDesignation.isBlank() || acc.orgDesignation == f.orgDesignation
            val matchesRole = f.roleFilter.isBlank() || acc.role == f.roleFilter
            val matchesActive = f.activeOnly == null || acc.active == f.activeOnly

            matchesQuery && matchesCategory && matchesBlood && matchesDiv && matchesDist && matchesUpz &&
                matchesUnion && matchesWard && matchesVillage && matchesInst &&
                matchesOrg && matchesRole && matchesActive
        }

        return when (f.sortOrder) {
            UserSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            UserSortOrder.NAME_ASC -> filtered.sortedBy { it.fullNameBn }
            UserSortOrder.PERSONAL_ID -> filtered.sortedBy { it.personalId }
        }
    }

    fun initializeSuperAdmin(
        fullNameBn: String,
        fullNameEn: String,
        customPersonalId: String?,
        password: String,
        mobile: String,
        email: String
    ) {
        viewModelScope.launch {
            _authBusy.value = true
            _authMessage.value = null
            val res = repository.initializeSuperAdmin(fullNameBn, fullNameEn, customPersonalId, password, mobile, email)
            _authBusy.value = false
            res.onSuccess { created ->
                _activePimsAccountId.value = created.id
                _statusBanner.value = false to "সুপার অ্যাডমিন অ্যাকাউন্ট তৈরি ও লগইন সম্পন্ন হয়েছে (${created.personalId})"
            }.onFailure { err ->
                _authMessage.value = err.message ?: "Initialization failed"
            }
        }
    }

    fun loginWithPersonalId(personalId: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            _authMessage.value = null
            when (val res = repository.authenticatePimsUser(personalId, password)) {
                is PimsAuthResult.Success -> {
                    _activePimsAccountId.value = res.account.id
                }
                is PimsAuthResult.InvalidCredentials -> {
                    _authMessage.value = "ভুল পার্সোনাল আইডি বা পাসওয়ার্ড (বাকি চেষ্টা: ${res.remainingAttempts})"
                }
                is PimsAuthResult.RateLimited -> {
                    _authMessage.value = "অতিরিক্ত ব্যর্থ প্রচেষ্টা! ${res.remainingSeconds} সেকেন্ড পর আবার চেষ্টা করুন।"
                }
                PimsAuthResult.AccountInactive -> {
                    _authMessage.value = "এই অ্যাকাউন্টটি নিষ্ক্রিয় করা হয়েছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"
                }
                is PimsAuthResult.Error -> {
                    _authMessage.value = res.message
                }
            }
            _authBusy.value = false
        }
    }

    fun logoutPimsSession() {
        _activePimsAccountId.value = null
        _authMessage.value = null
        _searchFilter.value = SearchFilterState()
    }

    fun changeOwnPassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = repository.changeOwnPassword(actor, currentPassword, newPassword)
            res.onSuccess {
                _statusBanner.value = false to "পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে"
                onSuccess()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "পাসওয়ার্ড পরিবর্তনে সমস্যা হয়েছে")
            }
        }
    }

    suspend fun generateNextPersonalId(prefix: String): Result<String> =
        repository.generateNextPersonalId(prefix)

    fun saveUserProfile(
        isNew: Boolean,
        draft: PimsAccount,
        initialPassword: String,
        onComplete: () -> Unit
    ) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = if (isNew) {
                repository.createPimsUser(actor, draft, initialPassword).map { Unit }
            } else {
                repository.updatePimsProfile(actor, draft)
            }
            res.onSuccess {
                _statusBanner.value = false to if (isNew) "নতুন ব্যবহারকারী সফলভাবে তৈরি হয়েছে" else "প্রোফাইল সফলভাবে হালনাগাদ হয়েছে"
                onComplete()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "সংরক্ষণ ব্যর্থ হয়েছে")
            }
        }
    }

    fun toggleUserActive(target: PimsAccount) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = repository.toggleAccountActive(actor, target, !target.active)
            res.onSuccess {
                _statusBanner.value = false to if (!target.active) "অ্যাকাউন্ট সক্রিয় করা হয়েছে" else "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে"
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "অবস্থা পরিবর্তন ব্যর্থ")
            }
        }
    }

    fun resetUserPassword(target: PimsAccount, newInitialPassword: String, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = repository.adminResetPassword(actor, target, newInitialPassword)
            res.onSuccess {
                _statusBanner.value = false to "পাসওয়ার্ড রিসেট সম্পন্ন হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "রিসেট ব্যর্থ")
            }
        }
    }

    fun deleteUser(target: PimsAccount, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = repository.deletePimsAccount(actor, target)
            res.onSuccess {
                _statusBanner.value = false to "প্রোফাইল মুছে ফেলা হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "মুছে ফেলা সম্ভব হয়নি")
            }
        }
    }

    fun saveWebsiteSettings(settings: WebsiteSettings) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = repository.saveWebsiteSettings(actor, settings)
            res.onSuccess {
                _statusBanner.value = false to "ওয়েবসাইট ও ব্র্যান্ডিং সেটিংস সংরক্ষিত হয়েছে"
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "সেটিংস সংরক্ষণ ব্যর্থ")
            }
        }
    }

    fun saveLocation(node: LocationNode, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            val res = repository.saveLocation(actor, node)
            res.onSuccess {
                _statusBanner.value = false to "লোকেশন সংরক্ষিত হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "লোকেশন সংরক্ষণ ব্যর্থ")
            }
        }
    }

    fun assignVillageToWard(
        villageNode: LocationNode,
        targetWardId: String,
        targetWardNumberCode: String,
        onDone: () -> Unit = {}
    ) {
        val actor = activePimsAccount.value ?: return
        if (!actor.roleEnum().canManageMasterDatasets()) {
            _statusBanner.value = true to "শুধুমাত্র অনুমোদিত অ্যাডমিন ওয়ার্ড ম্যাপিং যাচাই/সংরক্ষণ করতে পারবেন।"
            return
        }
        val updated = villageNode.copy(
            parentId = targetWardId,
            code = if (targetWardNumberCode.isNotBlank()) "W-${targetWardNumberCode.toIntOrNull() ?: targetWardNumberCode}" else villageNode.code
        )
        viewModelScope.launch {
            repository.saveLocation(actor, updated).onSuccess {
                _statusBanner.value = false to "গ্রামটি ওয়ার্ডে যাচাইকৃত হিসেবে সংযুক্ত হয়েছে (${villageNode.nameBn})"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "ওয়ার্ড সংযুক্তি ব্যর্থ")
            }
        }
    }

    fun exportGeographyDataset(context: Context, asCsv: Boolean, lang: AppLang) {
        val nodes = mergedGeoNodes.value
        val result = if (asCsv) {
            PdfAndExportEngine.exportGeographyCsv(context, nodes)
        } else {
            PdfAndExportEngine.exportGeographyJson(context, geoDatasetMeta, nodes)
        }
        result.onSuccess { file ->
            val mime = if (asCsv) "text/csv" else "application/json"
            val ok = PdfAndExportEngine.shareExportFile(
                context,
                file,
                mime,
                AppStrings.pick(lang, "সুন্দরগঞ্জ ভূগোল ডেটা রপ্তানি", "Export Sundarganj Geo Dataset")
            )
            _statusBanner.value = if (ok) {
                false to AppStrings.pick(lang, "ডেটাবেস রপ্তানি সম্পন্ন হয়েছে (${file.name})", "Geo export ready (${file.name})")
            } else {
                false to AppStrings.pick(lang, "ফাইল সংরক্ষিত: ${file.name}", "Exported file saved: ${file.name}")
            }
        }.onFailure { err ->
            _statusBanner.value = true to (err.message ?: AppStrings.pick(lang, "রপ্তানি ব্যর্থ", "Export failed"))
        }
    }

    fun deleteLocation(locationId: String) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.deleteLocation(actor, locationId).onSuccess {
                _statusBanner.value = false to "লোকেশন মুছে ফেলা হয়েছে"
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "লোকেশন মুছে ফেলা ব্যর্থ")
            }
        }
    }

    fun importLocationsCsv(csv: String, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.importLocationsCsv(actor, csv, locations.value).onSuccess { count ->
                _statusBanner.value = false to "$count টি লোকেশন CSV থেকে যোগ করা হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "CSV আমদানি ব্যর্থ")
            }
        }
    }

    fun saveInstitution(inst: EducationalInstitution, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.saveInstitution(actor, inst, institutions.value).onSuccess {
                _statusBanner.value = false to "শিক্ষা প্রতিষ্ঠান সংরক্ষিত হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "প্রতিষ্ঠান সংরক্ষণ ব্যর্থ")
            }
        }
    }

    fun deleteInstitution(institutionId: String) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.deleteInstitution(actor, institutionId).onSuccess {
                _statusBanner.value = false to "শিক্ষা প্রতিষ্ঠান মুছে ফেলা হয়েছে"
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "মুছে ফেলা ব্যর্থ")
            }
        }
    }

    fun importInstitutionsCsv(csv: String, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.importInstitutionsCsv(actor, csv, locations.value, institutions.value).onSuccess { count ->
                _statusBanner.value = false to "$count টি শিক্ষা প্রতিষ্ঠান CSV থেকে যোগ করা হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "CSV আমদানি ব্যর্থ")
            }
        }
    }

    fun saveOrgPosition(position: OrgPosition, onDone: () -> Unit = {}) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.saveOrgPosition(actor, position).onSuccess {
                _statusBanner.value = false to "সাংগঠনিক পদবী সংরক্ষিত হয়েছে"
                onDone()
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "পদবী সংরক্ষণ ব্যর্থ")
            }
        }
    }

    fun deleteOrgPosition(positionId: String) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.deleteOrgPosition(actor, positionId).onSuccess {
                _statusBanner.value = false to "সাংগঠনিক পদবী মুছে ফেলা হয়েছে"
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "মুছে ফেলা ব্যর্থ")
            }
        }
    }

    fun saveVaultEntry(
        category: VaultCategory,
        title: String,
        secret: String,
        passphrase: String,
        onDone: () -> Unit = {}
    ) {
        val actor = activePimsAccount.value ?: return
        viewModelScope.launch {
            repository.saveEncryptedVaultEntry(actor.personalId, category, title, secret, passphrase)
                .onSuccess {
                    _statusBanner.value = false to "গোপন তথ্য এনক্রিপ্ট করে সংরক্ষিত হয়েছে"
                    onDone()
                }.onFailure { err ->
                    _statusBanner.value = true to (err.message ?: "ভল্টে সংরক্ষণ ব্যর্থ")
                }
        }
    }

    fun deleteVaultEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteVaultEntry(entryId).onSuccess {
                _statusBanner.value = false to "ভল্ট এন্ট্রি মুছে ফেলা হয়েছে"
            }.onFailure { err ->
                _statusBanner.value = true to (err.message ?: "মুছে ফেলা ব্যর্থ")
            }
        }
    }

    fun getChildLocations(level: LocationLevel, parentId: String): List<LocationNode> =
        locations.value
            .filter { it.level == level.name && (level == LocationLevel.DIVISION || it.parentId == parentId) }
            .sortedBy { it.nameBn }

    private fun recordStreamFirestoreError(pathTag: String, firestoreError: FirebaseFirestoreException) {
        val (bn, en) = repository.explainFirestoreError(firestoreError.code, pathTag)
        lastStreamError = Triple(firestoreError.code.name, bn, en)
        _diagnostics.value = _diagnostics.value.copy(
            lastFirestoreErrorCode = firestoreError.code.name,
            lastFirestoreErrorReasonBn = bn,
            lastFirestoreErrorReasonEn = en
        )
    }

    private fun handleStreamException(tag: String, error: Throwable) {
        val firestoreError = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (error is CancellationException && firestoreError == null) throw error
        if (firestoreError != null) {
            recordStreamFirestoreError(tag, firestoreError)
        }
        Log.w("PimsViewModel", "Stream error in $tag", error)
    }
}
