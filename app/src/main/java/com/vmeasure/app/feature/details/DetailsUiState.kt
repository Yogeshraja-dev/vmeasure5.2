package com.vmeasure.app.feature.details

import com.vmeasure.app.feature.userform.SectionForm
import com.vmeasure.app.feature.userform.UserFormUiState

data class DetailsUiState(
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val form: UserFormUiState = UserFormUiState(),
    val sections: List<SectionForm> = emptyList()
)
