package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.security.CryptoSecurity
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ActivityLog
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.DiagnosticCheckStatus
import com.example.data.model.EducationalInstitution
import com.example.data.model.InstitutionType
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.model.OrgPosition
import com.example.data.model.PimsAccount
import com.example.data.model.PimsRole
import com.example.data.model.VaultCategory
import com.example.data.model.VaultEntry
import com.example.data.model.WebsiteSettings
import com.example.data.model.WorkspaceMeta
import com.example.data.seed.BangladeshGeoSeed
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

sealed interface PimsAuthResult {
    data class Success(val account: PimsAccount) : PimsAuthResult
    data class InvalidCredentials(val remainingAttempts: Int) : PimsAuthResult
    data class RateLimited(val remainingSeconds: Long) : PimsAuthResult
    data object AccountInactive : PimsAuthResult
    data class Error(val message: String) : PimsAuthResult
}

data class BootstrapOutcome(
    val succeeded: Boolean,
    val errorCode: String = "",
    val reasonBn: String = "",
    val reasonEn: String = "",
    val failedPath: String = ""
)

class PimsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google Sign-In required before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeWorkspaceMeta(uid: String): Flow<WorkspaceMeta?> = flow {
        val path = "users/$uid"
        emitAll(
            userDoc(uid).snapshots()
                .map { it.toObject(WorkspaceMeta::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeWebsiteSettings(uid: String): Flow<WebsiteSettings> = flow {
        val path = "users/$uid/settings/website"
        emitAll(
            userDoc(uid).collection("settings").document("website").snapshots()
                .map { snapshot ->
                    snapshot.toObject(WebsiteSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: WebsiteSettings()
                }
                .catch { error -> handleStreamError(error, OperationType.GET, path, uid) }
        )
    }

    fun observeAccounts(uid: String): Flow<List<PimsAccount>> = flow {
        val path = "users/$uid/pims_accounts"
        emitAll(
            userDoc(uid).collection("pims_accounts").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PimsAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeLocations(uid: String): Flow<List<LocationNode>> = flow {
        val path = "users/$uid/locations"
        emitAll(
            userDoc(uid).collection("locations").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LocationNode::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeInstitutions(uid: String): Flow<List<EducationalInstitution>> = flow {
        val path = "users/$uid/institutions"
        emitAll(
            userDoc(uid).collection("institutions").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EducationalInstitution::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeOrgPositions(uid: String): Flow<List<OrgPosition>> = flow {
        val path = "users/$uid/org_positions"
        emitAll(
            userDoc(uid).collection("org_positions").snapshots()
                .map { snapshot ->
                    snapshot.toObjects(OrgPosition::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.rankOrder }
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeVaultEntries(uid: String, ownerPersonalId: String): Flow<List<VaultEntry>> = flow {
        val path = "users/$uid/vault_entries"
        emitAll(
            userDoc(uid).collection("vault_entries")
                .whereEqualTo("ownerPersonalId", ownerPersonalId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(VaultEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    fun observeActivityLogs(uid: String): Flow<List<ActivityLog>> = flow {
        val path = "users/$uid/activity_logs"
        emitAll(
            userDoc(uid).collection("activity_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ActivityLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error -> handleStreamError(error, OperationType.LIST, path, uid) }
        )
    }

    private suspend fun handleStreamError(error: Throwable, op: OperationType, path: String, uid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }

    /**
     * Ensures root workspace metadata, default website settings, verified Bangladesh 8 Divisions +
     * 64 Districts, and default organizational positions exist without fabricating fake user records.
     * Uses read-only admin checks before attempting admin-only writes, seeds missing IDs idempotently,
     * and transparently reports PERMISSION_DENIED or Firestore failures via BootstrapOutcome.
     */
    suspend fun ensureWorkspaceBootstrapped(): BootstrapOutcome {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        if (uid.isNullOrBlank()) {
            return BootstrapOutcome(
                succeeded = false,
                errorCode = "UNAUTHENTICATED",
                reasonBn = "Firebase Authentication-এ ব্যবহারকারী লগইন করা নেই।",
                reasonEn = "User is not signed in to Firebase Authentication.",
                failedPath = "auth"
            )
        }
        var currentPath = "users/$uid"
        return try {
            val rootSnap = userDoc(uid).get().await()
            if (!rootSnap.exists()) {
                userDoc(uid).set(
                    mapOf(
                        "userId" to uid,
                        "email" to (currentUser.email ?: "").take(200),
                        "displayName" to (currentUser.displayName ?: "").take(120),
                        "workspaceInitialized" to false,
                        "nextIdSequence" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Read-only admin privilege verification matching firestore.rules isAdmin()
            currentPath = "admins/$uid"
            val emailMatchesBootstrapped = currentUser.email == BOOTSTRAPPED_ADMIN_EMAIL
            val isBootstrappedSuperAdmin = emailMatchesBootstrapped && currentUser.isEmailVerified
            val hasActiveAdminDoc = if (isBootstrappedSuperAdmin) {
                true
            } else {
                try {
                    val adminSnap = db.collection("admins").document(uid).get().await()
                    adminSnap.exists() &&
                        adminSnap.getBoolean("active") == true &&
                        adminSnap.getString("role") in setOf("admin", "ADMIN", "SUPER_ADMIN")
                } catch (_: Exception) {
                    false
                }
            }
            val canWriteAdminMasterData = isBootstrappedSuperAdmin || hasActiveAdminDoc

            currentPath = "users/$uid/settings/website"
            val settingsRef = userDoc(uid).collection("settings").document("website")
            val settingsExists = settingsRef.get().await().exists()
            if (!settingsExists) {
                if (!canWriteAdminMasterData) {
                    val (bn, en) = explainFirestoreError(
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                        currentPath,
                        currentUser
                    )
                    return BootstrapOutcome(
                        succeeded = false,
                        errorCode = "PERMISSION_DENIED",
                        reasonBn = bn,
                        reasonEn = en,
                        failedPath = currentPath
                    )
                }
                settingsRef.set(WebsiteSettings().toCreateMap()).await()
            }

            // Idempotently check & seed verified 8 Divisions + 64 Districts (never duplicates existing IDs)
            currentPath = "users/$uid/locations"
            val locSnap = userDoc(uid).collection("locations").get().await()
            val existingLocIds = locSnap.documents.mapTo(HashSet()) { it.id }
            val missingGeoSeeds = BangladeshGeoSeed.divisionsAndDistricts.filter { it.id !in existingLocIds }
            if (missingGeoSeeds.isNotEmpty()) {
                if (!canWriteAdminMasterData) {
                    if (existingLocIds.isEmpty()) {
                        val (bn, en) = explainFirestoreError(
                            FirebaseFirestoreException.Code.PERMISSION_DENIED,
                            currentPath,
                            currentUser
                        )
                        return BootstrapOutcome(
                            succeeded = false,
                            errorCode = "PERMISSION_DENIED",
                            reasonBn = bn,
                            reasonEn = en,
                            failedPath = currentPath
                        )
                    }
                } else {
                    missingGeoSeeds.chunked(350).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { seed ->
                            val ref = userDoc(uid).collection("locations").document(seed.id)
                            batch.set(
                                ref,
                                LocationNode(
                                    level = seed.level.name,
                                    parentId = seed.parentId,
                                    nameBn = seed.nameBn,
                                    nameEn = seed.nameEn,
                                    code = seed.code
                                ).toCreateMap()
                            )
                        }
                        batch.commit().await()
                    }
                }
            }

            // Idempotently check & seed organizational position hierarchy
            currentPath = "users/$uid/org_positions"
            val posSnap = userDoc(uid).collection("org_positions").get().await()
            val existingPosIds = posSnap.documents.mapTo(HashSet()) { it.id }
            val missingPositions = BangladeshGeoSeed.defaultOrgPositions.filter { it.id !in existingPosIds }
            if (missingPositions.isNotEmpty() && canWriteAdminMasterData) {
                val batch = db.batch()
                missingPositions.forEach { pos ->
                    val ref = userDoc(uid).collection("org_positions").document(pos.id)
                    batch.set(
                        ref,
                        OrgPosition(
                            levelNameBn = pos.levelNameBn,
                            levelNameEn = pos.levelNameEn,
                            designationBn = pos.designationBn,
                            designationEn = pos.designationEn,
                            rankOrder = pos.rankOrder
                        ).toCreateMap()
                    )
                }
                batch.commit().await()
            }

            BootstrapOutcome(
                succeeded = true,
                reasonBn = "ওয়ার্কস্পেস, ডিফল্ট সেটিংস এবং মাস্টার ডেটা সফলভাবে যাচাই ও লোড হয়েছে।",
                reasonEn = "Workspace metadata, settings, and master datasets verified successfully."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            val (reasonBn, reasonEn) = explainFirestoreError(e.code, currentPath, currentUser)
            BootstrapOutcome(
                succeeded = false,
                errorCode = e.code.name,
                reasonBn = reasonBn,
                reasonEn = reasonEn,
                failedPath = currentPath
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            BootstrapOutcome(
                succeeded = false,
                errorCode = e::class.java.simpleName.uppercase(),
                reasonBn = "ওয়ার্কস্পেস সিডিং ব্যর্থ ($currentPath): ${e.localizedMessage ?: "অজানা ত্রুটি"}",
                reasonEn = "Workspace bootstrap failed at $currentPath: ${e.localizedMessage ?: "Unknown error"}",
                failedPath = currentPath
            )
        }
    }

    /**
     * Strictly READ-ONLY runtime diagnostic check using live Firebase Auth + Firestore `.get()`
     * operations. Never creates, updates, or deletes any document and never exposes tokens/secrets.
     */
    suspend fun runReadOnlyAuthPermissionDiagnostics(
        lastBootstrap: BootstrapOutcome?,
        lastStreamError: Triple<String, String, String>?,
        refreshAuthUser: Boolean = false
    ): AuthPermissionDiagnostics {
        val existingUser = auth.currentUser
        if (refreshAuthUser && existingUser != null) {
            withTimeoutOrNull(3000L) {
                try {
                    existingUser.reload().await()
                } catch (_: Exception) {
                    // Non-fatal if offline; keep cached FirebaseAuth claims
                }
            }
        }
        val user = auth.currentUser
        if (user == null || user.uid.isBlank()) {
            return AuthPermissionDiagnostics(
                isLoading = false,
                isFirebaseSignedIn = false,
                bootstrapStatus = DiagnosticCheckStatus.FAILED,
                bootstrapDetailBn = lastBootstrap?.reasonBn ?: "Firebase Authentication সক্রিয় নয়।",
                bootstrapDetailEn = lastBootstrap?.reasonEn ?: "Not signed in to Firebase Authentication.",
                lastFirestoreErrorCode = "UNAUTHENTICATED",
                lastFirestoreErrorReasonBn = "ব্যবহারকারী Google দিয়ে Firebase-এ সাইন-ইন করেননি।",
                lastFirestoreErrorReasonEn = "User is not signed in with Google in Firebase Auth.",
                checkedAtEpochMs = System.currentTimeMillis()
            )
        }

        val uid = user.uid
        val email = user.email.orEmpty()
        val verified = user.isEmailVerified
        val providers = user.providerData
            .map { it.providerId }
            .filter { it != "firebase" && it.isNotBlank() }
            .distinct()
        val emailMatchesBootstrapped = email.isNotBlank() && email == BOOTSTRAPPED_ADMIN_EMAIL

        var adminsDocExists: Boolean? = null
        var adminsDocActive: Boolean? = null
        var adminsDocRole = ""
        var adminReadErrorCode = ""
        var adminReadReasonBn = ""
        var adminReadReasonEn = ""

        try {
            val adminSnap = db.collection("admins").document(uid).get().await()
            adminsDocExists = adminSnap.exists()
            if (adminSnap.exists()) {
                adminsDocActive = adminSnap.getBoolean("active")
                adminsDocRole = adminSnap.getString("role").orEmpty()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.GET, "admins/$uid")
            adminReadErrorCode = e.code.name
            val (bn, en) = explainFirestoreError(e.code, "admins/$uid", user)
            adminReadReasonBn = bn
            adminReadReasonEn = en
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "admins/$uid")
            adminReadErrorCode = e::class.java.simpleName.uppercase()
            adminReadReasonBn = e.localizedMessage ?: "/admins/$uid পড়া সম্ভব হয়নি।"
            adminReadReasonEn = e.localizedMessage ?: "Failed to read /admins/$uid."
        }

        val hasActiveAdminRecord = adminsDocExists == true &&
            adminsDocActive == true &&
            adminsDocRole in setOf("admin", "ADMIN", "SUPER_ADMIN")
        val isBootstrappedSuperAdmin = emailMatchesBootstrapped && verified
        val adminPermissionGranted = isBootstrappedSuperAdmin || hasActiveAdminRecord

        val (roleEn, roleBn) = when {
            isBootstrappedSuperAdmin -> "Super Admin" to "সুপার অ্যাডমিন (Super Admin)"
            hasActiveAdminRecord && adminsDocRole == "SUPER_ADMIN" -> "Super Admin" to "সুপার অ্যাডমিন (Super Admin)"
            hasActiveAdminRecord -> "Admin" to "অ্যাডমিন (Admin)"
            else -> "Normal User" to "সাধারণ ব্যবহারকারী (Normal User)"
        }

        val bootstrapOutcome = lastBootstrap
        val bootstrapStatus = when {
            bootstrapOutcome == null -> DiagnosticCheckStatus.CHECKING
            bootstrapOutcome.succeeded -> DiagnosticCheckStatus.SUCCESS
            else -> DiagnosticCheckStatus.FAILED
        }

        val errorCode: String
        val errorReasonBn: String
        val errorReasonEn: String
        when {
            lastStreamError != null -> {
                errorCode = lastStreamError.first
                errorReasonBn = lastStreamError.second
                errorReasonEn = lastStreamError.third
            }
            bootstrapOutcome != null && !bootstrapOutcome.succeeded && bootstrapOutcome.errorCode.isNotBlank() -> {
                errorCode = bootstrapOutcome.errorCode
                errorReasonBn = bootstrapOutcome.reasonBn
                errorReasonEn = bootstrapOutcome.reasonEn
            }
            adminReadErrorCode.isNotBlank() -> {
                errorCode = adminReadErrorCode
                errorReasonBn = adminReadReasonBn
                errorReasonEn = adminReadReasonEn
            }
            !adminPermissionGranted && emailMatchesBootstrapped && !verified -> {
                errorCode = "EMAIL_NOT_VERIFIED"
                errorReasonBn = "ইমেইলটি মিললেও email_verified=false হওয়ায় Firestore rules অ্যাডমিন অনুমতি দিচ্ছে না।"
                errorReasonEn = "Email matches bootstrapped admin, but email_verified is false so Firestore rules deny admin access."
            }
            !adminPermissionGranted -> {
                errorCode = "NO_ERROR (ADMIN_NOT_GRANTED)"
                errorReasonBn = "কোনো সিস্টেম ত্রুটি নেই, তবে বর্তমান অ্যাকাউন্টটি অ্যাডমিন তালিকায় (/admins/$uid) না থাকায় অ্যাডমিন রাইট অনুমতি নেই।"
                errorReasonEn = "No transport failure, but account lacks a verified bootstrapped email or active /admins/$uid record."
            }
            else -> {
                errorCode = "NONE"
                errorReasonBn = "কোনো Firestore ত্রুটি পাওয়া যায়নি।"
                errorReasonEn = "No Firestore error detected."
            }
        }

        return AuthPermissionDiagnostics(
            isLoading = false,
            googleEmail = email,
            firebaseUid = uid,
            emailVerified = verified,
            isFirebaseSignedIn = true,
            authProviders = providers,
            emailMatchesBootstrappedAdmin = emailMatchesBootstrapped,
            effectiveUserRole = roleEn,
            effectiveUserRoleBn = roleBn,
            adminPermissionGranted = adminPermissionGranted,
            adminsDocExists = adminsDocExists,
            adminsDocActive = adminsDocActive,
            adminsDocRole = adminsDocRole,
            bootstrapStatus = bootstrapStatus,
            bootstrapDetailBn = bootstrapOutcome?.reasonBn.orEmpty(),
            bootstrapDetailEn = bootstrapOutcome?.reasonEn.orEmpty(),
            lastFirestoreErrorCode = errorCode,
            lastFirestoreErrorReasonBn = errorReasonBn,
            lastFirestoreErrorReasonEn = errorReasonEn,
            checkedAtEpochMs = System.currentTimeMillis()
        )
    }

    fun explainFirestoreError(
        code: FirebaseFirestoreException.Code,
        path: String,
        user: com.google.firebase.auth.FirebaseUser? = auth.currentUser
    ): Pair<String, String> {
        return when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                val emailMatches = user?.email == BOOTSTRAPPED_ADMIN_EMAIL
                val verified = user?.isEmailVerified == true
                when {
                    user == null -> "Firebase Auth সাইন-ইন না থাকায় $path-এ অনুমতি নাকচ হয়েছে।" to
                        "Permission denied at $path because Firebase Auth session is missing."
                    emailMatches && !verified -> "$path-এ PERMISSION_DENIED: ইমেইল যাচাইকৃত (email_verified=true) নয়।" to
                        "PERMISSION_DENIED at $path: Google email_verified claim is false."
                    else -> "$path-এ PERMISSION_DENIED: Firestore Security Rules অনুযায়ী এই অ্যাকাউন্টে অ্যাডমিন রাইট অনুমতি নেই (/admins/${user.uid} অনুপস্থিত বা নিষ্ক্রিয় অথবা Bootstrapped Admin নয়)।" to
                        "PERMISSION_DENIED at $path: Account is not the verified bootstrapped admin and has no active admin grant at /admins/${user.uid}."
                }
            }
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "$path-এ UNAUTHENTICATED: নতুন করে Google Sign-In প্রয়োজন।" to
                    "UNAUTHENTICATED at $path: Fresh Google Sign-In required."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "$path-এ UNAVAILABLE: নেটওয়ার্ক বা Firestore সার্ভারের সাথে সংযোগ বিচ্ছিন্ন।" to
                    "UNAVAILABLE at $path: Network or Firestore backend unreachable."
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "$path-এ FAILED_PRECONDITION: প্রয়োজনীয় ইনডেক্স বা পূর্বশর্ত অনুপস্থিত।" to
                    "FAILED_PRECONDITION at $path: Required Firestore index or precondition missing."
            else ->
                "$path-এ ${code.name}: Firestore অপারেশন সম্পন্ন হয়নি।" to
                    "${code.name} at $path: Firestore operation could not complete."
        }
    }

    /**
     * Initializes the single Super Admin account when the workspace has 0 PIMS accounts.
     * Public self-registration is strictly disabled once an admin exists.
     */
    suspend fun initializeSuperAdmin(
        fullNameBn: String,
        fullNameEn: String,
        customPersonalId: String?,
        password: String,
        mobileNumber: String,
        emailAddress: String
    ): Result<PimsAccount> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/pims_accounts"
            val existing = userDoc(uid).collection("pims_accounts").limit(1).get().await()
            if (!existing.isEmpty) {
                return Result.failure(IllegalStateException("অ্যাডমিন অ্যাকাউন্ট ইতিমধ্যে তৈরি হয়েছে।"))
            }
            require(fullNameBn.isNotBlank()) { "Full name (Bangla) is required" }
            require(CryptoSecurity.isStrongPassword(password)) { "Password must be at least 8 characters with letters/digits" }

            val personalId = customPersonalId?.trim()?.uppercase()?.takeIf { it.length >= 3 }
                ?: generateUniquePersonalIdInternal(uid, "SA")
            val hash = CryptoSecurity.hashPassword(password)
            val docId = "acc_${UUID.randomUUID().toString().replace("-", "").take(16)}"

            val account = PimsAccount(
                id = docId,
                personalId = personalId,
                passwordHash = hash.hashBase64,
                passwordSalt = hash.saltBase64,
                role = PimsRole.SUPER_ADMIN.name,
                active = true,
                mustChangePassword = false,
                failedLoginAttempts = 0L,
                lockedUntilEpochMs = 0L,
                canEditSelfProfile = true,
                fullNameBn = fullNameBn.trim(),
                fullNameEn = fullNameEn.trim(),
                mobileNumber = mobileNumber.trim(),
                emailAddress = emailAddress.trim(),
                orgLevel = "কেন্দ্রীয়",
                orgDesignation = "সুপার অ্যাডমিন",
                orgStatus = "সক্রিয়"
            )

            userDoc(uid).collection("pims_accounts").document(docId)
                .set(account.toCreateMap(isUpdate = false)).await()

            userDoc(uid).update(
                mapOf(
                    "workspaceInitialized" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            logActivity(
                uid = uid,
                actorId = personalId,
                actorRole = PimsRole.SUPER_ADMIN.name,
                actionType = "INIT_SUPER_ADMIN",
                targetId = personalId,
                summaryBn = "প্রাথমিক সুপার অ্যাডমিন অ্যাকাউন্ট তৈরি করা হয়েছে ($personalId)",
                summaryEn = "Initial Super Admin account created ($personalId)",
                status = "SUCCESS"
            )
            Result.success(account)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            handleFirestoreError(e, OperationType.CREATE, path)
            val (bn, _) = explainFirestoreError(e.code, path, auth.currentUser)
            Result.failure(IllegalStateException(bn, e))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun authenticatePimsUser(personalIdInput: String, password: String): PimsAuthResult {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return PimsAuthResult.Error(e.message ?: "Google Sign-In required")
        }
        val cleanId = personalIdInput.trim().uppercase()
        if (cleanId.isBlank() || password.isBlank()) {
            return PimsAuthResult.InvalidCredentials(MAX_LOGIN_ATTEMPTS)
        }
        val path = "users/$uid/pims_accounts"
        return try {
            val snap = userDoc(uid).collection("pims_accounts")
                .whereEqualTo("personalId", cleanId)
                .limit(1)
                .get()
                .await()
            val doc = snap.documents.firstOrNull()
                ?: run {
                    logActivity(
                        uid, cleanId, "UNKNOWN", "LOGIN_ATTEMPT", cleanId,
                        "অজানা পার্সোনাল আইডি দিয়ে লগইনের চেষ্টা ($cleanId)",
                        "Login attempt with unknown Personal ID ($cleanId)",
                        "FAILED"
                    )
                    return PimsAuthResult.InvalidCredentials(MAX_LOGIN_ATTEMPTS)
                }
            val account = doc.toObject(PimsAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return PimsAuthResult.Error("Invalid account record")

            if (!account.active) {
                logActivity(
                    uid, cleanId, account.role, "LOGIN_BLOCKED", cleanId,
                    "নিষ্ক্রিয় অ্যাকাউন্ট থেকে লগইনের চেষ্টা ($cleanId)",
                    "Login blocked for inactive account ($cleanId)",
                    "DENIED"
                )
                return PimsAuthResult.AccountInactive
            }

            val now = System.currentTimeMillis()
            if (account.lockedUntilEpochMs > now) {
                val remSec = ((account.lockedUntilEpochMs - now) / 1000L).coerceAtLeast(1L)
                return PimsAuthResult.RateLimited(remSec)
            }

            val ok = CryptoSecurity.verifyPassword(password, account.passwordHash, account.passwordSalt)
            if (ok) {
                if (account.failedLoginAttempts > 0L || account.lockedUntilEpochMs > 0L) {
                    userDoc(uid).collection("pims_accounts").document(account.id).update(
                        mapOf(
                            "failedLoginAttempts" to 0L,
                            "lockedUntilEpochMs" to 0L,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                logActivity(
                    uid, account.personalId, account.role, "LOGIN_SUCCESS", account.personalId,
                    "সফলভাবে লগইন করেছেন (${account.fullNameBn})",
                    "Signed in successfully (${account.fullNameEn.ifBlank { account.fullNameBn }})",
                    "SUCCESS"
                )
                PimsAuthResult.Success(account.copy(failedLoginAttempts = 0L, lockedUntilEpochMs = 0L))
            } else {
                val nextAttempts = account.failedLoginAttempts + 1L
                val lockUntil = if (nextAttempts >= MAX_LOGIN_ATTEMPTS) now + LOCKOUT_DURATION_MS else 0L
                userDoc(uid).collection("pims_accounts").document(account.id).update(
                    mapOf(
                        "failedLoginAttempts" to nextAttempts.coerceAtMost(100L),
                        "lockedUntilEpochMs" to lockUntil,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                logActivity(
                    uid, account.personalId, account.role, "LOGIN_FAILED", account.personalId,
                    "ভুল পাসওয়ার্ড প্রদানের কারণে লগইন ব্যর্থ (প্রচেষ্টা #$nextAttempts)",
                    "Failed login due to wrong password (attempt #$nextAttempts)",
                    "FAILED"
                )
                if (lockUntil > now) {
                    PimsAuthResult.RateLimited(LOCKOUT_DURATION_MS / 1000L)
                } else {
                    PimsAuthResult.InvalidCredentials((MAX_LOGIN_ATTEMPTS - nextAttempts.toInt()).coerceAtLeast(0))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            PimsAuthResult.Error(e.localizedMessage ?: "Authentication error")
        }
    }

    suspend fun generateNextPersonalId(prefix: String = "BD"): Result<String> {
        return try {
            val uid = requireUserId()
            val generated = generateUniquePersonalIdInternal(uid, prefix)
            Result.success(generated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generateUniquePersonalIdInternal(uid: String, prefix: String): String {
        val cleanPrefix = prefix.uppercase().filter { it.isLetterOrDigit() }.ifBlank { "BD" }.take(6)
        val seq = db.runTransaction { tx ->
            val snap = tx.get(userDoc(uid))
            val current = snap.getLong("nextIdSequence") ?: 1L
            tx.update(
                userDoc(uid),
                mapOf(
                    "nextIdSequence" to (current + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            current
        }.await()

        var candidate = "$cleanPrefix-${seq.toString().padStart(5, '0')}"
        var safety = 0
        while (safety < 10) {
            val dup = userDoc(uid).collection("pims_accounts")
                .whereEqualTo("personalId", candidate)
                .limit(1)
                .get()
                .await()
            if (dup.isEmpty) return candidate
            safety++
            candidate = "$cleanPrefix-${(seq + safety).toString().padStart(5, '0')}"
        }
        return "$cleanPrefix-${UUID.randomUUID().toString().take(6).uppercase()}"
    }

    suspend fun createPimsUser(
        actor: PimsAccount,
        draft: PimsAccount,
        initialPassword: String
    ): Result<PimsAccount> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/pims_accounts"
            if (!actor.roleEnum().canCreateAccounts()) {
                logActivity(uid, actor.personalId, actor.role, "CREATE_USER", draft.personalId, "অননুমোদিত অ্যাকাউন্ট তৈরির চেষ্টা", "Unauthorized user creation attempt", "DENIED")
                return Result.failure(SecurityException("Permission denied: only administrators can create accounts."))
            }
            if (draft.roleEnum() == PimsRole.SUPER_ADMIN && !actor.roleEnum().canManageAdmins()) {
                return Result.failure(SecurityException("Only Super Admin can create Super Admin accounts."))
            }
            if (draft.roleEnum() == PimsRole.ADMIN && !actor.roleEnum().canManageAdmins()) {
                return Result.failure(SecurityException("Only Super Admin can create Admin accounts."))
            }
            require(draft.fullNameBn.isNotBlank()) { "নাম (বাংলা) আবশ্যক।" }
            require(CryptoSecurity.isStrongPassword(initialPassword)) { "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।" }

            val cleanPid = draft.personalId.trim().uppercase().ifBlank {
                generateUniquePersonalIdInternal(uid, "BD")
            }
            require(cleanPid.length in 3..40) { "Personal ID must be 3-40 characters." }

            val dup = userDoc(uid).collection("pims_accounts")
                .whereEqualTo("personalId", cleanPid)
                .limit(1)
                .get()
                .await()
            if (!dup.isEmpty) {
                return Result.failure(IllegalArgumentException("এই পার্সোনাল আইডি ($cleanPid) ইতিমধ্যে ব্যবহৃত হয়েছে।"))
            }

            val hashed = CryptoSecurity.hashPassword(initialPassword)
            val docId = "acc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val entity = draft.copy(
                id = docId,
                personalId = cleanPid,
                passwordHash = hashed.hashBase64,
                passwordSalt = hashed.saltBase64,
                failedLoginAttempts = 0L,
                lockedUntilEpochMs = 0L
            )
            userDoc(uid).collection("pims_accounts").document(docId)
                .set(entity.toCreateMap(isUpdate = false))
                .await()

            logActivity(
                uid, actor.personalId, actor.role, "CREATE_USER", cleanPid,
                "নতুন ব্যবহারকারী তৈরি: ${entity.fullNameBn} ($cleanPid)",
                "Created user: ${entity.fullNameEn.ifBlank { entity.fullNameBn }} ($cleanPid)",
                "SUCCESS"
            )
            Result.success(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePimsProfile(actor: PimsAccount, updated: PimsAccount): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            require(updated.id.isNotBlank()) { "Account document ID is missing" }
            path = "users/$uid/pims_accounts/${updated.id}"
            if (!actor.roleEnum().canEditAccount(updated, actor.personalId, actor.assignedRecordIds)) {
                logActivity(uid, actor.personalId, actor.role, "UPDATE_USER", updated.personalId, "অননুমোদিত প্রোফাইল সম্পাদনার চেষ্টা", "Unauthorized profile edit attempt", "DENIED")
                return Result.failure(SecurityException("Permission denied to edit this profile."))
            }
            require(updated.fullNameBn.isNotBlank()) { "নাম (বাংলা) আবশ্যক।" }
            // Preserve immutable personalId, passwordHash/salt unless explicitly changed via password methods, and createdAt
            val existingSnap = userDoc(uid).collection("pims_accounts").document(updated.id).get().await()
            val existing = existingSnap.toObject(PimsAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.failure(IllegalStateException("Account not found"))

            val safeRole = if (actor.roleEnum().canManageAdmins()) updated.role else existing.role
            val merged = updated.copy(
                personalId = existing.personalId,
                passwordHash = existing.passwordHash,
                passwordSalt = existing.passwordSalt,
                role = safeRole,
                createdAt = existing.createdAt
            )
            userDoc(uid).collection("pims_accounts").document(updated.id)
                .set(merged.toCreateMap(isUpdate = true))
                .await()

            logActivity(
                uid, actor.personalId, actor.role, "UPDATE_USER", existing.personalId,
                "প্রোফাইল হালনাগাদ: ${merged.fullNameBn} (${existing.personalId})",
                "Updated profile: ${merged.fullNameEn.ifBlank { merged.fullNameBn }} (${existing.personalId})",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun changeOwnPassword(
        actor: PimsAccount,
        currentPassword: String,
        newPassword: String
    ): Result<PimsAccount> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/pims_accounts/${actor.id}"
            if (!CryptoSecurity.verifyPassword(currentPassword, actor.passwordHash, actor.passwordSalt)) {
                return Result.failure(IllegalArgumentException("বর্তমান পাসওয়ার্ড সঠিক নয়।"))
            }
            require(CryptoSecurity.isStrongPassword(newPassword)) { "নতুন পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।" }
            val hashed = CryptoSecurity.hashPassword(newPassword)
            userDoc(uid).collection("pims_accounts").document(actor.id).update(
                mapOf(
                    "passwordHash" to hashed.hashBase64,
                    "passwordSalt" to hashed.saltBase64,
                    "mustChangePassword" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                uid, actor.personalId, actor.role, "PASSWORD_CHANGE", actor.personalId,
                "নিজের পাসওয়ার্ড পরিবর্তন করেছেন",
                "Changed own password",
                "SUCCESS"
            )
            Result.success(
                actor.copy(
                    passwordHash = hashed.hashBase64,
                    passwordSalt = hashed.saltBase64,
                    mustChangePassword = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adminResetPassword(
        actor: PimsAccount,
        target: PimsAccount,
        newInitialPassword: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/pims_accounts/${target.id}"
            if (!actor.roleEnum().isAdminTier()) {
                return Result.failure(SecurityException("Permission denied"))
            }
            if (target.roleEnum() == PimsRole.SUPER_ADMIN && !actor.roleEnum().canManageAdmins()) {
                return Result.failure(SecurityException("Only Super Admin can reset Super Admin password"))
            }
            require(CryptoSecurity.isStrongPassword(newInitialPassword)) { "পাসওয়ার্ড কমপক্ষে ৮ অক্ষরের হতে হবে।" }
            val hashed = CryptoSecurity.hashPassword(newInitialPassword)
            userDoc(uid).collection("pims_accounts").document(target.id).update(
                mapOf(
                    "passwordHash" to hashed.hashBase64,
                    "passwordSalt" to hashed.saltBase64,
                    "mustChangePassword" to true,
                    "failedLoginAttempts" to 0L,
                    "lockedUntilEpochMs" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                uid, actor.personalId, actor.role, "ADMIN_RESET_PASSWORD", target.personalId,
                "পাসওয়ার্ড রিসেট করা হয়েছে (${target.personalId})",
                "Admin reset password for ${target.personalId}",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleAccountActive(actor: PimsAccount, target: PimsAccount, newActive: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/pims_accounts/${target.id}"
            if (!actor.roleEnum().isAdminTier()) return Result.failure(SecurityException("Permission denied"))
            if (target.personalId == actor.personalId) return Result.failure(IllegalArgumentException("নিজের অ্যাকাউন্ট নিষ্ক্রিয় করা যাবে না।"))
            if (target.roleEnum() == PimsRole.SUPER_ADMIN && !actor.roleEnum().canManageAdmins()) {
                return Result.failure(SecurityException("Cannot modify Super Admin"))
            }
            userDoc(uid).collection("pims_accounts").document(target.id).update(
                mapOf(
                    "active" to newActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logActivity(
                uid, actor.personalId, actor.role, if (newActive) "ACTIVATE_USER" else "DEACTIVATE_USER",
                target.personalId,
                if (newActive) "অ্যাকাউন্ট সক্রিয় করা হয়েছে (${target.personalId})" else "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে (${target.personalId})",
                if (newActive) "Activated account ${target.personalId}" else "Deactivated account ${target.personalId}",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePimsAccount(actor: PimsAccount, target: PimsAccount): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/pims_accounts/${target.id}"
            if (!actor.roleEnum().isAdminTier()) return Result.failure(SecurityException("Permission denied"))
            if (target.personalId == actor.personalId) return Result.failure(IllegalArgumentException("নিজের অ্যাকাউন্ট মুছে ফেলা যাবে না।"))
            if (target.roleEnum() == PimsRole.SUPER_ADMIN && !actor.roleEnum().canManageAdmins()) {
                return Result.failure(SecurityException("Only Super Admin can delete Super Admin"))
            }
            userDoc(uid).collection("pims_accounts").document(target.id).delete().await()
            logActivity(
                uid, actor.personalId, actor.role, "DELETE_USER", target.personalId,
                "ব্যবহারকারী মুছে ফেলা হয়েছে: ${target.fullNameBn} (${target.personalId})",
                "Deleted account ${target.personalId}",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveWebsiteSettings(actor: PimsAccount, settings: WebsiteSettings): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid/settings/website"
            if (!actor.roleEnum().canManageSettings()) {
                return Result.failure(SecurityException("Only Super Admin can update website settings."))
            }
            val existingSnap = userDoc(uid).collection("settings").document("website").get().await()
            val existingCreated = existingSnap.getTimestamp("createdAt")
            val payload = settings.copy(createdAt = existingCreated).toCreateMap()
            userDoc(uid).collection("settings").document("website").set(payload).await()
            logActivity(
                uid, actor.personalId, actor.role, "UPDATE_SETTINGS", "website",
                "ওয়েবসাইট ও অ্যাপ সেটিংস হালনাগাদ করা হয়েছে",
                "Updated website & branding settings",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveLocation(actor: PimsAccount, node: LocationNode): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) {
                return Result.failure(SecurityException("Admin permission required"))
            }
            require(node.nameBn.isNotBlank()) { "লোকেশনের বাংলা নাম আবশ্যক।" }
            val docId = node.id.ifBlank { "loc_${UUID.randomUUID().toString().replace("-", "").take(14)}" }
            path = "users/$uid/locations/$docId"
            val existingCreated = if (node.id.isNotBlank()) {
                userDoc(uid).collection("locations").document(docId).get().await().getTimestamp("createdAt")
            } else null
            userDoc(uid).collection("locations").document(docId)
                .set(node.copy(createdAt = existingCreated).toCreateMap())
                .await()
            logActivity(
                uid, actor.personalId, actor.role, "SAVE_LOCATION", docId,
                "লোকেশন সংরক্ষিত: ${node.nameBn} (${node.level})",
                "Saved location: ${node.nameEn.ifBlank { node.nameBn }} (${node.level})",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteLocation(actor: PimsAccount, locationId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))
            path = "users/$uid/locations/$locationId"
            userDoc(uid).collection("locations").document(locationId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * CSV format per line: level,parentNameOrId,nameBn,nameEn,code
     * Example: UPAZILA,dist_dhaka,সাভার,Savar,2672
     */
    suspend fun importLocationsCsv(
        actor: PimsAccount,
        csvContent: String,
        existingLocations: List<LocationNode>
    ): Result<Int> {
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))
            val byId = existingLocations.associateBy { it.id }
            val byName = existingLocations.associateBy { it.nameBn.trim() }
            val byNameEn = existingLocations.associateBy { it.nameEn.trim().lowercase() }
            val lines = csvContent.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("level", ignoreCase = true) }

            var addedCount = 0
            val batch = db.batch()
            lines.take(400).forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 3) {
                    val level = LocationLevel.fromRaw(cols[0].trim().uppercase())
                    val parentKey = cols[1].trim()
                    val nameBn = cols[2].trim()
                    val nameEn = cols.getOrNull(3)?.trim().orEmpty()
                    val code = cols.getOrNull(4)?.trim().orEmpty()
                    val resolvedParentId = when {
                        level == LocationLevel.DIVISION -> ""
                        byId.containsKey(parentKey) -> parentKey
                        byName.containsKey(parentKey) -> byName[parentKey]!!.id
                        byNameEn.containsKey(parentKey.lowercase()) -> byNameEn[parentKey.lowercase()]!!.id
                        else -> parentKey
                    }
                    val duplicate = existingLocations.any {
                        it.level == level.name &&
                            it.parentId == resolvedParentId &&
                            it.nameBn.equals(nameBn, ignoreCase = true)
                    }
                    if (!duplicate && nameBn.isNotBlank()) {
                        val docId = "loc_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                        val ref = userDoc(uid).collection("locations").document(docId)
                        batch.set(
                            ref,
                            LocationNode(
                                level = level.name,
                                parentId = resolvedParentId,
                                nameBn = nameBn,
                                nameEn = nameEn,
                                code = code
                            ).toCreateMap()
                        )
                        addedCount++
                    }
                }
            }
            if (addedCount > 0) {
                batch.commit().await()
                logActivity(
                    uid, actor.personalId, actor.role, "IMPORT_LOCATIONS_CSV", "",
                    "CSV থেকে $addedCount টি লোকেশন আমদানি করা হয়েছে",
                    "Imported $addedCount locations from CSV",
                    "SUCCESS"
                )
            }
            Result.success(addedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/locations")
            Result.failure(e)
        }
    }

    suspend fun saveInstitution(
        actor: PimsAccount,
        inst: EducationalInstitution,
        existingInstitutions: List<EducationalInstitution>
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))
            require(inst.nameBn.isNotBlank()) { "শিক্ষা প্রতিষ্ঠানের নাম (বাংলা) আবশ্যক।" }

            val duplicate = existingInstitutions.any {
                it.id != inst.id &&
                    it.nameBn.trim().equals(inst.nameBn.trim(), ignoreCase = true) &&
                    it.districtId == inst.districtId &&
                    it.upazilaId == inst.upazilaId
            }
            if (duplicate) {
                return Result.failure(IllegalArgumentException("এই এলাকায় একই নামে শিক্ষা প্রতিষ্ঠান ইতিমধ্যে রয়েছে।"))
            }

            val docId = inst.id.ifBlank { "inst_${UUID.randomUUID().toString().replace("-", "").take(14)}" }
            path = "users/$uid/institutions/$docId"
            val existingCreated = if (inst.id.isNotBlank()) {
                userDoc(uid).collection("institutions").document(docId).get().await().getTimestamp("createdAt")
            } else null
            userDoc(uid).collection("institutions").document(docId)
                .set(inst.copy(createdAt = existingCreated).toCreateMap())
                .await()
            logActivity(
                uid, actor.personalId, actor.role, "SAVE_INSTITUTION", docId,
                "শিক্ষা প্রতিষ্ঠান সংরক্ষিত: ${inst.nameBn}",
                "Saved institution: ${inst.nameEn.ifBlank { inst.nameBn }}",
                "SUCCESS"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteInstitution(actor: PimsAccount, institutionId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))
            userDoc(uid).collection("institutions").document(institutionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * CSV format per line: nameBn,nameEn,institutionType,eiinCode,divisionIdOrName,districtIdOrName,upazilaIdOrName
     */
    suspend fun importInstitutionsCsv(
        actor: PimsAccount,
        csvContent: String,
        locations: List<LocationNode>,
        existingInstitutions: List<EducationalInstitution>
    ): Result<Int> {
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))

            fun resolveLoc(key: String, level: LocationLevel): LocationNode? {
                val clean = key.trim()
                if (clean.isBlank()) return null
                return locations.firstOrNull {
                    it.level == level.name &&
                        (it.id == clean || it.nameBn.equals(clean, true) || it.nameEn.equals(clean, true))
                }
            }

            val lines = csvContent.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("nameBn", ignoreCase = true) }

            var addedCount = 0
            val batch = db.batch()
            lines.take(400).forEach { line ->
                val cols = parseCsvLine(line)
                if (cols.size >= 3) {
                    val nameBn = cols[0].trim()
                    val nameEn = cols.getOrNull(1)?.trim().orEmpty()
                    val type = InstitutionType.fromRaw(cols.getOrNull(2)?.trim()?.uppercase().orEmpty())
                    val eiin = cols.getOrNull(3)?.trim().orEmpty()
                    val div = resolveLoc(cols.getOrNull(4).orEmpty(), LocationLevel.DIVISION)
                    val dist = resolveLoc(cols.getOrNull(5).orEmpty(), LocationLevel.DISTRICT)
                    val upz = resolveLoc(cols.getOrNull(6).orEmpty(), LocationLevel.UPAZILA)
                    val locLabel = listOfNotNull(div?.nameBn, dist?.nameBn, upz?.nameBn).joinToString(" > ")

                    val duplicate = existingInstitutions.any {
                        it.nameBn.equals(nameBn, true) && it.districtId == (dist?.id ?: "")
                    }
                    if (!duplicate && nameBn.isNotBlank()) {
                        val docId = "inst_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                        val ref = userDoc(uid).collection("institutions").document(docId)
                        batch.set(
                            ref,
                            EducationalInstitution(
                                nameBn = nameBn,
                                nameEn = nameEn,
                                institutionType = type.name,
                                eiinCode = eiin,
                                divisionId = div?.id ?: "",
                                districtId = dist?.id ?: "",
                                upazilaId = upz?.id ?: "",
                                locationLabel = locLabel
                            ).toCreateMap()
                        )
                        addedCount++
                    }
                }
            }
            if (addedCount > 0) {
                batch.commit().await()
                logActivity(
                    uid, actor.personalId, actor.role, "IMPORT_INSTITUTIONS_CSV", "",
                    "CSV থেকে $addedCount টি শিক্ষা প্রতিষ্ঠান আমদানি করা হয়েছে",
                    "Imported $addedCount institutions from CSV",
                    "SUCCESS"
                )
            }
            Result.success(addedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveOrgPosition(actor: PimsAccount, position: OrgPosition): Result<Unit> {
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))
            require(position.levelNameBn.isNotBlank() && position.designationBn.isNotBlank()) {
                "সাংগঠনিক স্তর ও পদবী আবশ্যক।"
            }
            val docId = position.id.ifBlank { "pos_${UUID.randomUUID().toString().replace("-", "").take(14)}" }
            val existingCreated = if (position.id.isNotBlank()) {
                userDoc(uid).collection("org_positions").document(docId).get().await().getTimestamp("createdAt")
            } else null
            userDoc(uid).collection("org_positions").document(docId)
                .set(position.copy(createdAt = existingCreated).toCreateMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOrgPosition(actor: PimsAccount, positionId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            if (!actor.roleEnum().canManageMasterDatasets()) return Result.failure(SecurityException("Admin permission required"))
            userDoc(uid).collection("org_positions").document(positionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEncryptedVaultEntry(
        ownerPersonalId: String,
        category: VaultCategory,
        title: String,
        plaintextSecret: String,
        passphrase: String
    ): Result<Unit> {
        return try {
            val uid = requireUserId()
            require(title.isNotBlank() && plaintextSecret.isNotBlank() && passphrase.isNotBlank()) {
                "শিরোনাম, গোপন তথ্য এবং ভল্ট পাসফ্রেজ আবশ্যক।"
            }
            val encrypted = CryptoSecurity.encryptAesGcm(plaintextSecret, passphrase)
            val docId = "vault_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            val entry = VaultEntry(
                ownerPersonalId = ownerPersonalId,
                category = category.name,
                title = title.trim(),
                ciphertextBase64 = encrypted.ciphertextBase64,
                ivBase64 = encrypted.ivBase64,
                saltBase64 = encrypted.saltBase64
            )
            userDoc(uid).collection("vault_entries").document(docId)
                .set(entry.toCreateMap())
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultEntry(entryId: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            userDoc(uid).collection("vault_entries").document(entryId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun logActivity(
        uid: String,
        actorId: String,
        actorRole: String,
        actionType: String,
        targetId: String,
        summaryBn: String,
        summaryEn: String,
        status: String
    ) {
        try {
            val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            userDoc(uid).collection("activity_logs").document(logId).set(
                mapOf(
                    "actorPersonalId" to actorId.take(40),
                    "actorRole" to actorRole.take(30),
                    "actionType" to actionType.take(60),
                    "targetPersonalId" to targetId.take(40),
                    "summaryBn" to summaryBn.take(300),
                    "summaryEn" to summaryEn.take(300),
                    "status" to status,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Audit log write failure should not crash user flow
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    companion object {
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L
        private const val BOOTSTRAPPED_ADMIN_EMAIL = "mdalaminmiabd2880@gmail.com"
    }
}
