package com.vmeasure.app.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vmeasure.app.domain.repository.UserRepository

class ListsViewModelFactory(
    private val repo: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ListsViewModel(repo) as T
    }
}
