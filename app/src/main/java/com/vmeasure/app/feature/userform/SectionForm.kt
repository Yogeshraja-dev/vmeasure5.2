package com.vmeasure.app.feature.userform

/**
 * UI model for measurement section.
 * sectionId is null for new/unsaved sections (Add flow), but always non-null for existing sections (Edit flow).
 */
data class SectionForm(
    val sectionId: String? = null,
    val type: String,
    val createdAtEpoch: Long,
    val values: Map<String, String> = emptyMap(),
    val notes: String = ""
)
