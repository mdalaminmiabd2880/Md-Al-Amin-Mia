package com.example.ui.theme

import androidx.annotation.FontRes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val HindSiliguriFontFamily = FontFamily(
    Font(R.font.hind_siliguri, FontWeight.Normal),
    Font(R.font.hind_siliguri, FontWeight.Medium),
    Font(R.font.hind_siliguri, FontWeight.SemiBold),
    Font(R.font.hind_siliguri, FontWeight.Bold)
)

val NotoBengaliFontFamily = FontFamily(
    Font(R.font.noto_sans_bengali, FontWeight.Normal),
    Font(R.font.noto_sans_bengali, FontWeight.Medium),
    Font(R.font.noto_sans_bengali, FontWeight.SemiBold),
    Font(R.font.noto_sans_bengali, FontWeight.Bold)
)

val NotoSerifBengaliFontFamily = FontFamily(
    Font(R.font.noto_serif_bengali, FontWeight.Normal),
    Font(R.font.noto_serif_bengali, FontWeight.Medium),
    Font(R.font.noto_serif_bengali, FontWeight.SemiBold),
    Font(R.font.noto_serif_bengali, FontWeight.Bold)
)

val BalooDa2FontFamily = FontFamily(
    Font(R.font.baloo_da_2, FontWeight.Normal),
    Font(R.font.baloo_da_2, FontWeight.Medium),
    Font(R.font.baloo_da_2, FontWeight.SemiBold),
    Font(R.font.baloo_da_2, FontWeight.Bold)
)

val TiroBanglaFontFamily = FontFamily(
    Font(R.font.tiro_bangla, FontWeight.Normal),
    Font(R.font.tiro_bangla, FontWeight.Medium),
    Font(R.font.tiro_bangla, FontWeight.SemiBold),
    Font(R.font.tiro_bangla, FontWeight.Bold)
)

val MonoIdFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

enum class BengaliFontOption(
    @FontRes val fontResId: Int,
    val labelBn: String,
    val labelEn: String,
    val descriptionBn: String
) {
    HIND_SILIGURI(
        fontResId = R.font.hind_siliguri,
        labelBn = "হিন্দ শিলিগুড়ি",
        labelEn = "Hind Siliguri",
        descriptionBn = "আধুনিক ও স্পষ্ট UI বাংলা ফন্ট"
    ),
    NOTO_SANS_BENGALI(
        fontResId = R.font.noto_sans_bengali,
        labelBn = "নোটো স্যান্স বাংলা",
        labelEn = "Noto Sans Bengali",
        descriptionBn = "স্ট্যান্ডার্ড ও পরিচ্ছন্ন স্যান্স ফন্ট"
    ),
    NOTO_SERIF_BENGALI(
        fontResId = R.font.noto_serif_bengali,
        labelBn = "নোটো সেরিফ বাংলা",
        labelEn = "Noto Serif Bengali",
        descriptionBn = "দাপ্তরিক ও আনুষ্ঠানিক সেরিফ ফন্ট"
    ),
    BALOO_DA_2(
        fontResId = R.font.baloo_da_2,
        labelBn = "বালু দা ২",
        labelEn = "Baloo Da 2",
        descriptionBn = "বোল্ড ও গোলগাল ডিসপ্লে ফন্ট"
    ),
    TIRO_BANGLA(
        fontResId = R.font.tiro_bangla,
        labelBn = "তিরো বাংলা",
        labelEn = "Tiro Bangla",
        descriptionBn = "সুস্পষ্ট পড়ার উপযোগী সেরিফ ফন্ট"
    );

    fun fontFamily(): FontFamily = when (this) {
        HIND_SILIGURI -> HindSiliguriFontFamily
        NOTO_SANS_BENGALI -> NotoBengaliFontFamily
        NOTO_SERIF_BENGALI -> NotoSerifBengaliFontFamily
        BALOO_DA_2 -> BalooDa2FontFamily
        TIRO_BANGLA -> TiroBanglaFontFamily
    }

    companion object {
        fun fromRaw(raw: String?): BengaliFontOption =
            enumValues<BengaliFontOption>().firstOrNull { it.name == raw } ?: HIND_SILIGURI
    }
}

fun buildAppTypography(option: BengaliFontOption = BengaliFontOption.HIND_SILIGURI): Typography {
    val bnFamily = option.fontFamily()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 48.sp
        ),
        displayMedium = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        displaySmall = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp
        ),
        titleSmall = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bnFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    )
}

val AppTypography = buildAppTypography(BengaliFontOption.HIND_SILIGURI)
