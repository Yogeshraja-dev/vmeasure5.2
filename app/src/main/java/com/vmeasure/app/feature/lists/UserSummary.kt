package com.vmeasure.app.feature.lists

data class UserSummary(
    val publicUserId: String,
    val name: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val tags: List<String>,
    val createdAtEpoch: Long,
    val editedAtEpoch: Long?
)
