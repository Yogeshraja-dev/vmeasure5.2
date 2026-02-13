package com.vmeasure.app.feature.userform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vmeasure.app.domain.repository.UserRepository

class UserFormViewModelFactory(
    private val repo: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UserFormViewModel(repo) as T
    }
}
