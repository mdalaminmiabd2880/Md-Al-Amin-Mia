package com.example.data.seed

import com.example.data.model.LocationLevel

data class SeedLocation(
    val id: String,
    val level: LocationLevel,
    val parentId: String,
    val nameBn: String,
    val nameEn: String,
    val code: String
)

data class SeedOrgPosition(
    val id: String,
    val levelNameBn: String,
    val levelNameEn: String,
    val designationBn: String,
    val designationEn: String,
    val rankOrder: Long
)

/**
 * Verified official Bangladesh Administrative Geography:
 * All 8 Divisions and all 64 Districts with BBS-aligned names in Bengali & English.
 * Sub-district levels (Upazila, Union, Ward, Village) are intentionally NOT fabricated
 * and can be added manually or imported via CSV by authorized administrators.
 */
object BangladeshGeoSeed {
    val divisionsAndDistricts: List<SeedLocation> = listOf(
        // 8 Divisions
        SeedLocation("div_dhaka", LocationLevel.DIVISION, "", "ঢাকা", "Dhaka", "30"),
        SeedLocation("div_chattogram", LocationLevel.DIVISION, "", "চট্টগ্রাম", "Chattogram", "20"),
        SeedLocation("div_rajshahi", LocationLevel.DIVISION, "", "রাজশাহী", "Rajshahi", "50"),
        SeedLocation("div_khulna", LocationLevel.DIVISION, "", "খুলনা", "Khulna", "40"),
        SeedLocation("div_barishal", LocationLevel.DIVISION, "", "বরিশাল", "Barishal", "10"),
        SeedLocation("div_sylhet", LocationLevel.DIVISION, "", "সিলেট", "Sylhet", "60"),
        SeedLocation("div_rangpur", LocationLevel.DIVISION, "", "রংপুর", "Rangpur", "55"),
        SeedLocation("div_mymensingh", LocationLevel.DIVISION, "", "ময়মনসিংহ", "Mymensingh", "45"),

        // Dhaka Division (13 Districts)
        SeedLocation("dist_dhaka", LocationLevel.DISTRICT, "div_dhaka", "ঢাকা", "Dhaka", "26"),
        SeedLocation("dist_faridpur", LocationLevel.DISTRICT, "div_dhaka", "ফরিদপুর", "Faridpur", "29"),
        SeedLocation("dist_gazipur", LocationLevel.DISTRICT, "div_dhaka", "গাজীপুর", "Gazipur", "33"),
        SeedLocation("dist_gopalganj", LocationLevel.DISTRICT, "div_dhaka", "গোপালগঞ্জ", "Gopalganj", "35"),
        SeedLocation("dist_kishoreganj", LocationLevel.DISTRICT, "div_dhaka", "কিশোরগঞ্জ", "Kishoreganj", "48"),
        SeedLocation("dist_madaripur", LocationLevel.DISTRICT, "div_dhaka", "মাদারীপুর", "Madaripur", "54"),
        SeedLocation("dist_manikganj", LocationLevel.DISTRICT, "div_dhaka", "মানিকগঞ্জ", "Manikganj", "56"),
        SeedLocation("dist_munshiganj", LocationLevel.DISTRICT, "div_dhaka", "মুন্সিগঞ্জ", "Munshiganj", "59"),
        SeedLocation("dist_narayanganj", LocationLevel.DISTRICT, "div_dhaka", "নারায়ণগঞ্জ", "Narayanganj", "67"),
        SeedLocation("dist_narsingdi", LocationLevel.DISTRICT, "div_dhaka", "নরসিংদী", "Narsingdi", "68"),
        SeedLocation("dist_rajbari", LocationLevel.DISTRICT, "div_dhaka", "রাজবাড়ী", "Rajbari", "82"),
        SeedLocation("dist_shariatpur", LocationLevel.DISTRICT, "div_dhaka", "শরীয়তপুর", "Shariatpur", "86"),
        SeedLocation("dist_tangail", LocationLevel.DISTRICT, "div_dhaka", "টাঙ্গাইল", "Tangail", "93"),

        // Chattogram Division (11 Districts)
        SeedLocation("dist_bandarban", LocationLevel.DISTRICT, "div_chattogram", "বান্দরবান", "Bandarban", "03"),
        SeedLocation("dist_brahmanbaria", LocationLevel.DISTRICT, "div_chattogram", "ব্রাহ্মণবাড়িয়া", "Brahmanbaria", "12"),
        SeedLocation("dist_chandpur", LocationLevel.DISTRICT, "div_chattogram", "চাঁদপুর", "Chandpur", "13"),
        SeedLocation("dist_chattogram", LocationLevel.DISTRICT, "div_chattogram", "চট্টগ্রাম", "Chattogram", "15"),
        SeedLocation("dist_cumilla", LocationLevel.DISTRICT, "div_chattogram", "কুমিল্লা", "Cumilla", "19"),
        SeedLocation("dist_coxsbazar", LocationLevel.DISTRICT, "div_chattogram", "কক্সবাজার", "Cox's Bazar", "22"),
        SeedLocation("dist_feni", LocationLevel.DISTRICT, "div_chattogram", "ফেনী", "Feni", "30"),
        SeedLocation("dist_khagrachhari", LocationLevel.DISTRICT, "div_chattogram", "খাগড়াছড়ি", "Khagrachhari", "46"),
        SeedLocation("dist_lakshmipur", LocationLevel.DISTRICT, "div_chattogram", "লক্ষ্মীপুর", "Lakshmipur", "51"),
        SeedLocation("dist_noakhali", LocationLevel.DISTRICT, "div_chattogram", "নোয়াখালী", "Noakhali", "75"),
        SeedLocation("dist_rangamati", LocationLevel.DISTRICT, "div_chattogram", "রাঙ্গামাটি", "Rangamati", "84"),

        // Rajshahi Division (8 Districts)
        SeedLocation("dist_bogura", LocationLevel.DISTRICT, "div_rajshahi", "বগুড়া", "Bogura", "10"),
        SeedLocation("dist_chapainawabganj", LocationLevel.DISTRICT, "div_rajshahi", "চাঁপাইনবাবগঞ্জ", "Chapainawabganj", "70"),
        SeedLocation("dist_joypurhat", LocationLevel.DISTRICT, "div_rajshahi", "জয়পুরহাট", "Joypurhat", "38"),
        SeedLocation("dist_naogaon", LocationLevel.DISTRICT, "div_rajshahi", "নওগাঁ", "Naogaon", "64"),
        SeedLocation("dist_natore", LocationLevel.DISTRICT, "div_rajshahi", "নাটোর", "Natore", "69"),
        SeedLocation("dist_pabna", LocationLevel.DISTRICT, "div_rajshahi", "পাবনা", "Pabna", "76"),
        SeedLocation("dist_rajshahi", LocationLevel.DISTRICT, "div_rajshahi", "রাজশাহী", "Rajshahi", "81"),
        SeedLocation("dist_sirajganj", LocationLevel.DISTRICT, "div_rajshahi", "সিরাজগঞ্জ", "Sirajganj", "88"),

        // Khulna Division (10 Districts)
        SeedLocation("dist_bagerhat", LocationLevel.DISTRICT, "div_khulna", "বাগেরহাট", "Bagerhat", "01"),
        SeedLocation("dist_chuadanga", LocationLevel.DISTRICT, "div_khulna", "চুয়াডাঙ্গা", "Chuadanga", "18"),
        SeedLocation("dist_jashore", LocationLevel.DISTRICT, "div_khulna", "যশোর", "Jashore", "41"),
        SeedLocation("dist_jhenaidah", LocationLevel.DISTRICT, "div_khulna", "ঝিনাইদহ", "Jhenaidah", "44"),
        SeedLocation("dist_khulna", LocationLevel.DISTRICT, "div_khulna", "খুলনা", "Khulna", "47"),
        SeedLocation("dist_kushtia", LocationLevel.DISTRICT, "div_khulna", "কুষ্টিয়া", "Kushtia", "50"),
        SeedLocation("dist_magura", LocationLevel.DISTRICT, "div_khulna", "মাগুরা", "Magura", "55"),
        SeedLocation("dist_meherpur", LocationLevel.DISTRICT, "div_khulna", "মেহেরপুর", "Meherpur", "57"),
        SeedLocation("dist_narail", LocationLevel.DISTRICT, "div_khulna", "নড়াইল", "Narail", "65"),
        SeedLocation("dist_satkhira", LocationLevel.DISTRICT, "div_khulna", "সাতক্ষীরা", "Satkhira", "87"),

        // Barishal Division (6 Districts)
        SeedLocation("dist_barguna", LocationLevel.DISTRICT, "div_barishal", "বরগুনা", "Barguna", "04"),
        SeedLocation("dist_barishal", LocationLevel.DISTRICT, "div_barishal", "বরিশাল", "Barishal", "06"),
        SeedLocation("dist_bhola", LocationLevel.DISTRICT, "div_barishal", "ভোলা", "Bhola", "09"),
        SeedLocation("dist_jhalokathi", LocationLevel.DISTRICT, "div_barishal", "ঝালকাঠি", "Jhalokathi", "42"),
        SeedLocation("dist_patuakhali", LocationLevel.DISTRICT, "div_barishal", "পটুয়াখালী", "Patuakhali", "78"),
        SeedLocation("dist_pirojpur", LocationLevel.DISTRICT, "div_barishal", "পিরোজপুর", "Pirojpur", "79"),

        // Sylhet Division (4 Districts)
        SeedLocation("dist_habiganj", LocationLevel.DISTRICT, "div_sylhet", "হবিগঞ্জ", "Habiganj", "36"),
        SeedLocation("dist_moulvibazar", LocationLevel.DISTRICT, "div_sylhet", "মৌলভীবাজার", "Moulvibazar", "58"),
        SeedLocation("dist_sunamganj", LocationLevel.DISTRICT, "div_sylhet", "সুনামগঞ্জ", "Sunamganj", "90"),
        SeedLocation("dist_sylhet", LocationLevel.DISTRICT, "div_sylhet", "সিলেট", "Sylhet", "91"),

        // Rangpur Division (8 Districts)
        SeedLocation("dist_dinajpur", LocationLevel.DISTRICT, "div_rangpur", "দিনাজপুর", "Dinajpur", "27"),
        SeedLocation("dist_gaibandha", LocationLevel.DISTRICT, "div_rangpur", "গাইবান্ধা", "Gaibandha", "32"),
        SeedLocation("dist_kurigram", LocationLevel.DISTRICT, "div_rangpur", "কুড়িগ্রাম", "Kurigram", "49"),
        SeedLocation("dist_lalmonirhat", LocationLevel.DISTRICT, "div_rangpur", "লালমনিরহাট", "Lalmonirhat", "52"),
        SeedLocation("dist_nilphamari", LocationLevel.DISTRICT, "div_rangpur", "নীলফামারী", "Nilphamari", "73"),
        SeedLocation("dist_panchagarh", LocationLevel.DISTRICT, "div_rangpur", "পঞ্চগড়", "Panchagarh", "77"),
        SeedLocation("dist_rangpur", LocationLevel.DISTRICT, "div_rangpur", "রংপুর", "Rangpur", "85"),
        SeedLocation("dist_thakurgaon", LocationLevel.DISTRICT, "div_rangpur", "ঠাকুরগাঁও", "Thakurgaon", "94"),

        // Mymensingh Division (4 Districts)
        SeedLocation("dist_jamalpur", LocationLevel.DISTRICT, "div_mymensingh", "জামালপুর", "Jamalpur", "39"),
        SeedLocation("dist_mymensingh", LocationLevel.DISTRICT, "div_mymensingh", "ময়মনসিংহ", "Mymensingh", "61"),
        SeedLocation("dist_netrokona", LocationLevel.DISTRICT, "div_mymensingh", "নেত্রকোণা", "Netrokona", "72"),
        SeedLocation("dist_sherpur", LocationLevel.DISTRICT, "div_mymensingh", "শেরপুর", "Sherpur", "89")
    )

    val defaultOrgPositions: List<SeedOrgPosition> = listOf(
        SeedOrgPosition("pos_central_director", "কেন্দ্রীয়", "Central", "পরিচালক", "Director", 1L),
        SeedOrgPosition("pos_division_coord", "বিভাগীয়", "Divisional", "বিভাগীয় সমন্বয়ক", "Divisional Coordinator", 2L),
        SeedOrgPosition("pos_district_sec", "জেলা", "District", "জেলা সম্পাদক", "District Secretary", 3L),
        SeedOrgPosition("pos_upazila_org", "উপজেলা", "Upazila", "উপজেলা সংগঠক", "Upazila Organizer", 4L),
        SeedOrgPosition("pos_general_member", "সাধারণ", "General", "সদস্য", "Member", 5L)
    )
}
