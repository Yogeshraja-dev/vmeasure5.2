package com.vmeasure.app.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vmeasure.app.domain.repository.UserRepository

class DetailsViewModelFactory(
    private val repo: UserRepository,
    private val publicUserId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailsViewModel(repo, publicUserId) as T
    }
}
