package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.localization.AppLang
import com.example.core.localization.AppStrings
import com.example.core.security.CryptoSecurity
import com.example.data.geo.GeoDatasetMeta
import com.example.data.geo.MergedGeoNode
import com.example.data.model.PimsAccount
import com.example.data.model.WebsiteSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfFieldSelection(
    val includeBasic: Boolean = true,
    val includeEducation: Boolean = true,
    val includeGoals: Boolean = true,
    val includeOrganization: Boolean = true,
    val includeAddress: Boolean = true
)

object PdfAndExportEngine {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = (A4_WIDTH - 72)

    fun compressUriToBase64(context: Context, uri: Uri, maxDim: Int = 256, maxChars: Int = 140_000): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val w = (original.width * ratio).toInt().coerceAtLeast(32)
            val h = (original.height * ratio).toInt().coerceAtLeast(32)
            val scaled = Bitmap.createScaledBitmap(original, w, h, true)

            var quality = 80
            var encoded: String
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                quality -= 15
            } while (encoded.length > maxChars && quality >= 25)
            encoded
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64Bitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun resolveBengaliTypeface(
        context: Context,
        @androidx.annotation.FontRes bengaliFontResId: Int = R.font.hind_siliguri
    ): Typeface {
        return runCatching { ResourcesCompat.getFont(context, bengaliFontResId) }.getOrNull()
            ?: runCatching { ResourcesCompat.getFont(context, R.font.noto_sans_bengali) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    fun generateProfilesPdf(
        context: Context,
        accounts: List<PimsAccount>,
        settings: WebsiteSettings,
        selection: PdfFieldSelection,
        lang: AppLang,
        fileNamePrefix: String = "pims_profile",
        @androidx.annotation.FontRes bengaliFontResId: Int = R.font.hind_siliguri
    ): Result<File> {
        if (accounts.isEmpty()) {
            return Result.failure(
                IllegalArgumentException(
                    AppStrings.pick(
                        lang,
                        "PDF তৈরির জন্য কোনো প্রোফাইল পাওয়া যায়নি।",
                        "No profiles available to generate PDF."
                    )
                )
            )
        }
        val doc = PdfDocument()
        return try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(exportsDir, "${fileNamePrefix}_$stamp.pdf")

            val bengaliTypeface = resolveBengaliTypeface(context, bengaliFontResId)
            val primaryColor = parseHexOrDefault(settings.primaryColorHex, Color.rgb(13, 74, 62))
            val accentColor = parseHexOrDefault(settings.accentColorHex, Color.rgb(201, 150, 43))
            val logoBitmap = decodeBase64Bitmap(settings.logoBase64)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.brand_logo)

            val totalPages = accounts.size

            accounts.forEachIndexed { index, account ->
                val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, index + 1).create()
                val page = doc.startPage(pageInfo)
                drawAccountPage(
                    canvas = page.canvas,
                    account = account,
                    settings = settings,
                    selection = selection,
                    lang = lang,
                    typeface = bengaliTypeface,
                    primaryColor = primaryColor,
                    accentColor = accentColor,
                    logoBitmap = logoBitmap,
                    pageNumber = index + 1,
                    totalPages = totalPages
                )
                doc.finishPage(page)
            }

            FileOutputStream(outFile).use { fos ->
                doc.writeTo(fos)
            }
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            runCatching { doc.close() }
        }
    }

    private fun drawAccountPage(
        canvas: Canvas,
        account: PimsAccount,
        settings: WebsiteSettings,
        selection: PdfFieldSelection,
        lang: AppLang,
        typeface: Typeface,
        primaryColor: Int,
        accentColor: Int,
        logoBitmap: Bitmap?,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, A4_WIDTH.toFloat(), A4_HEIGHT.toFloat(), bgPaint)

        // Header Banner
        val headerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, A4_WIDTH.toFloat(), 82f, headerPaint)
        val accentPaint = Paint().apply { color = accentColor }
        canvas.drawRect(0f, 82f, A4_WIDTH.toFloat(), 86f, accentPaint)

        var titleStartX = MARGIN
        if (logoBitmap != null) {
            val rect = RectF(MARGIN, 16f, MARGIN + 48f, 64f)
            canvas.drawBitmap(logoBitmap, null, rect, null)
            titleStartX += 60f
        }

        val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.WHITE
            textSize = 16f
        }
        val headerSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.rgb(235, 245, 240)
            textSize = 10.5f
        }

        val appTitle = if (lang == AppLang.BN) settings.appNameBn else settings.appNameEn
        canvas.drawText(appTitle, titleStartX, 36f, headerTitlePaint)
        canvas.drawText(settings.headerText.ifBlank { "Personal Information Record" }, titleStartX, 56f, headerSubPaint)

        val pidBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.rgb(255, 232, 163)
            textSize = 11f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("ID: ${account.personalId}", A4_WIDTH - MARGIN, 46f, pidBadgePaint)

        var y = 104f

        // Profile Summary Row with Photo
        val photoBitmap = decodeBase64Bitmap(account.photoBase64)
        if (photoBitmap != null) {
            val photoRect = RectF(A4_WIDTH - MARGIN - 74f, y, A4_WIDTH - MARGIN, y + 74f)
            canvas.drawBitmap(photoBitmap, null, photoRect, null)
        } else {
            val boxPaint = Paint().apply {
                color = Color.rgb(242, 244, 243)
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(
                RectF(A4_WIDTH - MARGIN - 74f, y, A4_WIDTH - MARGIN, y + 74f),
                8f, 8f, boxPaint
            )
        }

        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = Color.rgb(20, 28, 25)
            textSize = 15f
        }
        val subNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.rgb(75, 88, 82)
            textSize = 11f
        }
        canvas.drawText(account.fullNameBn.ifBlank { account.fullNameEn }, MARGIN, y + 18f, namePaint)
        if (account.fullNameEn.isNotBlank()) {
            canvas.drawText(account.fullNameEn, MARGIN, y + 36f, subNamePaint)
        }
        val roleText = "${AppStrings.roleLabel(lang, account.role)} • ${account.personalId}"
        canvas.drawText(roleText, MARGIN, y + 54f, subNamePaint)
        if (account.bloodGroup.isNotBlank()) {
            canvas.drawText(
                "${AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group")}: ${account.bloodGroup}",
                MARGIN,
                y + 70f,
                subNamePaint
            )
        }

        y += 90f

        if (selection.includeBasic) {
            y = drawSection(
                canvas, y, typeface, primaryColor,
                AppStrings.pick(lang, "ক. মৌলিক তথ্য", "A. Basic Information"),
                listOf(
                    AppStrings.pick(lang, "পিতার নাম", "Father's Name") to account.fatherName,
                    AppStrings.pick(lang, "মাতার নাম", "Mother's Name") to account.motherName,
                    AppStrings.pick(lang, "জন্ম তারিখ", "Date of Birth") to account.dateOfBirth,
                    AppStrings.pick(lang, "লিঙ্গ", "Gender") to account.gender,
                    AppStrings.pick(lang, "রক্তের গ্রুপ", "Blood Group") to account.bloodGroup,
                    AppStrings.pick(lang, "মোবাইল নম্বর", "Mobile") to account.mobileNumber,
                    AppStrings.pick(lang, "ইমেইল", "Email") to account.emailAddress
                )
            )
        }

        if (selection.includeEducation && y < A4_HEIGHT - 120f) {
            y = drawSection(
                canvas, y, typeface, primaryColor,
                AppStrings.pick(lang, "খ. শিক্ষাগত তথ্য", "B. Educational Information"),
                listOf(
                    AppStrings.pick(lang, "শিক্ষা প্রতিষ্ঠান", "Institution") to account.institutionName,
                    AppStrings.pick(lang, "প্রতিষ্ঠানের ধরন", "Type") to if (account.institutionType.isNotBlank()) AppStrings.institutionTypeLabel(lang, account.institutionType) else "",
                    AppStrings.pick(lang, "শ্রেণি / বর্ষ", "Class / Year") to account.currentClassOrYear,
                    AppStrings.pick(lang, "যোগ্যতা", "Qualification") to account.educationalQualification,
                    AppStrings.pick(lang, "বিভাগ / বিষয়", "Department / Subject") to account.departmentOrSubject,
                    AppStrings.pick(lang, "একাডেমিক তথ্য", "Academic Notes") to account.academicInfo
                )
            )
        }

        if (selection.includeGoals && y < A4_HEIGHT - 120f) {
            y = drawSection(
                canvas, y, typeface, primaryColor,
                AppStrings.pick(lang, "গ. ব্যক্তিগত লক্ষ্য ও দক্ষতা", "C. Personal Goals & Skills"),
                listOf(
                    AppStrings.pick(lang, "ভবিষ্যৎ লক্ষ্য", "Career Goal") to account.careerGoal,
                    AppStrings.pick(lang, "দক্ষতা", "Skills") to account.skills,
                    AppStrings.pick(lang, "আগ্রহ", "Interests") to account.interests,
                    AppStrings.pick(lang, "পরিচিতি", "Introduction") to account.personalIntro
                )
            )
        }

        if (selection.includeOrganization && y < A4_HEIGHT - 120f) {
            y = drawSection(
                canvas, y, typeface, primaryColor,
                AppStrings.pick(lang, "ঘ. সাংগঠনিক তথ্য", "D. Organizational Information"),
                listOf(
                    AppStrings.pick(lang, "স্তর", "Org Level") to account.orgLevel,
                    AppStrings.pick(lang, "পদবী", "Designation") to account.orgDesignation,
                    AppStrings.pick(lang, "যোগদানের তারিখ", "Joining Date") to account.orgJoiningDate,
                    AppStrings.pick(lang, "অবস্থা", "Status") to account.orgStatus,
                    AppStrings.pick(lang, "অতিরিক্ত তথ্য", "Additional Info") to account.orgAdditionalInfo
                )
            )
        }

        if (selection.includeAddress && y < A4_HEIGHT - 100f) {
            y = drawSection(
                canvas, y, typeface, primaryColor,
                AppStrings.pick(lang, "ঙ. ঠিকানা", "E. Address"),
                listOf(
                    AppStrings.pick(lang, "বিভাগ / জেলা", "Division / District") to listOf(account.divisionName, account.districtName).filter { it.isNotBlank() }.joinToString(" • "),
                    AppStrings.pick(lang, "উপজেলা / ইউনিয়ন", "Upazila / Union") to listOf(account.upazilaName, account.unionName).filter { it.isNotBlank() }.joinToString(" • "),
                    AppStrings.pick(lang, "ওয়ার্ড / গ্রাম", "Ward / Village") to listOf(account.wardName, account.villageName).filter { it.isNotBlank() }.joinToString(" • ")
                )
            )
        }

        // Footer
        val linePaint = Paint().apply {
            color = Color.rgb(210, 218, 214)
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN, A4_HEIGHT - 44f, A4_WIDTH - MARGIN, A4_HEIGHT - 44f, linePaint)
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.rgb(100, 112, 107)
            textSize = 9.5f
        }
        canvas.drawText(settings.footerText, MARGIN, A4_HEIGHT - 26f, footerPaint)
        val pageLabel = if (lang == AppLang.BN) "পৃষ্ঠা $pageNumber / $totalPages" else "Page $pageNumber / $totalPages"
        val rightFooterPaint = TextPaint(footerPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(pageLabel, A4_WIDTH - MARGIN, A4_HEIGHT - 26f, rightFooterPaint)
    }

    private fun drawSection(
        canvas: Canvas,
        startY: Float,
        typeface: Typeface,
        primaryColor: Int,
        sectionTitle: String,
        rows: List<Pair<String, String>>
    ): Float {
        val visibleRows = rows.filter { it.second.isNotBlank() }
        if (visibleRows.isEmpty()) return startY

        var y = startY
        val titleBg = Paint().apply { color = Color.rgb(236, 245, 241) }
        canvas.drawRoundRect(RectF(MARGIN, y, A4_WIDTH - MARGIN, y + 20f), 4f, 4f, titleBg)

        val secTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            color = primaryColor
            textSize = 11f
        }
        canvas.drawText(sectionTitle, MARGIN + 8f, y + 14f, secTitlePaint)
        y += 26f

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            color = Color.rgb(34, 42, 39)
            textSize = 10f
        }

        visibleRows.forEach { (label, value) ->
            if (y > A4_HEIGHT - 64f) return y
            val lineText = "$label: $value"
            val layout = StaticLayout.Builder.obtain(lineText, 0, lineText.length, textPaint, CONTENT_WIDTH - 12)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1f, 1.1f)
                .build()
            canvas.save()
            canvas.translate(MARGIN + 6f, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 4f
        }
        return y + 6f
    }

    fun printPdfFile(context: Context, pdfFile: File, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(pdfFile.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        try {
            printManager.print(
                jobName,
                adapter,
                PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
            )
        } catch (_: Exception) {
            // Print service unavailable on headless/restricted device
        }
    }

    fun exportAccountsCsv(context: Context, accounts: List<PimsAccount>): Result<File> {
        return try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(exportsDir, "pims_export_$stamp.csv")
            val header = listOf(
                "PersonalID", "Role", "Active", "FullNameBn", "FullNameEn",
                "FatherName", "MotherName", "DateOfBirth", "Gender", "BloodGroup",
                "Mobile", "Email", "Institution", "ClassOrYear", "Qualification",
                "Department", "OrgLevel", "OrgDesignation", "Division", "District",
                "Upazila", "Union", "Ward", "Village"
            ).joinToString(",")

            val sb = StringBuilder("\uFEFF").appendLine(header) // UTF-8 BOM for Bengali Excel/Sheets compatibility
            accounts.forEach { a ->
                val row = listOf(
                    a.personalId, a.role, a.active.toString(), a.fullNameBn, a.fullNameEn,
                    a.fatherName, a.motherName, a.dateOfBirth, a.gender, a.bloodGroup,
                    a.mobileNumber, a.emailAddress, a.institutionName, a.currentClassOrYear,
                    a.educationalQualification, a.departmentOrSubject, a.orgLevel, a.orgDesignation,
                    a.divisionName, a.districtName, a.upazilaName, a.unionName, a.wardName, a.villageName
                ).joinToString(",") { CryptoSecurity.sanitizeCsvCell(it) }
                sb.appendLine(row)
            }
            outFile.writeText(sb.toString(), Charsets.UTF_8)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Structured JSON backup excluding all password hashes, salts, and lockout metadata.
     */
    fun exportStructuredBackupJson(
        context: Context,
        accounts: List<PimsAccount>,
        settings: WebsiteSettings
    ): Result<File> {
        return try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(exportsDir, "pims_backup_$stamp.json")

            val accountsArray = JSONArray()
            accounts.forEach { a ->
                accountsArray.put(JSONObject().apply {
                    put("personalId", a.personalId)
                    put("role", a.role)
                    put("active", a.active)
                    put("fullNameBn", a.fullNameBn)
                    put("fullNameEn", a.fullNameEn)
                    put("fatherName", a.fatherName)
                    put("motherName", a.motherName)
                    put("dateOfBirth", a.dateOfBirth)
                    put("gender", a.gender)
                    put("bloodGroup", a.bloodGroup)
                    put("mobileNumber", a.mobileNumber)
                    put("emailAddress", a.emailAddress)
                    put("institutionName", a.institutionName)
                    put("institutionType", a.institutionType)
                    put("currentClassOrYear", a.currentClassOrYear)
                    put("educationalQualification", a.educationalQualification)
                    put("departmentOrSubject", a.departmentOrSubject)
                    put("academicInfo", a.academicInfo)
                    put("careerGoal", a.careerGoal)
                    put("personalIntro", a.personalIntro)
                    put("skills", a.skills)
                    put("interests", a.interests)
                    put("orgLevel", a.orgLevel)
                    put("orgDesignation", a.orgDesignation)
                    put("orgJoiningDate", a.orgJoiningDate)
                    put("orgStatus", a.orgStatus)
                    put("address", a.fullAddressLabel())
                })
            }

            val root = JSONObject().apply {
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                put("appNameBn", settings.appNameBn)
                put("appNameEn", settings.appNameEn)
                put("totalAccounts", accounts.size)
                put("accounts", accountsArray)
            }
            outFile.writeText(root.toString(2), Charsets.UTF_8)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportGeographyCsv(
        context: Context,
        nodes: List<MergedGeoNode>
    ): Result<File> {
        return try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(exportsDir, "sundarganj_geo_$stamp.csv")
            val header = listOf(
                "ID", "Level", "ParentID", "UnionID", "WardIDs", "NameBn", "NameEn",
                "Code", "VerificationStatus", "OfficialReference", "AdminOverridden",
                "SourceUrl", "LastVerifiedAt", "NotesBn"
            ).joinToString(",")
            val sb = StringBuilder("\uFEFF").appendLine(header)
            nodes.forEach { m ->
                val n = m.node
                val row = listOf(
                    n.id,
                    n.level,
                    n.parentId,
                    m.unionId,
                    m.effectiveWardIds.joinToString(";"),
                    n.nameBn,
                    n.nameEn,
                    n.code,
                    m.effectiveStatus.name,
                    m.isOfficialReference.toString(),
                    m.isAdminOverridden.toString(),
                    m.sourceUrl,
                    m.lastVerifiedAt,
                    m.noteBn
                ).joinToString(",") { CryptoSecurity.sanitizeCsvCell(it) }
                sb.appendLine(row)
            }
            outFile.writeText(sb.toString(), Charsets.UTF_8)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportGeographyJson(
        context: Context,
        meta: GeoDatasetMeta,
        nodes: List<MergedGeoNode>
    ): Result<File> {
        return try {
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(exportsDir, "sundarganj_geo_$stamp.json")

            val recordsArray = JSONArray()
            nodes.forEach { m ->
                val n = m.node
                recordsArray.put(JSONObject().apply {
                    put("id", n.id)
                    put("level", n.level)
                    put("parentId", n.parentId)
                    put("unionId", m.unionId)
                    put("effectiveWardIds", JSONArray(m.effectiveWardIds))
                    put("nameBn", n.nameBn)
                    put("nameEn", n.nameEn)
                    put("code", n.code)
                    put("verificationStatus", m.effectiveStatus.name)
                    put("isOfficialReference", m.isOfficialReference)
                    put("isAdminOverridden", m.isAdminOverridden)
                    put("sourceUrl", m.sourceUrl)
                    put("lastVerifiedAt", m.lastVerifiedAt)
                    put("noteBn", m.noteBn)
                    put("noteEn", m.noteEn)
                })
            }

            val root = JSONObject().apply {
                put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
                put("datasetId", meta.datasetId)
                put("version", meta.version)
                put("upazilaVillageCountOfficial", meta.upazilaVillageCountOfficial)
                put("totalRecords", nodes.size)
                put("discrepancyAnalysisBn", meta.discrepancyAnalysisBn)
                put("discrepancyAnalysisEn", meta.discrepancyAnalysisEn)
                put("records", recordsArray)
            }
            outFile.writeText(root.toString(2), Charsets.UTF_8)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareExportFile(context: Context, file: File, mimeType: String, chooserTitle: String): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                if (context !is android.app.Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun parseHexOrDefault(hex: String, fallback: Int): Int = try {
        Color.parseColor(hex)
    } catch (_: Exception) {
        fallback
    }
}
