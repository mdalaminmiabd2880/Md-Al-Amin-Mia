package com.example.data.geo

import android.content.Context
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.seed.BangladeshGeoSeed
import org.json.JSONObject
import java.io.InputStream
import java.text.Normalizer

enum class GeoVerificationStatus {
    VERIFIED,
    AWAITING_VERIFICATION,
    CONFLICT;

    fun label(lang: AppLang): String = when (this) {
        VERIFIED -> AppStrings.pick(lang, "যাচাইকৃত", "Verified")
        AWAITING_VERIFICATION -> AppStrings.pick(lang, "যাচাইাধীন", "Awaiting Verification")
        CONFLICT -> AppStrings.pick(lang, "অসঙ্গতি / আংশিক", "Conflict / Partial")
    }

    companion object {
        fun fromRaw(raw: String): GeoVerificationStatus =
            enumValues<GeoVerificationStatus>().firstOrNull { it.name == raw.uppercase() }
                ?: AWAITING_VERIFICATION
    }
}

data class GeoSourceRef(
    val title: String,
    val url: String
)

data class GeoDatasetMeta(
    val version: String = "1.0.0",
    val datasetId: String = "sundarganj_geo_v1",
    val lastVerifiedAt: String = "",
    val divisionId: String = "div_rangpur",
    val divisionNameBn: String = "রংপুর",
    val districtId: String = "dist_gaibandha",
    val districtNameBn: String = "গাইবান্ধা",
    val upazilaId: String = "upz_sundarganj",
    val upazilaNameBn: String = "সুন্দরগঞ্জ",
    val upazilaNameEn: String = "Sundarganj",
    val totalUnions: Int = 15,
    val wardsPerUnion: Int = 9,
    val totalWards: Int = 135,
    val upazilaVillageCountOfficial: Int = 148,
    val upazilaMouzaCountOfficial: Int = 110,
    val discrepancyAnalysisBn: String = "",
    val discrepancyAnalysisEn: String = "",
    val sources: List<GeoSourceRef> = emptyList()
)

data class GeoReferenceRecord(
    val id: String,
    val level: LocationLevel,
    val parentId: String,
    val unionId: String,
    val nameBn: String,
    val nameEn: String,
    val code: String,
    val aliasesBn: List<String> = emptyList(),
    val wardNumber: Int? = null,
    val verifiedWardNumbers: List<Int> = emptyList(),
    val verifiedWardIds: List<String> = emptyList(),
    val officialVillageCount: Int? = null,
    val verificationStatus: GeoVerificationStatus = GeoVerificationStatus.AWAITING_VERIFICATION,
    val sourceUrl: String = "",
    val lastVerifiedAt: String = "",
    val conflictNoteBn: String = "",
    val conflictNoteEn: String = ""
)

data class MergedGeoNode(
    val node: LocationNode,
    val isOfficialReference: Boolean,
    val isAdminOverridden: Boolean,
    val reference: GeoReferenceRecord?,
    val unionId: String,
    val effectiveWardIds: List<String>,
    val effectiveStatus: GeoVerificationStatus,
    val aliasesBn: List<String> = emptyList(),
    val sourceUrl: String = "",
    val lastVerifiedAt: String = "",
    val noteBn: String = "",
    val noteEn: String = ""
)

data class UnionGeoSummary(
    val unionNode: LocationNode,
    val reference: GeoReferenceRecord,
    val wardCount: Int,
    val collectedVillageCount: Int,
    val verifiedVillageCount: Int
)

data class WardVillageResolution(
    val wardMappingVerified: Boolean,
    val wardMappedVillages: List<MergedGeoNode>,
    val unionVillages: List<MergedGeoNode>
)

object BengaliTextSearch {
    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val nfc = Normalizer.normalize(input.trim(), Normalizer.Form.NFC)
        return nfc
            .lowercase()
            // Normalize composed vs decomposed Bengali nukta letters (য়/য়, ড়/ড়, ঢ়/ঢ়)
            .replace("\u09AF\u09BC", "\u09DF")
            .replace("\u09A1\u09BC", "\u09DC")
            .replace("\u09A2\u09BC", "\u09DD")
            // Also normalize য vs য় softly when user types approximate Bengali phonetic spelling? No, keep য distinct from য়, only unify nukta forms.
            .replace(Regex("\\s+"), " ")
    }

    fun matches(query: String, vararg candidates: String): Boolean {
        val normQ = normalize(query)
        if (normQ.isBlank()) return true
        return candidates.any { candidate ->
            candidate.isNotBlank() && normalize(candidate).contains(normQ)
        }
    }

    fun toBengaliDigits(number: Int): String {
        val bn = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { ch ->
            if (ch in '0'..'9') bn[ch - '0'] else ch
        }.joinToString("")
    }
}

class SundarganjGeoRepository(private val assetOpener: () -> InputStream) {

    constructor(context: Context) : this({
        context.applicationContext.assets.open(ASSET_FILE_NAME)
    })

    private val loadedData: LoadedBundle by lazy { parseAsset() }

    val meta: GeoDatasetMeta
        get() = loadedData.meta

    val referenceRecords: List<GeoReferenceRecord>
        get() = loadedData.records

    val referenceById: Map<String, GeoReferenceRecord>
        get() = loadedData.recordById

    val seedAndReferenceLocations: List<LocationNode>
        get() = loadedData.baseLocationNodes

    fun mergeWithFirestore(firestoreLocations: List<LocationNode>): List<MergedGeoNode> {
        val firestoreById = firestoreLocations.associateBy { it.id }
        val consumedIds = LinkedHashSet<String>()
        val result = ArrayList<MergedGeoNode>(loadedData.baseLocationNodes.size + firestoreLocations.size)

        // Precompute union lookup by ward ID so admin-assigned villages resolve their unionId accurately
        val wardToUnion = HashMap<String, String>()
        loadedData.records
            .filter { it.level == LocationLevel.WARD }
            .forEach { wardToUnion[it.id] = it.unionId }
        firestoreLocations
            .filter { it.level == LocationLevel.WARD.name }
            .forEach { ward ->
                if (ward.parentId.isNotBlank()) wardToUnion[ward.id] = ward.parentId
            }

        // 1. Keep Bangladesh 8 Divisions + 64 Districts base + Sundarganj Upazila/Unions/Wards/Villages
        for (baseNode in loadedData.baseLocationNodes) {
            val ref = loadedData.recordById[baseNode.id]
            val overrideNode = firestoreById[baseNode.id]
            consumedIds.add(baseNode.id)

            val effectiveNode = if (overrideNode != null) {
                baseNode.copy(
                    level = overrideNode.level.ifBlank { baseNode.level },
                    parentId = overrideNode.parentId,
                    nameBn = overrideNode.nameBn.ifBlank { baseNode.nameBn },
                    nameEn = overrideNode.nameEn.ifBlank { baseNode.nameEn },
                    code = overrideNode.code.ifBlank { baseNode.code },
                    createdAt = overrideNode.createdAt,
                    updatedAt = overrideNode.updatedAt
                )
            } else {
                baseNode
            }

            val resolvedUnionId = when {
                ref != null && ref.unionId.isNotBlank() -> ref.unionId
                effectiveNode.level == LocationLevel.UNION.name -> effectiveNode.id
                effectiveNode.level == LocationLevel.WARD.name -> effectiveNode.parentId
                effectiveNode.level == LocationLevel.VILLAGE.name ->
                    wardToUnion[effectiveNode.parentId] ?: effectiveNode.parentId
                else -> ""
            }

            val effectiveWards = buildEffectiveWardIds(
                node = effectiveNode,
                ref = ref,
                overrideNode = overrideNode,
                wardToUnion = wardToUnion
            )

            val effectiveStatus = computeEffectiveStatus(
                node = effectiveNode,
                ref = ref,
                overrideNode = overrideNode,
                effectiveWardIds = effectiveWards
            )

            result.add(
                MergedGeoNode(
                    node = effectiveNode,
                    isOfficialReference = ref != null,
                    isAdminOverridden = overrideNode != null,
                    reference = ref,
                    unionId = resolvedUnionId,
                    effectiveWardIds = effectiveWards,
                    effectiveStatus = effectiveStatus,
                    aliasesBn = ref?.aliasesBn.orEmpty(),
                    sourceUrl = ref?.sourceUrl.orEmpty(),
                    lastVerifiedAt = ref?.lastVerifiedAt ?: loadedData.meta.lastVerifiedAt,
                    noteBn = ref?.conflictNoteBn.orEmpty(),
                    noteEn = ref?.conflictNoteEn.orEmpty()
                )
            )
        }

        // 2. Append any purely custom Firestore locations created by Admins
        for (fsNode in firestoreLocations) {
            if (fsNode.id in consumedIds) continue
            consumedIds.add(fsNode.id)
            val resolvedUnionId = when (fsNode.levelEnum()) {
                LocationLevel.UNION -> fsNode.id
                LocationLevel.WARD -> fsNode.parentId
                LocationLevel.VILLAGE -> wardToUnion[fsNode.parentId] ?: fsNode.parentId
                else -> ""
            }
            val effectiveWards = if (
                fsNode.level == LocationLevel.VILLAGE.name &&
                fsNode.parentId.isNotBlank() &&
                fsNode.parentId != resolvedUnionId
            ) {
                listOf(fsNode.parentId)
            } else {
                emptyList()
            }
            val status = if (fsNode.level != LocationLevel.VILLAGE.name || effectiveWards.isNotEmpty()) {
                GeoVerificationStatus.VERIFIED
            } else {
                GeoVerificationStatus.AWAITING_VERIFICATION
            }
            result.add(
                MergedGeoNode(
                    node = fsNode,
                    isOfficialReference = false,
                    isAdminOverridden = true,
                    reference = null,
                    unionId = resolvedUnionId,
                    effectiveWardIds = effectiveWards,
                    effectiveStatus = status,
                    sourceUrl = "",
                    lastVerifiedAt = "",
                    noteBn = if (fsNode.level == LocationLevel.VILLAGE.name && effectiveWards.isEmpty()) {
                        "অ্যাডমিন সংযোজিত (ওয়ার্ড যাচাইাধীন)"
                    } else {
                        "অ্যাডমিন সংযোজিত রেকর্ড"
                    },
                    noteEn = if (fsNode.level == LocationLevel.VILLAGE.name && effectiveWards.isEmpty()) {
                        "Admin added (ward verification pending)"
                    } else {
                        "Admin-added custom record"
                    }
                )
            )
        }

        return result
    }

    fun resolveVillagesForSelection(
        mergedNodes: List<MergedGeoNode>,
        unionId: String,
        wardId: String
    ): WardVillageResolution {
        if (unionId.isBlank()) {
            return WardVillageResolution(
                wardMappingVerified = false,
                wardMappedVillages = emptyList(),
                unionVillages = emptyList()
            )
        }
        val unionVillages = mergedNodes
            .filter { it.node.level == LocationLevel.VILLAGE.name && it.unionId == unionId }
            .sortedBy { it.node.nameBn }

        if (wardId.isBlank()) {
            return WardVillageResolution(
                wardMappingVerified = false,
                wardMappedVillages = emptyList(),
                unionVillages = unionVillages
            )
        }

        val wardMapped = unionVillages.filter { v ->
            wardId in v.effectiveWardIds || v.node.parentId == wardId
        }
        return WardVillageResolution(
            wardMappingVerified = wardMapped.isNotEmpty(),
            wardMappedVillages = wardMapped,
            unionVillages = unionVillages
        )
    }

    fun computeUnionSummaries(mergedNodes: List<MergedGeoNode>): List<UnionGeoSummary> {
        val unions = mergedNodes
            .filter { it.node.level == LocationLevel.UNION.name && it.node.parentId == loadedData.meta.upazilaId }
            .sortedBy { it.node.code }
        val wardsByUnion = mergedNodes
            .filter { it.node.level == LocationLevel.WARD.name }
            .groupBy { it.unionId }
        val villagesByUnion = mergedNodes
            .filter { it.node.level == LocationLevel.VILLAGE.name }
            .groupBy { it.unionId }

        return unions.mapNotNull { u ->
            val ref = u.reference ?: return@mapNotNull null
            val vList = villagesByUnion[u.node.id].orEmpty()
            UnionGeoSummary(
                unionNode = u.node,
                reference = ref,
                wardCount = wardsByUnion[u.node.id].orEmpty().size,
                collectedVillageCount = vList.size,
                verifiedVillageCount = vList.count { it.effectiveStatus == GeoVerificationStatus.VERIFIED }
            )
        }
    }

    private fun buildEffectiveWardIds(
        node: LocationNode,
        ref: GeoReferenceRecord?,
        overrideNode: LocationNode?,
        wardToUnion: Map<String, String>
    ): List<String> {
        if (node.level != LocationLevel.VILLAGE.name) return emptyList()
        if (overrideNode != null && overrideNode.parentId in wardToUnion) {
            return listOf(overrideNode.parentId)
        }
        return ref?.verifiedWardIds.orEmpty()
    }

    private fun computeEffectiveStatus(
        node: LocationNode,
        ref: GeoReferenceRecord?,
        overrideNode: LocationNode?,
        effectiveWardIds: List<String>
    ): GeoVerificationStatus {
        if (overrideNode != null && node.level == LocationLevel.VILLAGE.name && effectiveWardIds.isNotEmpty()) {
            return GeoVerificationStatus.VERIFIED
        }
        return ref?.verificationStatus ?: GeoVerificationStatus.VERIFIED
    }

    private fun parseAsset(): LoadedBundle {
        val rawJson = assetOpener().bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(rawJson)
        val metaJson = root.getJSONObject("meta")

        val sourcesJson = metaJson.optJSONArray("sources")
        val sources = buildList {
            if (sourcesJson != null) {
                for (i in 0 until sourcesJson.length()) {
                    val obj = sourcesJson.getJSONObject(i)
                    add(GeoSourceRef(obj.optString("title"), obj.optString("url")))
                }
            }
        }

        val meta = GeoDatasetMeta(
            version = metaJson.optString("version", "1.0.0"),
            datasetId = metaJson.optString("datasetId", "sundarganj_geo_v1"),
            lastVerifiedAt = metaJson.optString("lastVerifiedAt", ""),
            divisionId = metaJson.optString("divisionId", "div_rangpur"),
            divisionNameBn = metaJson.optString("divisionNameBn", "রংপুর"),
            districtId = metaJson.optString("districtId", "dist_gaibandha"),
            districtNameBn = metaJson.optString("districtNameBn", "গাইবান্ধা"),
            upazilaId = metaJson.optString("upazilaId", "upz_sundarganj"),
            upazilaNameBn = metaJson.optString("upazilaNameBn", "সুন্দরগঞ্জ"),
            upazilaNameEn = metaJson.optString("upazilaNameEn", "Sundarganj"),
            totalUnions = metaJson.optInt("totalUnions", 15),
            wardsPerUnion = metaJson.optInt("wardsPerUnion", 9),
            totalWards = metaJson.optInt("totalWards", 135),
            upazilaVillageCountOfficial = metaJson.optInt("upazilaVillageCountOfficial", 148),
            upazilaMouzaCountOfficial = metaJson.optInt("upazilaMouzaCountOfficial", 110),
            discrepancyAnalysisBn = metaJson.optString("discrepancyAnalysisBn", ""),
            discrepancyAnalysisEn = metaJson.optString("discrepancyAnalysisEn", ""),
            sources = sources
        )

        val records = ArrayList<GeoReferenceRecord>()
        val baseNodes = ArrayList<LocationNode>()

        // Base 8 divisions & 64 districts (offline guaranteed)
        BangladeshGeoSeed.divisionsAndDistricts.forEach { seed ->
            baseNodes.add(
                LocationNode(
                    id = seed.id,
                    level = seed.level.name,
                    parentId = seed.parentId,
                    nameBn = seed.nameBn,
                    nameEn = seed.nameEn,
                    code = seed.code
                )
            )
        }

        // Sundarganj Upazila node
        val upazilaSourceUrl = "https://sundarganj.gaibandha.gov.bd"
        val upazilaRecord = GeoReferenceRecord(
            id = meta.upazilaId,
            level = LocationLevel.UPAZILA,
            parentId = meta.districtId,
            unionId = "",
            nameBn = meta.upazilaNameBn,
            nameEn = meta.upazilaNameEn,
            code = metaJson.optString("upazilaCode", "3291"),
            verificationStatus = GeoVerificationStatus.VERIFIED,
            sourceUrl = upazilaSourceUrl,
            lastVerifiedAt = meta.lastVerifiedAt,
            conflictNoteBn = meta.discrepancyAnalysisBn,
            conflictNoteEn = meta.discrepancyAnalysisEn
        )
        records.add(upazilaRecord)
        baseNodes.add(
            LocationNode(
                id = upazilaRecord.id,
                level = LocationLevel.UPAZILA.name,
                parentId = upazilaRecord.parentId,
                nameBn = upazilaRecord.nameBn,
                nameEn = upazilaRecord.nameEn,
                code = upazilaRecord.code
            )
        )

        val verifiedWardIdsLookup = HashSet<String>()
        val villagesArray = root.getJSONArray("villages")
        val unionCodeById = HashMap<String, String>()

        val unionsArray = root.getJSONArray("unions")
        for (i in 0 until unionsArray.length()) {
            val uObj = unionsArray.getJSONObject(i)
            val uId = uObj.getString("id")
            val uCode = uObj.getString("code")
            unionCodeById[uId] = uCode
        }

        // First scan villages so we know which wards have verified villages mapped
        for (i in 0 until villagesArray.length()) {
            val vObj = villagesArray.getJSONObject(i)
            val unionId = vObj.getString("unionId")
            val uCode = unionCodeById[unionId].orEmpty()
            val wArr = vObj.optJSONArray("verifiedWardNumbers")
            if (wArr != null) {
                for (j in 0 until wArr.length()) {
                    val wNum = wArr.getInt(j)
                    verifiedWardIdsLookup.add(wardIdFor(uCode, wNum))
                }
            }
        }

        // Unions & deterministic 1-9 Wards (15 * 9 = 135 wards)
        for (i in 0 until unionsArray.length()) {
            val uObj = unionsArray.getJSONObject(i)
            val uId = uObj.getString("id")
            val uCode = uObj.getString("code")
            val aliases = buildList {
                val arr = uObj.optJSONArray("aliasesBn")
                if (arr != null) for (k in 0 until arr.length()) add(arr.getString(k))
            }
            val uStatus = GeoVerificationStatus.fromRaw(uObj.optString("verificationStatus", "AWAITING_VERIFICATION"))
            val uSource = uObj.optString("sourceUrl", "")
            val uVerifiedAt = uObj.optString("lastVerifiedAt", meta.lastVerifiedAt)
            val uRecord = GeoReferenceRecord(
                id = uId,
                level = LocationLevel.UNION,
                parentId = meta.upazilaId,
                unionId = uId,
                nameBn = uObj.getString("nameBn"),
                nameEn = uObj.optString("nameEn", ""),
                code = uCode,
                aliasesBn = aliases,
                officialVillageCount = if (uObj.has("officialVillageCount")) uObj.getInt("officialVillageCount") else null,
                verificationStatus = uStatus,
                sourceUrl = uSource,
                lastVerifiedAt = uVerifiedAt,
                conflictNoteBn = uObj.optString("notesBn", ""),
                conflictNoteEn = uObj.optString("notesEn", "")
            )
            records.add(uRecord)
            baseNodes.add(
                LocationNode(
                    id = uRecord.id,
                    level = LocationLevel.UNION.name,
                    parentId = uRecord.parentId,
                    nameBn = uRecord.nameBn,
                    nameEn = uRecord.nameEn,
                    code = uRecord.code
                )
            )

            for (wNum in 1..meta.wardsPerUnion) {
                val wardId = wardIdFor(uCode, wNum)
                val wardMapped = wardId in verifiedWardIdsLookup
                val wRecord = GeoReferenceRecord(
                    id = wardId,
                    level = LocationLevel.WARD,
                    parentId = uId,
                    unionId = uId,
                    nameBn = "${BengaliTextSearch.toBengaliDigits(wNum)} নং ওয়ার্ড",
                    nameEn = "Ward ${wNum.toString().padStart(2, '0')}",
                    code = wNum.toString().padStart(2, '0'),
                    wardNumber = wNum,
                    verificationStatus = if (wardMapped) {
                        GeoVerificationStatus.VERIFIED
                    } else {
                        GeoVerificationStatus.AWAITING_VERIFICATION
                    },
                    sourceUrl = uSource,
                    lastVerifiedAt = uVerifiedAt,
                    conflictNoteBn = if (wardMapped) {
                        "এই ওয়ার্ডের দাপ্তরিক গ্রাম বিন্যাস যাচাইকৃত।"
                    } else {
                        "এই ওয়ার্ডের গ্রাম-ওয়ার্ড ম্যাপিং যাচাইাধীন।"
                    },
                    conflictNoteEn = if (wardMapped) {
                        "Ward-to-village mapping verified from official source."
                    } else {
                        "Ward-to-village mapping awaiting official verification."
                    }
                )
                records.add(wRecord)
                baseNodes.add(
                    LocationNode(
                        id = wRecord.id,
                        level = LocationLevel.WARD.name,
                        parentId = wRecord.parentId,
                        nameBn = wRecord.nameBn,
                        nameEn = wRecord.nameEn,
                        code = wRecord.code
                    )
                )
            }
        }

        // Villages
        for (i in 0 until villagesArray.length()) {
            val vObj = villagesArray.getJSONObject(i)
            val vId = vObj.getString("id")
            val unionId = vObj.getString("unionId")
            val uCode = unionCodeById[unionId].orEmpty()
            val aliases = buildList {
                val arr = vObj.optJSONArray("aliasesBn")
                if (arr != null) for (k in 0 until arr.length()) add(arr.getString(k))
            }
            val wardNums = buildList {
                val arr = vObj.optJSONArray("verifiedWardNumbers")
                if (arr != null) for (k in 0 until arr.length()) add(arr.getInt(k))
            }
            val wardIds = wardNums.map { wardIdFor(uCode, it) }
            val vStatus = GeoVerificationStatus.fromRaw(vObj.optString("verificationStatus", "AWAITING_VERIFICATION"))
            val primaryParent = wardIds.firstOrNull() ?: unionId
            val vRecord = GeoReferenceRecord(
                id = vId,
                level = LocationLevel.VILLAGE,
                parentId = primaryParent,
                unionId = unionId,
                nameBn = vObj.getString("nameBn"),
                nameEn = vObj.optString("nameEn", ""),
                code = if (wardNums.isNotEmpty()) {
                    "W-${wardNums.joinToString(",")}"
                } else {
                    "UNVERIFIED_WARD"
                },
                aliasesBn = aliases,
                verifiedWardNumbers = wardNums,
                verifiedWardIds = wardIds,
                verificationStatus = vStatus,
                sourceUrl = vObj.optString("sourceUrl", ""),
                lastVerifiedAt = meta.lastVerifiedAt,
                conflictNoteBn = vObj.optString("conflictNoteBn", if (wardIds.isEmpty()) "ওয়ার্ড ম্যাপিং যাচাইাধীন" else ""),
                conflictNoteEn = vObj.optString("conflictNoteEn", if (wardIds.isEmpty()) "Ward mapping awaiting verification" else "")
            )
            records.add(vRecord)
            baseNodes.add(
                LocationNode(
                    id = vRecord.id,
                    level = LocationLevel.VILLAGE.name,
                    parentId = vRecord.parentId,
                    nameBn = vRecord.nameBn,
                    nameEn = vRecord.nameEn,
                    code = vRecord.code
                )
            )
        }

        return LoadedBundle(
            meta = meta,
            records = records,
            recordById = records.associateBy { it.id },
            baseLocationNodes = baseNodes
        )
    }

    private data class LoadedBundle(
        val meta: GeoDatasetMeta,
        val records: List<GeoReferenceRecord>,
        val recordById: Map<String, GeoReferenceRecord>,
        val baseLocationNodes: List<LocationNode>
    )

    companion object {
        const val ASSET_FILE_NAME = "sundarganj_geo_v1.json"

        fun wardIdFor(unionCode: String, wardNumber: Int): String {
            val w = wardNumber.toString().padStart(2, '0')
            return "ward_sundarganj_${unionCode}_w$w"
        }
    }
}
