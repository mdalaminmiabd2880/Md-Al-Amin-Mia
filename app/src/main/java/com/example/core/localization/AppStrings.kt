package com.example.core.localization

enum class AppLang { BN, EN }

object AppStrings {
    fun pick(lang: AppLang, bn: String, en: String): String =
        if (lang == AppLang.BN) bn else en

    fun roleLabel(lang: AppLang, roleName: String): String = when (roleName) {
        "SUPER_ADMIN" -> pick(lang, "সুপার অ্যাডমিন", "Super Admin")
        "ADMIN" -> pick(lang, "অ্যাডমিন", "Admin")
        "EDITOR" -> pick(lang, "এডিটর", "Editor")
        "VIEWER" -> pick(lang, "ভিউয়ার", "Viewer")
        else -> pick(lang, "সাধারণ ব্যবহারকারী", "Normal User")
    }

    fun locationLevelLabel(lang: AppLang, level: String): String = when (level) {
        "DIVISION" -> pick(lang, "বিভাগ", "Division")
        "DISTRICT" -> pick(lang, "জেলা", "District")
        "UPAZILA" -> pick(lang, "উপজেলা / থানা", "Upazila")
        "UNION" -> pick(lang, "ইউনিয়ন / পৌরসভা", "Union")
        "WARD" -> pick(lang, "ওয়ার্ড", "Ward")
        "VILLAGE" -> pick(lang, "গ্রাম / এলাকা", "Village / Area")
        else -> level
    }

    fun institutionTypeLabel(lang: AppLang, type: String): String = when (type) {
        "SCHOOL" -> pick(lang, "স্কুল", "School")
        "COLLEGE" -> pick(lang, "কলেজ", "College")
        "MADRASA" -> pick(lang, "মাদ্রাসা", "Madrasa")
        "UNIVERSITY" -> pick(lang, "বিশ্ববিদ্যালয়", "University")
        "TECHNICAL" -> pick(lang, "কারিগরি প্রতিষ্ঠান", "Technical Institute")
        else -> pick(lang, "অন্যান্য", "Other")
    }

    fun vaultCategoryLabel(lang: AppLang, category: String): String = when (category) {
        "CREDENTIAL" -> pick(lang, "লগইন তথ্য / পাসওয়ার্ড", "Credentials")
        "DOCUMENT" -> pick(lang, "নথি / ডকুমেন্ট", "Document Info")
        else -> pick(lang, "ব্যক্তিগত নোট", "Private Note")
    }
}
