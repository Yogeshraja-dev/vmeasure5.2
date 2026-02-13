package com.vmeasure.app.domain.repository

import androidx.paging.PagingSource
import com.vmeasure.app.domain.model.UserSummary

import com.vmeasure.app.data.db.dao.UserWithTagsRow

interface UserRepository {
//    fun pagingUserSummaries(search: String?, nameSortAsc: Boolean): PagingSource<Int, UserSummary>

    fun pagingUserRows(search: String?, nameSortAsc: Boolean): PagingSource<Int, UserWithTagsRow>
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

}
