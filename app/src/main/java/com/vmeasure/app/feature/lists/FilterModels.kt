package com.vmeasure.app.feature.lists

import com.vmeasure.app.domain.model.TagType

//enum class DateSortOption {
//    CUSTOM_EDITED_DATE,
//    RECENT_EDITED_DATE,
//    LAST_UPDATED_DATE
//}
//
//enum class NameSortOption {
//    A_Z,
//    Z_A
//}

enum class SortOption {
    CUSTOM_EDITED_DATE,
    RECENT_EDITED_DATE,
    LAST_UPDATED_DATE,
    A_Z,
    Z_A
}

data class ListFilters(
//    val dateSort: DateSortOption = DateSortOption.RECENT_EDITED_DATE,
//    val nameSort: NameSortOption = NameSortOption.A_Z,
    val sort: SortOption = SortOption.RECENT_EDITED_DATE,

    // Type filter = AND semantics
    val typesAnd: Set<TagType> = emptySet(),

    val favouriteOnly: Boolean = false,
    val pinnedOnly: Boolean = false,

    // Special Date range (user.specialDate)
    val specialFrom: String = "",  // dd/MM/yyyy
    val specialTo: String = "",    // dd/MM/yyyy

    // Custom Edited Date range (user.editedAt)
    val editedFrom: String = "",   // dd/MM/yyyy
    val editedTo: String = ""      // dd/MM/yyyy
) {
    fun isDefault(): Boolean = this == ListFilters()
}

//data class ListsUiState(
//    val searchText: String = "",
//    val filters: ListFilters = ListFilters()
//)
