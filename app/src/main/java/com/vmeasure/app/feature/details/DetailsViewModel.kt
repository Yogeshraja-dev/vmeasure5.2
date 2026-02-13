package com.vmeasure.app.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.data.db.entity.UserEntity
import com.vmeasure.app.domain.repository.UserRepository
import com.vmeasure.app.feature.userform.SectionForm
import com.vmeasure.app.feature.userform.UserFormUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val repo: UserRepository,
    private val publicUserId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState

    private var originalUser: UserEntity? = null
    private var originalSections: List<MeasurementSectionEntity> = emptyList()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching { repo.loadUserWithSections(publicUserId) }
                .onSuccess { (user, sections) ->
                    originalUser = user
                    originalSections = sections

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            isEditMode = false,
                            form = user.toForm(),
                            sections = sections.map { it.toForm() }
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Load failed") }
                }
        }
    }

    fun enterEdit() {
        _uiState.update { it.copy(isEditMode = true, error = null) }
    }

    fun cancelEdit() {
        val user = originalUser ?: return
        _uiState.update {
            it.copy(
                isEditMode = false,
                error = null,
                form = user.toForm(),
                sections = originalSections.map { s -> s.toForm() }
            )
        }
    }

    fun updateForm(update: (UserFormUiState) -> UserFormUiState) {
        _uiState.update { it.copy(form = update(it.form)) }
    }

    fun clearSectionAt(index: Int) {
        _uiState.update { st ->
            val list = st.sections.toMutableList()
            val old = list[index]
            list[index] = old.copy(values = emptyMap(), notes = "")
            st.copy(sections = list)
        }
    }

    fun deleteSectionAt(index: Int) {
        _uiState.update { st ->
            val list = st.sections.toMutableList()
            list.removeAt(index)
            st.copy(sections = list)
        }
    }

    fun duplicateSectionAt(index: Int) {
        _uiState.update { st ->
            val list = st.sections.toMutableList()
            val original = list[index]
            // New section in edit mode => sectionId null so repo inserts a new row (new 6-digit ID)
            val copy = original.copy(sectionId = null)
            list.add(index + 1, copy)
            st.copy(sections = list)
        }
    }

    fun updateSectionField(index: Int, label: String, value: String) {
        _uiState.update { st ->
            val list = st.sections.toMutableList()
            val old = list[index]
            val m = old.values.toMutableMap()
            m[label] = value
            list[index] = old.copy(values = m)
            st.copy(sections = list)
        }
    }

    fun updateSectionNotes(index: Int, value: String) {
        _uiState.update { st ->
            val list = st.sections.toMutableList()
            val old = list[index]
            list[index] = old.copy(notes = value)
            st.copy(sections = list)
        }
    }

    fun save(onSaved: () -> Unit) {
        val user = originalUser ?: return
        val form = _uiState.value.form
        val sections = _uiState.value.sections

        val nameTrim = form.name.trim()
        if (nameTrim.isBlank()) {
            _uiState.update { it.copy(error = "Customer name is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            runCatching {
                repo.saveUserEdits(
                    publicUserId = publicUserId,
                    originalUser = user,
                    originalSections = originalSections,
                    updatedForm = form.copy(name = nameTrim),
                    updatedSections = sections
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, isEditMode = false) }
                onSaved()
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
            }
        }
    }
}

private fun UserEntity.toForm(): UserFormUiState = UserFormUiState(
    name = name,
    dateOfBirth = dateOfBirth,
    specialDate = specialDate,
    isFavorite = isFavorite,
    isPinned = isPinned,
    contactNumber = contactNumber,
    instagramId = instagramId,
    otherMedia = otherMedia,
    location = location,
    isSaving = false,
    error = null
)

private fun MeasurementSectionEntity.toForm(): SectionForm {
    val values = mutableMapOf<String, String>()

    fun put(label: String, v: String?) {
        val t = v?.trim().orEmpty()
        if (t.isNotBlank()) values[label] = t
    }

    when (type) {
        "Blouse" -> {
            put("U Bust", blouse_uBust)
            put("Bust", blouse_bust)
            put("Waist", blouse_waist)
            put("Hip", blouse_hip)
            put("Armhole", blouse_armhole)
            put("Shoulder", blouse_shoulder)
            put("Length", blouse_length)
            put("F Neck", blouse_fNeck)
            put("B Neck", blouse_bNeck)
            put("Sleeve Length", blouse_sleeveLength)
            put("Sleeve Round", blouse_sleeveRound)
        }
        "Kurti" -> {
            put("Blouse Cut", kurti_blouseCut)
            put("U Bust", kurti_uBust)
            put("Bust", kurti_bust)
            put("Waist", kurti_waist)
            put("Armhole", kurti_armhole)
            put("Shoulder", kurti_shoulder)
            put("Blouse", kurti_blouse)
            put("F Neck", kurti_fNeck)
            put("B Neck", kurti_bNeck)
            put("Sleeve Length", kurti_sleeveLength)
            put("Sleeve Round", kurti_sleeveRound)
        }
        "Pant" -> {
            put("Waist", pant_waist)
            put("Hip", pant_hip)
            put("Length", pant_length)
            put("Thigh Round", pant_thighRound)
            put("Knee Round", pant_kneeRound)
            put("Bottom", pant_bottom)
            put("Inseam", pant_inseam)
        }
        "Frock" -> {
            put("Waist", frock_waist)
            put("Frock Length", frock_frockLength)
            put("Yoke Length", frock_yokeLength)
        }
        "Crop Blouse and Skirt" -> {
            put("Blouse Waist", crop_blouseWaist)
            put("Blouse Length", crop_blouseLength)
            put("Skirt Length", crop_skirtLength)
            put("Waist Length", crop_waistLength)
        }
        "Kids Boy" -> {
            put("Chest", kids_chest)
            put("Waist", kids_waist)
            put("Length", kids_length)
            put("Shoulder", kids_shoulder)
            put("Sleeve Length", kids_sleeveLength)
            put("Pant Length", kids_pantLength)
            put("Pant Waist", kids_pantWaist)
        }
    }

    return SectionForm(
        sectionId = sectionId,
        type = type,
        createdAtEpoch = createdAtEpoch,
        values = values,
        notes = notes?.orEmpty() ?: ""
    )
}
