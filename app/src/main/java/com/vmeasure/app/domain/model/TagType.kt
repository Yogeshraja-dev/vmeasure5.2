package com.vmeasure.app.domain.model

enum class TagType(val displayName: String) {
    BLOUSE("Blouse"),
    KURTI("Kurti"),
    PANT("Pant"),
    FROCK("Frock"),
    CROP_BLOUSE_AND_SKIRT("Crop Blouse and Skirt"),
    KIDS_BOY("Kids Boy");

    companion object {
        val fixedOrder: List<TagType> = listOf(
            BLOUSE, KURTI, PANT, FROCK, CROP_BLOUSE_AND_SKIRT, KIDS_BOY
        )

        fun fromDisplayName(name: String): TagType? =
            values().firstOrNull { it.displayName.equals(name.trim(), ignoreCase = true) }
    }
}
