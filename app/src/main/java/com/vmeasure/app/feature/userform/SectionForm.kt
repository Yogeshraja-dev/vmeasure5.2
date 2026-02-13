package com.vmeasure.app.feature.userform

/**
 * UI model for a measurement section before saving.
 * - type: "Blouse", "Kurti", etc.
 * - values: label -> string
 * - notes stored separately (still persisted into typed notes column)
 */
data class SectionForm(
    val type: String,
    val createdAtEpoch: Long,
    val values: Map<String, String> = emptyMap(),
    val notes: String = ""
)
