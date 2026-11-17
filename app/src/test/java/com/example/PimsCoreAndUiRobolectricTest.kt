package com.example

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.example.core.localization.AppLang
import com.example.core.security.CryptoSecurity
import com.example.data.export.PdfAndExportEngine
import com.example.data.export.PdfFieldSelection
import com.example.data.model.AuthPermissionDiagnostics
import com.example.data.model.DiagnosticCheckStatus
import com.example.data.model.LocationLevel
import com.example.data.model.PimsAccount
import com.example.data.model.PimsRole
import com.example.data.model.WebsiteSettings
import com.example.data.seed.BangladeshGeoSeed
import com.example.ui.components.AuthPermissionDiagnosticCard
import com.example.ui.components.BengaliFontSelectorCard
import com.example.ui.theme.BengaliFontOption
import com.example.ui.theme.ByaktigatoTheme
import com.example.ui.theme.buildAppTypography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PimsCoreAndUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `bangladesh geo seed has exact 8 divisions and 64 districts with valid parents`() {
        val divisions = BangladeshGeoSeed.divisionsAndDistricts.filter { it.level == LocationLevel.DIVISION }
        val districts = BangladeshGeoSeed.divisionsAndDistricts.filter { it.level == LocationLevel.DISTRICT }
        val divisionIds = divisions.map { it.id }.toSet()

        assertEquals(8, divisions.size)
        assertEquals(64, districts.size)
        assertEquals(72, BangladeshGeoSeed.divisionsAndDistricts.map { it.id }.toSet().size)
        assertTrue(districts.all { it.parentId in divisionIds })
        assertTrue(BangladeshGeoSeed.defaultOrgPositions.isNotEmpty())
    }

    @Test
    fun `crypto security hashing aes gcm and csv sanitization work reliably`() {
        val hashed = CryptoSecurity.hashPassword("SecurePass123")
        assertTrue(CryptoSecurity.verifyPassword("SecurePass123", hashed.hashBase64, hashed.saltBase64))
        assertFalse(CryptoSecurity.verifyPassword("WrongPass", hashed.hashBase64, hashed.saltBase64))

        val enc = CryptoSecurity.encryptAesGcm("গোপন তথ্য-৪২", "MyVaultKey#99")
        val decOk = CryptoSecurity.decryptAesGcm(enc.ciphertextBase64, enc.ivBase64, enc.saltBase64, "MyVaultKey#99")
        assertEquals("গোপন তথ্য-৪২", decOk.getOrThrow())

        val decFail = CryptoSecurity.decryptAesGcm(enc.ciphertextBase64, enc.ivBase64, enc.saltBase64, "WrongKey")
        assertTrue(decFail.isFailure)

        assertEquals("\"'=cmd|' /C calc'!A0\"", CryptoSecurity.sanitizeCsvCell("=cmd|' /C calc'!A0"))
    }

    @Test
    fun `rbac permission hierarchy enforces strict isolation`() {
        val superAdmin = PimsAccount(id = "1", personalId = "SA-00001", role = PimsRole.SUPER_ADMIN.name)
        val admin = PimsAccount(id = "2", personalId = "AD-00002", role = PimsRole.ADMIN.name)
        val normal = PimsAccount(id = "3", personalId = "BD-00003", role = PimsRole.NORMAL_USER.name, canEditSelfProfile = false)
        val editableNormal = normal.copy(canEditSelfProfile = true)

        assertTrue(PimsRole.SUPER_ADMIN.canEditAccount(admin, superAdmin.personalId, emptyList()))
        assertFalse(PimsRole.ADMIN.canEditAccount(superAdmin, admin.personalId, emptyList()))
        assertTrue(PimsRole.NORMAL_USER.canViewAccount(normal, normal.personalId, emptyList()))
        assertFalse(PimsRole.NORMAL_USER.canViewAccount(admin, normal.personalId, emptyList()))
        assertFalse(PimsRole.NORMAL_USER.canEditAccount(normal, normal.personalId, emptyList()))
        assertTrue(PimsRole.NORMAL_USER.canEditAccount(editableNormal, editableNormal.personalId, emptyList()))
    }

    @Test
    fun `pdf and backup exports guard empty list and strip credential hashes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = WebsiteSettings()

        val emptyPdf = PdfAndExportEngine.generateProfilesPdf(
            context = context,
            accounts = emptyList(),
            settings = settings,
            selection = PdfFieldSelection(),
            lang = AppLang.BN
        )
        assertTrue(emptyPdf.isFailure)

        val account = PimsAccount(
            id = "acc_test1",
            personalId = "SA-00001",
            passwordHash = "SECRET_HASH_SHOULD_NOT_EXPORT",
            passwordSalt = "SECRET_SALT_SHOULD_NOT_EXPORT",
            fullNameBn = "মোঃ আলামিন মিয়া",
            fullNameEn = "Md Alamin Mia",
            bloodGroup = "O+",
            divisionName = "রংপুর",
            districtName = "গাইবান্ধা"
        )
        val csvBackup = PdfAndExportEngine.exportAccountsCsv(context, listOf(account)).getOrThrow()
        val csvText = csvBackup.readText()
        assertTrue(csvText.contains("SA-00001"))
        assertTrue(csvText.contains("মোঃ আলামিন মিয়া"))
        assertFalse(csvText.contains("SECRET_HASH_SHOULD_NOT_EXPORT"))

        val jsonBackup = PdfAndExportEngine.exportStructuredBackupJson(context, listOf(account), settings).getOrThrow()
        val jsonText = jsonBackup.readText()
        assertTrue(jsonText.contains("SA-00001"))
        assertFalse(jsonText.contains("SECRET_HASH_SHOULD_NOT_EXPORT"))
        assertFalse(jsonText.contains("SECRET_SALT_SHOULD_NOT_EXPORT"))
    }

    @Test
    fun `app resources contain required firebase config logo and bengali font`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
        assertNotNull(ResourcesCompat.getDrawable(context.resources, R.drawable.brand_logo, null))
        assertNotNull(ResourcesCompat.getFont(context, R.font.noto_sans_bengali))
    }

    @Test
    fun `diagnostic card renders all 10 read-only checks accurately`() {
        val sampleDiag = AuthPermissionDiagnostics(
            isLoading = false,
            googleEmail = "user@example.com",
            firebaseUid = "uid_test_123",
            emailVerified = true,
            isFirebaseSignedIn = true,
            authProviders = listOf("google.com"),
            emailMatchesBootstrappedAdmin = false,
            effectiveUserRole = "Normal User",
            effectiveUserRoleBn = "সাধারণ ব্যবহারকারী (Normal User)",
            adminPermissionGranted = false,
            adminsDocExists = false,
            bootstrapStatus = DiagnosticCheckStatus.FAILED,
            bootstrapDetailBn = "অনুমতি নাকচ হয়েছে",
            lastFirestoreErrorCode = "PERMISSION_DENIED",
            lastFirestoreErrorReasonBn = "/admins/uid_test_123 অনুপস্থিত।"
        )

        composeRule.setContent {
            ByaktigatoTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AuthPermissionDiagnosticCard(
                        diagnostics = sampleDiag,
                        lang = AppLang.BN,
                        onRefresh = {},
                        testTagPrefix = "test_diag"
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val tags = listOf(
            "test_diag_card",
            "test_diag_email",
            "test_diag_uid",
            "test_diag_email_verified",
            "test_diag_auth_status",
            "test_diag_bootstrapped_match",
            "test_diag_role",
            "test_diag_admin_permission",
            "test_diag_admins_doc",
            "test_diag_bootstrap_status",
            "test_diag_last_error"
        )
        tags.forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun `main activity startup and bundled images render without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        listOf(
            R.drawable.brand_logo,
            R.drawable.img_auth_hero,
            R.drawable.img_avatar_default,
            R.drawable.pims_launcher_art
        ).forEach { resId ->
            val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
            assertNotNull("Drawable $resId failed to decode", bmp)
            assertTrue("Drawable $resId decoded empty bitmap", bmp.width > 0 && bmp.height > 0)
        }
        assertNotNull(ResourcesCompat.getFont(context, R.font.jetbrains_mono))
        assertNotNull(ResourcesCompat.getFont(context, R.font.plus_jakarta_sans))

        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertTrue(com.google.firebase.FirebaseApp.getApps(context).isNotEmpty())
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
        controller.pause().stop().destroy()
    }

    @Test
    fun `all five bengali fonts and 15 typography slots load cleanly and pdf uses selected font`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BengaliFontOption.entries.forEach { option ->
            val typeface = ResourcesCompat.getFont(context, option.fontResId)
            assertNotNull("Font resource missing for ${option.name}", typeface)

            val typography = buildAppTypography(option)
            val allStyles = listOf(
                typography.displayLarge,
                typography.displayMedium,
                typography.displaySmall,
                typography.headlineLarge,
                typography.headlineMedium,
                typography.headlineSmall,
                typography.titleLarge,
                typography.titleMedium,
                typography.titleSmall,
                typography.bodyLarge,
                typography.bodyMedium,
                typography.bodySmall,
                typography.labelLarge,
                typography.labelMedium,
                typography.labelSmall
            )
            assertTrue("All 15 M3 styles must set fontFamily for ${option.name}", allStyles.all { it.fontFamily != null })
        }

        assertEquals(BengaliFontOption.NOTO_SERIF_BENGALI, BengaliFontOption.fromRaw("NOTO_SERIF_BENGALI"))
        assertEquals(BengaliFontOption.HIND_SILIGURI, BengaliFontOption.fromRaw("UNKNOWN_FONT"))

        // Verify PdfAndExportEngine resolves custom Bengali Typeface for every font option
        // and gracefully falls back to bundled Noto Sans Bengali if given an invalid resource id.
        BengaliFontOption.entries.forEach { option ->
            val resolved = PdfAndExportEngine.resolveBengaliTypeface(context, option.fontResId)
            assertNotNull("PDF typeface missing for ${option.name}", resolved)
            assertFalse("PDF typeface should not fall back to Typeface.DEFAULT for ${option.name}", resolved == Typeface.DEFAULT)
        }
        val fallbackResolved = PdfAndExportEngine.resolveBengaliTypeface(context, 0)
        assertNotNull(fallbackResolved)
    }

    @Test
    fun `bengali font selector card displays all options and notifies selection`() {
        var selected = BengaliFontOption.HIND_SILIGURI
        composeRule.setContent {
            ByaktigatoTheme(bengaliFont = selected) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    BengaliFontSelectorCard(
                        selectedFont = selected,
                        lang = AppLang.BN,
                        onSelectFont = { selected = it }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("bengali_font_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("bengali_font_preview").performScrollTo().assertIsDisplayed()
        BengaliFontOption.entries.forEach { option ->
            composeRule.onNodeWithTag("bengali_font_${option.name.lowercase()}")
                .performScrollTo()
                .assertIsDisplayed()
        }

        composeRule.onNodeWithTag("bengali_font_tiro_bangla").performScrollTo().performClick()
        assertEquals(BengaliFontOption.TIRO_BANGLA, selected)
    }
}
