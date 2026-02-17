package com.vmeasure.app.feature.lists

import com.vmeasure.app.data.db.dao.UserWithTagsRow

fun UserWithTagsRow.toSummary(): UserSummary {
    val tagsList = tagsCsv
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()

    return UserSummary(
        publicUserId = publicUserId,
        name = name,
        isPinned = isPinned,
        isFavorite = isFavorite,
        tags = tagsList,
        createdAtEpoch = createdAtEpoch,
        editedAtEpoch = editedAtEpoch
    )
}
