package com.vmeasure.app.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.vmeasure.app.domain.model.UserSummary
import com.vmeasure.app.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import androidx.paging.map
import kotlinx.coroutines.flow.map
class ListsViewModel(
    private val repo: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ListsUiEvent>()
    val events: SharedFlow<ListsUiEvent> = _events.asSharedFlow()

    // Local optimistic overrides for immediate UI response (pin/fav)
    private val _overrides = MutableStateFlow<Map<String, OverrideFlags>>(emptyMap())
    val overrides: StateFlow<Map<String, OverrideFlags>> = _overrides.asStateFlow()

    private val toggleJobs = mutableMapOf<String, Job>() // key: "<userId>:pin" or "<userId>:fav"

    val usersPaging: Flow<PagingData<UserSummary>> =
        uiState
            .debounce(250)
            .distinctUntilChanged()
            .flatMapLatest { state ->
                Pager(
                    config = PagingConfig(
                        pageSize = 100,
                        prefetchDistance = 20,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        repo.pagingUserRows(
                            search = state.searchText.trim().lowercase().ifEmpty { null },
                            nameSortAsc = state.nameSortAsc
                        )
                    }
                ).flow
            }
            .map { pagingData ->
                pagingData.map { row ->
                    val tags = row.tagsCsv
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?: emptyList()

                    UserSummary(
                        publicUserId = row.publicUserId,
                        name = row.name,
                        isPinned = row.isPinned,
                        isFavorite = row.isFavorite,
                        createdAtEpoch = row.createdAtEpoch,
                        tags = tags
                    )
                }
            }
            .cachedIn(viewModelScope)

    fun onSearchChanged(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }

    fun onTogglePinned(user: UserSummary) {
        val newValue = !(overrideOf(user.publicUserId)?.isPinned ?: user.isPinned)
        setOverride(user.publicUserId) { it.copy(isPinned = newValue) }
        debouncedWrite(key = "${user.publicUserId}:pin") {
            repo.setPinned(user.publicUserId, newValue)
        }
    }

    fun onToggleFavorite(user: UserSummary) {
        val newValue = !(overrideOf(user.publicUserId)?.isFavorite ?: user.isFavorite)
        setOverride(user.publicUserId) { it.copy(isFavorite = newValue) }
        debouncedWrite(key = "${user.publicUserId}:fav") {
            repo.setFavorite(user.publicUserId, newValue)
        }
    }

    fun onDeleteUser(user: UserSummary) {
        viewModelScope.launch {
            repo.deleteUser(user.publicUserId)
        }
    }

    fun onShareUser(user: UserSummary) {
        viewModelScope.launch {
            runCatching { repo.buildShareReport(user.publicUserId) }
                .onSuccess { _events.emit(ListsUiEvent.ShareText(it)) }
                .onFailure { _events.emit(ListsUiEvent.Error(it.message ?: "Share failed")) }
        }
    }

    fun onFilterClicked() {
        // Next milestone: open filter bottom sheet
    }

    private fun debouncedWrite(key: String, block: suspend () -> Unit) {
        toggleJobs[key]?.cancel()
        toggleJobs[key] = viewModelScope.launch {
            delay(250) // debounce writes
            block()
        }
    }

    private fun overrideOf(publicUserId: String): OverrideFlags? = _overrides.value[publicUserId]

    private fun setOverride(publicUserId: String, update: (OverrideFlags) -> OverrideFlags) {
        _overrides.update { map ->
            val current = map[publicUserId] ?: OverrideFlags()
            map + (publicUserId to update(current))
        }
    }

    data class OverrideFlags(
        val isPinned: Boolean? = null,
        val isFavorite: Boolean? = null
    )
}
