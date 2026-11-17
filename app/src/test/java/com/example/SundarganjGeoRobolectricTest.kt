package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.core.localization.AppLang
import com.example.data.export.PdfAndExportEngine
import com.example.data.geo.BengaliTextSearch
import com.example.data.geo.GeoVerificationStatus
import com.example.data.geo.SundarganjGeoRepository
import com.example.data.model.LocationLevel
import com.example.data.model.LocationNode
import com.example.data.model.PimsAccount
import com.example.data.model.PimsRole
import com.example.data.model.WebsiteSettings
import com.example.ui.screens.DataManagementScreen
import com.example.ui.screens.UserFormScreen
import com.example.ui.theme.ByaktigatoTheme
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SundarganjGeoRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun createRepo(): SundarganjGeoRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return SundarganjGeoRepository(context)
    }

    @Test
    fun `sundarganj dataset contains exact 15 unions 135 wards and zero duplicate ids`() {
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())

        // Base 72 + Upazila 1 + 15 unions + 135 wards + 133 villages
        val uniqueIds = merged.map { it.node.id }.toSet()
        assertEquals("Duplicate IDs detected in merged geography", merged.size, uniqueIds.size)

        val upazila = merged.first { it.node.id == "upz_sundarganj" }
        assertEquals("dist_gaibandha", upazila.node.parentId)
        assertEquals("সুন্দরগঞ্জ", upazila.node.nameBn)

        val expectedUnions = listOf(
            "বামনডাঙ্গা",
            "সোনারায়",
            "তারাপুর",
            "বেলকা",
            "দহবন্দ",
            "সর্বানন্দ",
            "রামজীবন",
            "ধোপাডাঙ্গা",
            "ছাপরহাটী",
            "শান্তিরাম",
            "হরিপুর",
            "কঞ্চিবাড়ী",
            "শ্রীপুর",
            "চন্ডিপুর",
            "কাপাসিয়া"
        )
        val unions = merged
            .filter { it.node.level == LocationLevel.UNION.name && it.node.parentId == "upz_sundarganj" }
            .sortedBy { it.node.code }

        assertEquals(15, unions.size)
        assertEquals(expectedUnions, unions.map { it.node.nameBn })

        val wards = merged.filter { it.node.level == LocationLevel.WARD.name }
        assertEquals(135, wards.size)
        unions.forEach { union ->
            val unionWards = wards.filter { it.node.parentId == union.node.id }
            assertEquals("Union ${union.node.nameBn} must have 9 wards", 9, unionWards.size)
            assertEquals((1..9).map { it.toString().padStart(2, '0') }, unionWards.map { it.node.code }.sorted())
        }

        val unionIds = unions.map { it.node.id }.toSet()
        val villages = merged.filter { it.node.level == LocationLevel.VILLAGE.name }
        assertEquals(133, villages.size)
        assertTrue(villages.all { it.unionId in unionIds })
    }

    @Test
    fun `verified ward mappings resolve strictly while unverified wards report pending verification`() {
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())

        // Sonaray W1 -> চন্দ্র
        val sonarayW1 = repo.resolveVillagesForSelection(
            mergedNodes = merged,
            unionId = "un_sundarganj_02_sonaray",
            wardId = SundarganjGeoRepository.wardIdFor("02", 1)
        )
        assertTrue(sonarayW1.wardMappingVerified)
        assertEquals(listOf("চন্দ্র"), sonarayW1.wardMappedVillages.map { it.node.nameBn })

        // Kanchibari W2 & W3 -> কালিরখামার (multi-ward verified village without duplicate node)
        val kanchibariW2 = repo.resolveVillagesForSelection(
            mergedNodes = merged,
            unionId = "un_sundarganj_12_kanchibari",
            wardId = SundarganjGeoRepository.wardIdFor("12", 2)
        )
        val kanchibariW3 = repo.resolveVillagesForSelection(
            mergedNodes = merged,
            unionId = "un_sundarganj_12_kanchibari",
            wardId = SundarganjGeoRepository.wardIdFor("12", 3)
        )
        assertTrue(kanchibariW2.wardMappingVerified)
        assertTrue(kanchibariW3.wardMappingVerified)
        assertEquals(listOf("কালিরখামার"), kanchibariW2.wardMappedVillages.map { it.node.nameBn })
        assertEquals(listOf("কালিরখামার"), kanchibariW3.wardMappedVillages.map { it.node.nameBn })

        // Chandipur W8 -> সীচা
        val chandipurW8 = repo.resolveVillagesForSelection(
            mergedNodes = merged,
            unionId = "un_sundarganj_14_chandipur",
            wardId = SundarganjGeoRepository.wardIdFor("14", 8)
        )
        assertTrue(chandipurW8.wardMappingVerified)
        assertEquals(listOf("সীচা"), chandipurW8.wardMappedVillages.map { it.node.nameBn })

        // Belka W1 -> unverified ward mapping, falls back to 11 Belka union villages
        val belkaW1 = repo.resolveVillagesForSelection(
            mergedNodes = merged,
            unionId = "un_sundarganj_04_belka",
            wardId = SundarganjGeoRepository.wardIdFor("04", 1)
        )
        assertFalse(belkaW1.wardMappingVerified)
        assertTrue(belkaW1.wardMappedVillages.isEmpty())
        assertEquals(11, belkaW1.unionVillages.size)

        // Haripur W3 -> unverified ward mapping, falls back to 11 Haripur union villages
        val haripurW3 = repo.resolveVillagesForSelection(
            mergedNodes = merged,
            unionId = "un_sundarganj_11_haripur",
            wardId = SundarganjGeoRepository.wardIdFor("11", 3)
        )
        assertFalse(haripurW3.wardMappingVerified)
        assertTrue(haripurW3.wardMappedVillages.isEmpty())
        assertEquals(11, haripurW3.unionVillages.size)
    }

    @Test
    fun `bengali unicode normalization matches nukta variants and aliases`() {
        assertTrue(BengaliTextSearch.matches("সোনারায়", "সোনারায়"))
        assertTrue(BengaliTextSearch.matches("কঞ্চিবাড়ী", "কঞ্চিবাড়ী"))
        assertTrue(BengaliTextSearch.matches("বজরা হলদিয়া", "বজরা হলদিয়া"))
        assertTrue(BengaliTextSearch.matches("ছাপড়হাটী", "ছাপরহাটী", "ছাপড়হাটী"))
        assertTrue(BengaliTextSearch.matches("sundarganj", "সুন্দরগঞ্জ (Sundarganj)"))
    }

    @Test
    fun `firestore admin override updates ward assignment cleanly without duplicates`() {
        val repo = createRepo()
        val belkaWard3 = SundarganjGeoRepository.wardIdFor("04", 3)
        val firestoreOverride = LocationNode(
            id = "vil_04_ramdakua",
            level = LocationLevel.VILLAGE.name,
            parentId = belkaWard3,
            nameBn = "রামডাকুয়া",
            nameEn = "Ramdakua",
            code = "W-3"
        )
        val customVillage = LocationNode(
            id = "loc_custom_belka_new",
            level = LocationLevel.VILLAGE.name,
            parentId = belkaWard3,
            nameBn = "নতুন চর পাড়া",
            nameEn = "Natun Char Para",
            code = "W-3"
        )

        val merged = repo.mergeWithFirestore(listOf(firestoreOverride, customVillage))
        assertEquals(merged.size, merged.map { it.node.id }.toSet().size)

        val overridden = merged.first { it.node.id == "vil_04_ramdakua" }
        assertTrue(overridden.isOfficialReference)
        assertTrue(overridden.isAdminOverridden)
        assertEquals(GeoVerificationStatus.VERIFIED, overridden.effectiveStatus)
        assertEquals(listOf(belkaWard3), overridden.effectiveWardIds)

        val resolution = repo.resolveVillagesForSelection(merged, "un_sundarganj_04_belka", belkaWard3)
        assertTrue(resolution.wardMappingVerified)
        assertEquals(setOf("রামডাকুয়া", "নতুন চর পাড়া"), resolution.wardMappedVillages.map { it.node.nameBn }.toSet())
    }

    @Test
    fun `geography csv and json exports serialize all Sundarganj datasets safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())

        val csvFile = PdfAndExportEngine.exportGeographyCsv(context, merged).getOrThrow()
        val csvText = csvFile.readText(Charsets.UTF_8)
        assertTrue(csvText.contains("কঞ্চিবাড়ী"))
        assertTrue(csvText.contains("un_sundarganj_15_kapasia"))
        assertTrue(csvText.contains("ward_sundarganj_14_w09"))

        val jsonFile = PdfAndExportEngine.exportGeographyJson(context, repo.meta, merged).getOrThrow()
        val json = JSONObject(jsonFile.readText(Charsets.UTF_8))
        assertEquals("sundarganj_geo_v1", json.getString("datasetId"))
        assertEquals(148, json.getInt("upazilaVillageCountOfficial"))
        assertEquals(merged.size, json.getJSONArray("records").length())
    }

    @Test
    fun `data management screen enforces read only for normal users and admin controls for admins`() {
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())
        val baseNodes = merged.map { it.node }
        val summaries = repo.computeUnionSummaries(merged)

        val normalActor = PimsAccount(id = "u1", personalId = "BD-00010", role = PimsRole.NORMAL_USER.name)
        val adminActor = PimsAccount(id = "a1", personalId = "AD-00001", role = PimsRole.ADMIN.name)

        composeRule.setContent {
            ByaktigatoTheme {
                DataManagementScreen(
                    actor = normalActor,
                    locations = baseNodes,
                    mergedGeoNodes = merged,
                    unionGeoSummaries = summaries,
                    geoMeta = repo.meta,
                    institutions = emptyList(),
                    orgPositions = emptyList(),
                    lang = AppLang.BN,
                    onSaveLocation = {},
                    onDeleteLocation = {},
                    onImportLocationsCsv = {},
                    onSaveInstitution = {},
                    onDeleteInstitution = {},
                    onImportInstitutionsCsv = {},
                    onSaveOrgPosition = {},
                    onDeleteOrgPosition = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sundarganj_geo_summary_card").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("add_location_button").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("export_geo_csv_button").fetchSemanticsNodes().size)
    }

    @Test
    fun `data management screen renders admin add and export buttons for admin actor`() {
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())
        val baseNodes = merged.map { it.node }
        val summaries = repo.computeUnionSummaries(merged)
        val adminActor = PimsAccount(id = "a1", personalId = "AD-00001", role = PimsRole.ADMIN.name)

        composeRule.setContent {
            ByaktigatoTheme {
                DataManagementScreen(
                    actor = adminActor,
                    locations = baseNodes,
                    mergedGeoNodes = merged,
                    unionGeoSummaries = summaries,
                    geoMeta = repo.meta,
                    institutions = emptyList(),
                    orgPositions = emptyList(),
                    lang = AppLang.BN,
                    onSaveLocation = {},
                    onDeleteLocation = {},
                    onImportLocationsCsv = {},
                    onSaveInstitution = {},
                    onDeleteInstitution = {},
                    onImportInstitutionsCsv = {},
                    onSaveOrgPosition = {},
                    onDeleteOrgPosition = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("locations_lazy_list")
            .performScrollToNode(hasTestTag("add_location_button"))
        composeRule.onNodeWithTag("add_location_button").assertIsDisplayed()
        composeRule.onNodeWithTag("locations_lazy_list")
            .performScrollToNode(hasTestTag("export_geo_json_button"))
        composeRule.onNodeWithTag("export_geo_csv_button").assertIsDisplayed()
        composeRule.onNodeWithTag("export_geo_json_button").assertIsDisplayed()
    }

    @Test
    fun `user form displays awaiting verification notice for unverified ward and hides for verified ward`() {
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())
        val baseNodes = merged.map { it.node }
        val adminActor = PimsAccount(id = "a1", personalId = "AD-00001", role = PimsRole.ADMIN.name)

        val unverifiedAccount = PimsAccount(
            id = "u_belka",
            personalId = "BD-00050",
            fullNameBn = "মোঃ করিম উদ্দিন",
            divisionId = "div_rangpur",
            divisionName = "রংপুর",
            districtId = "dist_gaibandha",
            districtName = "গাইবান্ধা",
            upazilaId = "upz_sundarganj",
            upazilaName = "সুন্দরগঞ্জ",
            unionId = "un_sundarganj_04_belka",
            unionName = "বেলকা",
            wardId = SundarganjGeoRepository.wardIdFor("04", 1),
            wardName = "১ নং ওয়ার্ড"
        )

        composeRule.setContent {
            ByaktigatoTheme {
                UserFormScreen(
                    actor = adminActor,
                    existing = unverifiedAccount,
                    locations = baseNodes,
                    mergedGeoNodes = merged,
                    institutions = emptyList(),
                    orgPositions = emptyList(),
                    settings = WebsiteSettings(),
                    lang = AppLang.BN,
                    onGenerateId = { Result.success("BD-00051") },
                    onQuickAddInstitution = {},
                    onSave = { _, _, _ -> },
                    onCancel = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ward_mapping_unverified_notice").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("picker_village").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("selected_address_preview").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `user form hides awaiting verification notice for verified Sonaray ward`() {
        val repo = createRepo()
        val merged = repo.mergeWithFirestore(emptyList())
        val baseNodes = merged.map { it.node }
        val adminActor = PimsAccount(id = "a1", personalId = "AD-00001", role = PimsRole.ADMIN.name)

        val verifiedAccount = PimsAccount(
            id = "u_sonaray",
            personalId = "BD-00052",
            fullNameBn = "মোঃ রফিকুল ইসলাম",
            divisionId = "div_rangpur",
            divisionName = "রংপুর",
            districtId = "dist_gaibandha",
            districtName = "গাইবান্ধা",
            upazilaId = "upz_sundarganj",
            upazilaName = "সুন্দরগঞ্জ",
            unionId = "un_sundarganj_02_sonaray",
            unionName = "সোনারায়",
            wardId = SundarganjGeoRepository.wardIdFor("02", 1),
            wardName = "১ নং ওয়ার্ড",
            villageName = "চন্দ্র"
        )

        composeRule.setContent {
            ByaktigatoTheme {
                UserFormScreen(
                    actor = adminActor,
                    existing = verifiedAccount,
                    locations = baseNodes,
                    mergedGeoNodes = merged,
                    institutions = emptyList(),
                    orgPositions = emptyList(),
                    settings = WebsiteSettings(),
                    lang = AppLang.BN,
                    onGenerateId = { Result.success("BD-00053") },
                    onQuickAddInstitution = {},
                    onSave = { _, _, _ -> },
                    onCancel = {}
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("ward_mapping_unverified_notice").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("picker_village").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("selected_address_preview").performScrollTo().assertIsDisplayed()
    }
}
