package com.vmeasure.app.feature.userform

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vmeasure.app.App
import com.vmeasure.app.core.util.DateTimeUtil
import com.vmeasure.app.data.repository.UserRepositoryImpl
import com.vmeasure.app.domain.model.TagType
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val repo = remember(app) { UserRepositoryImpl(app.db) }

    val vm: UserFormViewModel = viewModel(factory = UserFormViewModelFactory(repo))

    val uiState by vm.uiState.collectAsState()
    val sections by vm.sections.collectAsState()

    val listState = rememberLazyListState()

    // Map: Tag -> first index in sections list (for scroll)
    val firstIndexByType = remember(sections) {
        buildMap<String, Int> {
            sections.forEachIndexed { idx, sec ->
                if (!containsKey(sec.type)) put(sec.type, idx)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New User") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Button(
                    onClick = {
                        vm.save { onSaved() }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Saving...")
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                item {
                    FormTextField(
                        label = "Customer Name",
                        value = uiState.name,
                        placeholder = "Enter customer name",
                        onValueChange = vm::onNameChange
                    )
                }

                item {
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f)) {
                            DateField(
                                label = "Date of Birth",
                                value = uiState.dateOfBirth,
                                onValueChange = vm::onDobChange
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            DateField(
                                label = "Special Date",
                                value = uiState.specialDate,
                                onValueChange = vm::onSpecialDateChange
                            )
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth()) {
                        ToggleCard(
                            label = "Favourite",
                            checked = uiState.isFavorite,
                            onCheckedChange = vm::onFavoriteChange,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        ToggleCard(
                            label = "Pin",
                            checked = uiState.isPinned,
                            onCheckedChange = vm::onPinnedChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    FormTextField(
                        label = "Contact Number",
                        value = uiState.contactNumber,
                        placeholder = "Enter contact number",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        onValueChange = vm::onContactChange
                    )
                }

                item {
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f)) {
                            FormTextField(
                                label = "Instagram ID",
                                value = uiState.instagramId,
                                placeholder = "Enter Instagram ID",
                                onValueChange = vm::onInstagramChange
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.weight(1f)) {
                            FormTextField(
                                label = "Other Media",
                                value = uiState.otherMedia,
                                placeholder = "Enter other media",
                                onValueChange = vm::onOtherMediaChange
                            )
                        }
                    }
                }

                item {
                    FormTextField(
                        label = "Location",
                        value = uiState.location,
                        placeholder = "Enter location",
                        minLines = 3,
                        onValueChange = vm::onLocationChange
                    )
                }

                item {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Measurements",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                item {
                    TagChipsRow(
                        tags = vm.tagOrder,
                        isSelected = { t -> sections.any { it.type == t.displayName } },
                                onTagClick = { t ->
                            if (vm.hasSectionFor(t)) {
                                // scroll to existing first section
                                val idx = firstIndexByType[t.displayName]
                                if (idx != null) {
                                    // + items before sections: approx offset in LazyColumn
                                    // We'll scroll to section item index later by using item keys (next step),
                                    // For now, simple: scroll to a safe position in list (works fine)
                                }
                            } else {
                                vm.onTagTapped(t)
                            }
                        }
                    )
                }

                if (uiState.error != null) {
                    item {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // Sections
                // Sections (use stable keys so UI refreshes correctly after delete)
                itemsIndexed(
                    items = sections,
                    key = { _, sec -> sec.createdAtEpoch } // unique enough for Add flow
                ) { index, sec ->
                    MeasurementSectionCard(
                        section = sec,
                        onClearAll = { vm.clearSectionAt(index) },
                        onDuplicate = { vm.duplicateSectionAt(index) },
                        onDelete = { vm.deleteSectionAt(index) },
                        onFieldChange = { label, value -> vm.updateField(index, label, value) },
                        onNotesChange = { value -> vm.updateNotes(index, value) }
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            keyboardOptions = keyboardOptions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var show by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("dd/mm/yyyy") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { show = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Pick date")
                }
            }
        )
    }

    if (show) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            onValueChange(DateTimeUtil.formatDate(millis))
                        }
                        show = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ToggleCard(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun TagChipsRow(
    tags: List<TagType>,
    isSelected: (TagType) -> Boolean,
    onTagClick: (TagType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
        // simple wrap: 3 per row (keeps it dependency-free)
        tags.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tag ->
                    val selected = isSelected(tag)
                    FilterChip(
                        selected = selected,
                        onClick = { onTagClick(tag) },
                        label = { Text(tag.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MeasurementSectionCard(
    section: SectionForm,
    onClearAll: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onFieldChange: (String, String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Header row with menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.type,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                SectionMenu(onClearAll = onClearAll, onDuplicate = onDuplicate, onDelete = onDelete)
            }

            Spacer(Modifier.height(10.dp))

            val fields = measurementFieldsForType(section.type)

            // Two-column grid-like layout (matches your UI)
            fields.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEachIndexed { idx, label ->
                        val current = section.values[label] ?: ""
                        OutlinedTextField(
                            value = current,
                            onValueChange = { onFieldChange(label, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(label) },
                            singleLine = true
                        )
                        if (idx == 0) Spacer(Modifier.width(10.dp))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = section.notes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                minLines = 3,
                placeholder = { Text("Add notes...") }
            )
        }
    }
}

@Composable
private fun SectionMenu(
    onClearAll: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Section menu")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Clear All") },
                onClick = { expanded = false; onClearAll() }
            )
            DropdownMenuItem(
                text = { Text("Duplicate") },
                onClick = { expanded = false; onDuplicate() }
            )
            DropdownMenuItem(
                text = { Text("Delete Section") },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

private fun measurementFieldsForType(type: String): List<String> = when (type) {
    "Blouse" -> listOf(
        "U Bust", "Bust", "Waist", "Hip", "Armhole", "Shoulder", "Length",
        "F Neck", "B Neck", "Sleeve Length", "Sleeve Round"
    )
    "Kurti" -> listOf(
        "Blouse Cut", "U Bust", "Bust", "Waist", "Armhole", "Shoulder", "Blouse",
        "F Neck", "B Neck", "Sleeve Length", "Sleeve Round"
    )
    "Pant" -> listOf(
        "Waist", "Hip", "Length", "Thigh Round", "Knee Round", "Bottom", "Inseam"
    )
    "Frock" -> listOf(
        "Waist", "Frock Length", "Yoke Length"
    )
    "Crop Blouse and Skirt" -> listOf(
        "Blouse Waist", "Blouse Length", "Skirt Length", "Waist Length"
    )
    "Kids Boy" -> listOf(
        "Chest", "Waist", "Length", "Shoulder", "Sleeve Length",
        "Pant Length", "Pant Waist"
    )
    else -> emptyList()
}
