package com.vmeasure.app.feature.lists

import com.vmeasure.app.domain.model.TagType

enum class DateSortOption {
    CUSTOM_EDITED_DATE,   // UI option (we treat as editedAt sort, no extra inputs in mock)
    RECENT_EDITED_DATE,   // editedAt desc
    LAST_UPDATED_DATE     // same as editedAt desc (per your earlier answer)
}

enum class NameSortOption { A_Z, Z_A }

//data class ListFilters(
//    val dateSort: DateSortOption = DateSortOption.RECENT_EDITED_DATE,
//    val nameSort: NameSortOption = NameSortOption.A_Z,
//    val typesAnd: Set<TagType> = emptySet(),   // AND semantics
//    val favouriteOnly: Boolean = false,
//    val pinnedOnly: Boolean = false,
//    val specialFrom: String = "",              // dd/MM/yyyy (UI input)
//    val specialTo: String = ""                 // dd/MM/yyyy (UI input)
//)

data class ListFilters(
    val dateSort: DateSortOption = DateSortOption.RECENT_EDITED_DATE,
    val nameSort: NameSortOption = NameSortOption.A_Z,

    // Type filter = AND semantics
    val typesAnd: Set<TagType> = emptySet(),

    val favouriteOnly: Boolean = false,
    val pinnedOnly: Boolean = false,

    // Special Date range (User.specialDate)
    val specialFrom: String = "",   // dd/MM/yyyy
    val specialTo: String = "",     // dd/MM/yyyy

    // Custom Edited Date range (User.editedAt)
    val editedFrom: String = "",    // dd/MM/yyyy
    val editedTo: String = ""       // dd/MM/yyyy
) {
    fun isDefault(): Boolean = this == ListFilters()
}
