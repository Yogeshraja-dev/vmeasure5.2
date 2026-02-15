package com.vmeasure.app.feature.lists

data class ListsUiState(
    val searchText: String = "",
//    val nameSortAsc: Boolean = true // default A–Z
    val filters: ListFilters = ListFilters()
)

sealed interface ListsUiEvent {
    data class ShareText(val text: String) : ListsUiEvent
    data class Error(val message: String) : ListsUiEvent
}

