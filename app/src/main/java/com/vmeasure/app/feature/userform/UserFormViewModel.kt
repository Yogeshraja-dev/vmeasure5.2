package com.vmeasure.app.feature.userform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.core.util.DateTimeUtil
import com.vmeasure.app.domain.model.TagType
import com.vmeasure.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserFormViewModel(
    private val repo: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserFormUiState())
    val uiState: StateFlow<UserFormUiState> = _uiState

    // Ordered list of sections as they appear on screen (grouped by tag order)
    private val _sections = MutableStateFlow<List<SectionForm>>(emptyList())
    val sections: StateFlow<List<SectionForm>> = _sections

    val tagOrder: List<TagType> = TagType.fixedOrder

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun onDobChange(v: String) = _uiState.update { it.copy(dateOfBirth = v) }
    fun onSpecialDateChange(v: String) = _uiState.update { it.copy(specialDate = v) }
    fun onFavoriteChange(v: Boolean) = _uiState.update { it.copy(isFavorite = v) }
    fun onPinnedChange(v: Boolean) = _uiState.update { it.copy(isPinned = v) }
    fun onContactChange(v: String) = _uiState.update { it.copy(contactNumber = v) }
    fun onInstagramChange(v: String) = _uiState.update { it.copy(instagramId = v) }
    fun onOtherMediaChange(v: String) = _uiState.update { it.copy(otherMedia = v) }
    fun onLocationChange(v: String) = _uiState.update { it.copy(location = v) }

    fun hasSectionFor(tag: TagType): Boolean = _sections.value.any { it.type == tag.displayName }

    fun onTagTapped(tag: TagType) {
        if (hasSectionFor(tag)) return
        val now = DateTimeUtil.nowEpochMillis()
        val newSec = SectionForm(type = tag.displayName, createdAtEpoch = now)
        _sections.update { current ->
            insertSectionGrouped(current, newSec)
        }
    }

    fun clearSectionAt(index: Int) {
        _sections.update { current ->
            current.toMutableList().apply {
                val old = this[index]
                this[index] = old.copy(values = emptyMap(), notes = "")
            }
        }
    }

    fun deleteSectionAt(index: Int) {
        _sections.update { current ->
            current.toMutableList().apply { removeAt(index) }
        }
    }

    fun duplicateSectionAt(index: Int) {
        val now = DateTimeUtil.nowEpochMillis()
        _sections.update { current ->
            current.toMutableList().apply {
                val original = this[index]
                val copy = original.copy(createdAtEpoch = now)
                add(index + 1, copy) // next to original
            }
        }
    }

    fun updateField(index: Int, label: String, value: String) {
        _sections.update { current ->
            current.toMutableList().apply {
                val old = this[index]
                val newValues = old.values.toMutableMap()
                newValues[label] = value
                this[index] = old.copy(values = newValues)
            }
        }
    }

    fun updateNotes(index: Int, value: String) {
        _sections.update { current ->
            current.toMutableList().apply {
                val old = this[index]
                this[index] = old.copy(notes = value)
            }
        }
    }

    fun save(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        val nameTrim = state.name.trim()
        if (nameTrim.isBlank()) {
            _uiState.update { it.copy(error = "Customer name is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            runCatching {
                repo.createUser(
                    name = nameTrim,
                    dateOfBirth = state.dateOfBirth,
                    specialDate = state.specialDate,
                    isFavorite = state.isFavorite,
                    isPinned = state.isPinned,
                    contactNumber = state.contactNumber,
                    instagramId = state.instagramId,
                    otherMedia = state.otherMedia,
                    location = state.location,
                    sections = _sections.value
                )
            }.onSuccess { publicUserId ->
                _uiState.update { it.copy(isSaving = false) }
                onSuccess(publicUserId)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isSaving = false, error = e.message ?: "Save failed")
                }
            }
        }
    }

    private fun insertSectionGrouped(current: List<SectionForm>, newSec: SectionForm): List<SectionForm> {
        // Insert respecting tag order. Within tag, preserve creation order.
        val orderIndex = tagOrder.indexOfFirst { it.displayName == newSec.type }.takeIf { it >= 0 } ?: Int.MAX_VALUE

        val grouped = current.groupBy { it.type }.toMutableMap()
        val listForType = (grouped[newSec.type] ?: emptyList()).toMutableList()
        listForType.add(newSec)
        grouped[newSec.type] = listForType

        val rebuilt = mutableListOf<SectionForm>()
        tagOrder.forEach { t ->
            grouped[t.displayName]?.let { rebuilt.addAll(it.sortedBy { s -> s.createdAtEpoch }) }
        }
        // include any unknown types at end
        grouped.keys.filter { k -> tagOrder.none { it.displayName == k } }
            .sorted()
            .forEach { k -> rebuilt.addAll(grouped[k] ?: emptyList()) }

        return rebuilt
    }
}
