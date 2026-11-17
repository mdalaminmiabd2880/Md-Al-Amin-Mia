package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PimsRole {
    SUPER_ADMIN,
    ADMIN,
    EDITOR,
    VIEWER,
    NORMAL_USER;

    fun isAdminTier(): Boolean = this == SUPER_ADMIN || this == ADMIN
    fun canAccessManagement(): Boolean = this == SUPER_ADMIN || this == ADMIN || this == EDITOR || this == VIEWER
    fun canManageAdmins(): Boolean = this == SUPER_ADMIN
    fun canManageSettings(): Boolean = this == SUPER_ADMIN
    fun canManageMasterDatasets(): Boolean = this == SUPER_ADMIN || this == ADMIN
    fun canCreateAccounts(): Boolean = this == SUPER_ADMIN || this == ADMIN
    fun canExportAll(): Boolean = this == SUPER_ADMIN || this == ADMIN || this == VIEWER
    fun canFullBackup(): Boolean = this == SUPER_ADMIN

    fun canViewAccount(target: PimsAccount, selfPersonalId: String, assignedIds: List<String>): Boolean =
        when (this) {
            SUPER_ADMIN, ADMIN -> true
            EDITOR, VIEWER -> target.personalId == selfPersonalId || target.id in assignedIds || target.personalId in assignedIds || assignedIds.isEmpty()
            NORMAL_USER -> target.personalId == selfPersonalId
        }

    fun canEditAccount(target: PimsAccount, selfPersonalId: String, assignedIds: List<String>): Boolean =
        when (this) {
            SUPER_ADMIN -> true
            ADMIN -> target.roleEnum() != SUPER_ADMIN
            EDITOR -> (target.id in assignedIds || target.personalId in assignedIds || assignedIds.isEmpty()) &&
                target.roleEnum() != SUPER_ADMIN && target.roleEnum() != ADMIN
            VIEWER -> false
            NORMAL_USER -> target.personalId == selfPersonalId && target.canEditSelfProfile
        }

    companion object {
        fun fromRaw(raw: String): PimsRole =
            enumValues<PimsRole>().firstOrNull { it.name == raw } ?: NORMAL_USER
    }
}

enum class LocationLevel {
    DIVISION, DISTRICT, UPAZILA, UNION, WARD, VILLAGE;

    fun childLevel(): LocationLevel? = when (this) {
        DIVISION -> DISTRICT
        DISTRICT -> UPAZILA
        UPAZILA -> UNION
        UNION -> WARD
        WARD -> VILLAGE
        VILLAGE -> null
    }

    companion object {
        fun fromRaw(raw: String): LocationLevel =
            enumValues<LocationLevel>().firstOrNull { it.name == raw } ?: DIVISION
    }
}

enum class InstitutionType {
    SCHOOL, COLLEGE, MADRASA, UNIVERSITY, TECHNICAL, OTHER;

    companion object {
        fun fromRaw(raw: String): InstitutionType =
            enumValues<InstitutionType>().firstOrNull { it.name == raw } ?: OTHER
    }
}

enum class VaultCategory {
    CREDENTIAL, DOCUMENT, PRIVATE_NOTE;

    companion object {
        fun fromRaw(raw: String): VaultCategory =
            enumValues<VaultCategory>().firstOrNull { it.name == raw } ?: PRIVATE_NOTE
    }
}

@Keep
@IgnoreExtraProperties
data class WorkspaceMeta(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val workspaceInitialized: Boolean = false,
    val nextIdSequence: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WebsiteSettings(
    @DocumentId val id: String = "",
    val appNameBn: String = "ব্যক্তিগত তথ্য",
    val appNameEn: String = "Byaktigato Tathya (PIMS)",
    val primaryColorHex: String = "#0D4A3E",
    val accentColorHex: String = "#C9962B",
    val headerText: String = "নিরাপদ ব্যক্তিগত ও প্রাতিষ্ঠানিক তথ্য ব্যবস্থাপনা",
    val footerText: String = "© ২০২৬ ব্যক্তিগত তথ্য (PIMS) • সর্বস্বত্ব সংরক্ষিত",
    val welcomeMessageBn: String = "স্বাগতম! আপনার ব্যক্তিগত, শিক্ষাগত ও প্রাতিষ্ঠানিক তথ্য সুরক্ষিতভাবে সংরক্ষণ ও পরিচালনা করুন।",
    val welcomeMessageEn: String = "Welcome! Securely manage your personal, educational, and organizational records.",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val logoBase64: String = "",
    val showStatsSection: Boolean = true,
    val showQuickActionsSection: Boolean = true,
    val showPrivacyNotice: Boolean = true,
    val mandatoryFields: List<String> = listOf("fullNameBn", "mobileNumber"),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "appNameBn" to appNameBn.ifBlank { "ব্যক্তিগত তথ্য" }.take(80),
        "appNameEn" to appNameEn.ifBlank { "Byaktigato Tathya" }.take(80),
        "primaryColorHex" to primaryColorHex.take(9),
        "accentColorHex" to accentColorHex.take(9),
        "headerText" to headerText.take(160),
        "footerText" to footerText.take(200),
        "welcomeMessageBn" to welcomeMessageBn.take(400),
        "welcomeMessageEn" to welcomeMessageEn.take(400),
        "contactPhone" to contactPhone.take(40),
        "contactEmail" to contactEmail.take(120),
        "logoBase64" to logoBase64.take(200000),
        "showStatsSection" to showStatsSection,
        "showQuickActionsSection" to showQuickActionsSection,
        "showPrivacyNotice" to showPrivacyNotice,
        "mandatoryFields" to mandatoryFields.take(30),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class PimsAccount(
    @DocumentId val id: String = "",
    val personalId: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val role: String = PimsRole.NORMAL_USER.name,
    val active: Boolean = true,
    val mustChangePassword: Boolean = true,
    val failedLoginAttempts: Long = 0L,
    val lockedUntilEpochMs: Long = 0L,
    val canEditSelfProfile: Boolean = false,
    val assignedRecordIds: List<String> = emptyList(),
    // A. Basic Information
    val fullNameBn: String = "",
    val fullNameEn: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val bloodGroup: String = "",
    val mobileNumber: String = "",
    val emailAddress: String = "",
    val photoBase64: String = "",
    // B. Educational Information
    val institutionId: String = "",
    val institutionName: String = "",
    val institutionType: String = "",
    val currentClassOrYear: String = "",
    val educationalQualification: String = "",
    val departmentOrSubject: String = "",
    val academicInfo: String = "",
    // C. Personal Goals
    val careerGoal: String = "",
    val personalIntro: String = "",
    val skills: String = "",
    val interests: String = "",
    // D. Organizational Information
    val orgLevel: String = "",
    val orgDesignation: String = "",
    val orgJoiningDate: String = "",
    val orgStatus: String = "",
    val orgAdditionalInfo: String = "",
    // E. Address (Cascading Bangladesh Geography)
    val divisionId: String = "",
    val divisionName: String = "",
    val districtId: String = "",
    val districtName: String = "",
    val upazilaId: String = "",
    val upazilaName: String = "",
    val unionId: String = "",
    val unionName: String = "",
    val wardId: String = "",
    val wardName: String = "",
    val villageName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): PimsRole = PimsRole.fromRaw(role)

    fun fullAddressLabel(): String =
        listOf(villageName, wardName, unionName, upazilaName, districtName, divisionName)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    fun toCreateMap(isUpdate: Boolean = false): Map<String, Any> = mapOf(
        "personalId" to personalId.take(40),
        "passwordHash" to passwordHash.take(256),
        "passwordSalt" to passwordSalt.take(128),
        "role" to roleEnum().name,
        "active" to active,
        "mustChangePassword" to mustChangePassword,
        "failedLoginAttempts" to failedLoginAttempts.coerceIn(0L, 100L),
        "lockedUntilEpochMs" to lockedUntilEpochMs.coerceAtLeast(0L),
        "canEditSelfProfile" to canEditSelfProfile,
        "assignedRecordIds" to assignedRecordIds.take(100),
        "fullNameBn" to fullNameBn.take(120),
        "fullNameEn" to fullNameEn.take(120),
        "fatherName" to fatherName.take(120),
        "motherName" to motherName.take(120),
        "dateOfBirth" to dateOfBirth.take(30),
        "gender" to gender.take(30),
        "bloodGroup" to bloodGroup.take(12),
        "mobileNumber" to mobileNumber.take(30),
        "emailAddress" to emailAddress.take(120),
        "photoBase64" to photoBase64.take(200000),
        "institutionId" to institutionId.take(128),
        "institutionName" to institutionName.take(160),
        "institutionType" to institutionType.take(40),
        "currentClassOrYear" to currentClassOrYear.take(80),
        "educationalQualification" to educationalQualification.take(120),
        "departmentOrSubject" to departmentOrSubject.take(120),
        "academicInfo" to academicInfo.take(500),
        "careerGoal" to careerGoal.take(400),
        "personalIntro" to personalIntro.take(800),
        "skills" to skills.take(400),
        "interests" to interests.take(400),
        "orgLevel" to orgLevel.take(80),
        "orgDesignation" to orgDesignation.take(120),
        "orgJoiningDate" to orgJoiningDate.take(30),
        "orgStatus" to orgStatus.take(40),
        "orgAdditionalInfo" to orgAdditionalInfo.take(500),
        "divisionId" to divisionId.take(128),
        "divisionName" to divisionName.take(80),
        "districtId" to districtId.take(128),
        "districtName" to districtName.take(80),
        "upazilaId" to upazilaId.take(128),
        "upazilaName" to upazilaName.take(80),
        "unionId" to unionId.take(128),
        "unionName" to unionName.take(80),
        "wardId" to wardId.take(128),
        "wardName" to wardName.take(80),
        "villageName" to villageName.take(120),
        "createdAt" to (if (isUpdate && createdAt != null) createdAt else FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class LocationNode(
    @DocumentId val id: String = "",
    val level: String = LocationLevel.DIVISION.name,
    val parentId: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val code: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun levelEnum(): LocationLevel = LocationLevel.fromRaw(level)

    fun displayName(isBengali: Boolean): String = when {
        isBengali && nameBn.isNotBlank() && nameEn.isNotBlank() -> "$nameBn ($nameEn)"
        isBengali -> nameBn.ifBlank { nameEn }
        else -> nameEn.ifBlank { nameBn }
    }

    fun toCreateMap(): Map<String, Any> = mapOf(
        "level" to levelEnum().name,
        "parentId" to parentId.take(128),
        "nameBn" to nameBn.take(120),
        "nameEn" to nameEn.take(120),
        "code" to code.take(40),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class EducationalInstitution(
    @DocumentId val id: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val institutionType: String = InstitutionType.SCHOOL.name,
    val eiinCode: String = "",
    val divisionId: String = "",
    val districtId: String = "",
    val upazilaId: String = "",
    val unionId: String = "",
    val wardId: String = "",
    val locationLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun typeEnum(): InstitutionType = InstitutionType.fromRaw(institutionType)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "nameBn" to nameBn.take(160),
        "nameEn" to nameEn.take(160),
        "institutionType" to typeEnum().name,
        "eiinCode" to eiinCode.take(40),
        "divisionId" to divisionId.take(128),
        "districtId" to districtId.take(128),
        "upazilaId" to upazilaId.take(128),
        "unionId" to unionId.take(128),
        "wardId" to wardId.take(128),
        "locationLabel" to locationLabel.take(240),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class OrgPosition(
    @DocumentId val id: String = "",
    val levelNameBn: String = "",
    val levelNameEn: String = "",
    val designationBn: String = "",
    val designationEn: String = "",
    val rankOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "levelNameBn" to levelNameBn.take(100),
        "levelNameEn" to levelNameEn.take(100),
        "designationBn" to designationBn.take(120),
        "designationEn" to designationEn.take(120),
        "rankOrder" to rankOrder.coerceIn(0L, 10000L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class VaultEntry(
    @DocumentId val id: String = "",
    val ownerPersonalId: String = "",
    val category: String = VaultCategory.PRIVATE_NOTE.name,
    val title: String = "",
    val ciphertextBase64: String = "",
    val ivBase64: String = "",
    val saltBase64: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): VaultCategory = VaultCategory.fromRaw(category)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "ownerPersonalId" to ownerPersonalId.take(40),
        "category" to categoryEnum().name,
        "title" to title.take(140),
        "ciphertextBase64" to ciphertextBase64.take(400000),
        "ivBase64" to ivBase64.take(64),
        "saltBase64" to saltBase64.take(128),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val actorPersonalId: String = "",
    val actorRole: String = "",
    val actionType: String = "",
    val targetPersonalId: String = "",
    val summaryBn: String = "",
    val summaryEn: String = "",
    val status: String = "SUCCESS",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class DiagnosticCheckStatus {
    CHECKING,
    SUCCESS,
    FAILED
}

data class AuthPermissionDiagnostics(
    val isLoading: Boolean = true,
    val googleEmail: String = "",
    val firebaseUid: String = "",
    val emailVerified: Boolean = false,
    val isFirebaseSignedIn: Boolean = false,
    val authProviders: List<String> = emptyList(),
    val emailMatchesBootstrappedAdmin: Boolean = false,
    val effectiveUserRole: String = "Normal User",
    val effectiveUserRoleBn: String = "সাধারণ ব্যবহারকারী (Normal User)",
    val adminPermissionGranted: Boolean = false,
    val adminsDocExists: Boolean? = null,
    val adminsDocActive: Boolean? = null,
    val adminsDocRole: String = "",
    val bootstrapStatus: DiagnosticCheckStatus = DiagnosticCheckStatus.CHECKING,
    val bootstrapDetailBn: String = "",
    val bootstrapDetailEn: String = "",
    val lastFirestoreErrorCode: String = "",
    val lastFirestoreErrorReasonBn: String = "",
    val lastFirestoreErrorReasonEn: String = "",
    val checkedAtEpochMs: Long = 0L
)

