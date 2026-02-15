package com.vmeasure.app.domain.repository

import androidx.paging.PagingSource
import com.vmeasure.app.domain.model.UserSummary
import com.vmeasure.app.data.db.dao.UserWithTagsRow
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.data.db.entity.UserEntity
import com.vmeasure.app.feature.lists.ListFilters
import com.vmeasure.app.feature.userform.UserFormUiState
import com.vmeasure.app.feature.userform.SectionForm

interface UserRepository {
//    fun pagingUserSummaries(search: String?, nameSortAsc: Boolean): PagingSource<Int, UserSummary>

//    fun pagingUserRows(search: String?, nameSortAsc: Boolean): PagingSource<Int, UserWithTagsRow>
fun pagingUserRows(search: String?, filters: ListFilters): PagingSource<Int, UserWithTagsRow>
    suspend fun setPinned(publicUserId: String, pinned: Boolean)
    suspend fun setFavorite(publicUserId: String, favorite: Boolean)
    suspend fun deleteUser(publicUserId: String)

    suspend fun buildShareReport(publicUserId: String): String

    suspend fun createUser(
        name: String,
        dateOfBirth: String,
        specialDate: String,
        isFavorite: Boolean,
        isPinned: Boolean,
        contactNumber: String,
        instagramId: String,
        otherMedia: String,
        location: String,
        sections: List<com.vmeasure.app.feature.userform.SectionForm>
    ): String

//    suspend fun loadUserWithSections(publicUserId: String): Pair<com.vmeasure.app.data.db.entity.UserEntity, List<com.vmeasure.app.data.db.entity.MeasurementSectionEntity>>
//
//    suspend fun saveUserEdits(
//        originalUser: com.vmeasure.app.data.db.entity.UserEntity,
//        originalSections: List<com.vmeasure.app.data.db.entity.MeasurementSectionEntity>,
//        updatedForm: com.vmeasure.app.feature.userform.UserFormUiState,
//        updatedSections: List<com.vmeasure.app.feature.userform.SectionForm>
//    )

    suspend fun loadUserWithSections(publicUserId: String): Pair<UserEntity, List<MeasurementSectionEntity>>

    suspend fun saveUserEdits(
        publicUserId: String,
        originalUser: UserEntity,
        originalSections: List<MeasurementSectionEntity>,
        updatedForm: UserFormUiState,
        updatedSections: List<SectionForm>
    )

}
