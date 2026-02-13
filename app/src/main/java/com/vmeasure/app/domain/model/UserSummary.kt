package com.vmeasure.app.domain.model

data class UserSummary(
    val publicUserId: String,
    val name: String,
    val isPinned: Boolean,
    val isFavorite: Boolean,
    val createdAtEpoch: Long,
    val tags: List<String>
)
