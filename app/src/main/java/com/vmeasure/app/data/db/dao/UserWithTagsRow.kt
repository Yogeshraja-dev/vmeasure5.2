package com.vmeasure.app.data.db.dao

/**
 * Projection for Lists screen:
 * - One row per user
 * - tagsCsv is a comma-separated list of distinct section types (e.g., "Blouse,Kurti")
 */
data class UserWithTagsRow(
    val publicUserId: String,
    val name: String,
    val nameNormalized: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val createdAtEpoch: Long,
    val editedAtEpoch: Long?,
    val tagsCsv: String?
)
