package com.vmeasure.app.feature.lists

data class ListsUiState(
    val searchText: String = "",
    val nameSortAsc: Boolean = true // default A–Z
)

sealed interface ListsUiEvent {
    data class ShareText(val text: String) : ListsUiEvent
    data class Error(val message: String) : ListsUiEvent
}

