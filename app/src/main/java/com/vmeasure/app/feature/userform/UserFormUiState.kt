package com.vmeasure.app.feature.userform

data class UserFormUiState(
    val name: String = "",
    val dateOfBirth: String = "",
    val specialDate: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val contactNumber: String = "",
    val instagramId: String = "",
    val otherMedia: String = "",
    val location: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)
