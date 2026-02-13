package com.vmeasure.app.feature.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
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
import com.vmeasure.app.feature.userform.SectionForm
import com.vmeasure.app.feature.userform.UserFormUiState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import com.vmeasure.app.domain.model.TagType
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    publicUserId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    val repo = remember(app) { UserRepositoryImpl(app.db) }

    val vm: DetailsViewModel = viewModel(factory = DetailsViewModelFactory(repo, publicUserId))
    val st by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Details") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!st.isLoading && !st.isEditMode) {
                        IconButton(onClick = vm::enterEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (st.isEditMode) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = vm::cancelEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !st.isSaving
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { vm.save(onSaved) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !st.isSaving
                        ) {
                            if (st.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Saving...")
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->

        if (st.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (st.error != null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(st.error!!, color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        val readOnly = !st.isEditMode

        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        var pendingScrollType by remember { mutableStateOf<String?>(null) }

        val sectionsStartIndex = 8 + (if (st.isEditMode) 1 else 0)

        val firstSectionIndexByType = remember(st.sections) {
            buildMap<String, Int> {
                st.sections.forEachIndexed { idx, sec ->
                    if (!containsKey(sec.type)) put(sec.type, idx)
                }
            }
        }

        LaunchedEffect(st.sections, pendingScrollType, st.isEditMode) {
            val type = pendingScrollType ?: return@LaunchedEffect
            val secIndex = firstSectionIndexByType[type] ?: return@LaunchedEffect
            listState.animateScrollToItem(sectionsStartIndex + secIndex)
            pendingScrollType = null
        }


        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = if (st.isEditMode) 96.dp else 16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                FormTextField(
                    label = "Customer Name",
                    value = st.form.name,
                    placeholder = "Enter customer name",
                    readOnly = readOnly,
                    onValueChange = { v -> vm.updateForm { it.copy(name = v) } }
                )
            }

            item {
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        DateField(
                            label = "Date of Birth",
                            value = st.form.dateOfBirth,
                            readOnly = readOnly,
                            onValueChange = { v -> vm.updateForm { it.copy(dateOfBirth = v) } }
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        DateField(
                            label = "Special Date",
                            value = st.form.specialDate,
                            readOnly = readOnly,
                            onValueChange = { v -> vm.updateForm { it.copy(specialDate = v) } }
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth()) {
                    ToggleCard(
                        label = "Favourite",
                        checked = st.form.isFavorite,
                        enabled = !readOnly,
                        onCheckedChange = { v -> vm.updateForm { it.copy(isFavorite = v) } },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    ToggleCard(
                        label = "Pin",
                        checked = st.form.isPinned,
                        enabled = !readOnly,
                        onCheckedChange = { v -> vm.updateForm { it.copy(isPinned = v) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                FormTextField(
                    label = "Contact Number",
                    value = st.form.contactNumber,
                    placeholder = "Enter contact number",
                    readOnly = readOnly,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    onValueChange = { v -> vm.updateForm { it.copy(contactNumber = v) } }
                )
            }

            item {
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        FormTextField(
                            label = "Instagram ID",
                            value = st.form.instagramId,
                            placeholder = "Enter Instagram ID",
                            readOnly = readOnly,
                            onValueChange = { v -> vm.updateForm { it.copy(instagramId = v) } }
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        FormTextField(
                            label = "Other Media",
                            value = st.form.otherMedia,
                            placeholder = "Enter other media",
                            readOnly = readOnly,
                            onValueChange = { v -> vm.updateForm { it.copy(otherMedia = v) } }
                        )
                    }
                }
            }

            item {
                FormTextField(
                    label = "Location",
                    value = st.form.location,
                    placeholder = "Enter location",
                    readOnly = readOnly,
                    minLines = 3,
                    onValueChange = { v -> vm.updateForm { it.copy(location = v) } }
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

            // Tags row (only in Edit mode) so user can add new sections
            if (st.isEditMode) {
                item {
                    TagChipsRow(
                        tags = TagType.fixedOrder,
                        isSelected = { t -> st.sections.any { it.type == t.displayName } },
//                        onTagClick = { t -> vm.onTagTapped(t) }
                        onTagClick = { t ->
                            val type = t.displayName
                            if (st.sections.any { it.type == type }) {
                                pendingScrollType = type
                            } else {
                                vm.onTagTapped(t)   // you already added this method in DetailsViewModel
                                pendingScrollType = type
                            }
                        }
                    )
                }
            }

            if (st.error != null) {
                item {
                    Text(
                        text = st.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            items(st.sections.size) { index ->
                val sec = st.sections[index]
                MeasurementSectionCardDetails(
                    section = sec,
                    readOnly = readOnly,
                    showMenu = st.isEditMode,
                    onClearAll = { vm.clearSectionAt(index) },
                    onDuplicate = { vm.duplicateSectionAt(index) },
                    onDelete = { vm.deleteSectionAt(index) },
                    onFieldChange = { label, value -> vm.updateSectionField(index, label, value) },
                    onNotesChange = { value -> vm.updateSectionNotes(index, value) }
                )
            }
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    placeholder: String,
    readOnly: Boolean,
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
            readOnly = readOnly,
            enabled = !readOnly,
            keyboardOptions = keyboardOptions
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    readOnly: Boolean,
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
            enabled = !readOnly,
            trailingIcon = {
                IconButton(onClick = { show = true }, enabled = !readOnly) {
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
                        if (millis != null) onValueChange(DateTimeUtil.formatDate(millis))
                        show = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun ToggleCard(
    label: String,
    checked: Boolean,
    enabled: Boolean,
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
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun MeasurementSectionCardDetails(
    section: SectionForm,
    readOnly: Boolean,
    showMenu: Boolean,
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = section.type,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Created Time: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = DateTimeUtil.formatDateTime(section.createdAtEpoch),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Edited Time: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = section.editedAtEpoch?.let {
                                DateTimeUtil.formatDateTime(it)
                            } ?: "-",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (showMenu) {
                    SectionMenu(onClearAll = onClearAll, onDuplicate = onDuplicate, onDelete = onDelete)
                }
            }

            Spacer(Modifier.height(10.dp))

            val fields = measurementFieldsForType(section.type)

            fields.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEachIndexed { idx, label ->
                        val current = section.values[label] ?: ""
                        OutlinedTextField(
                            value = current,
                            onValueChange = { onFieldChange(label, it) },
                            modifier = Modifier.weight(1f),
                            label = { Text(label) },
                            singleLine = true,
                            readOnly = readOnly,
                            enabled = !readOnly
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
                readOnly = readOnly,
                enabled = !readOnly,
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
            DropdownMenuItem(text = { Text("Clear All") }, onClick = { expanded = false; onClearAll() })
            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { expanded = false; onDuplicate() })
            DropdownMenuItem(text = { Text("Delete Section") }, onClick = { expanded = false; onDelete() })
        }
    }
}

private fun measurementFieldsForType(type: String): List<String> = when (type) {
    "Blouse" -> listOf("U Bust","Bust","Waist","Hip","Armhole","Shoulder","Length","F Neck","B Neck","Sleeve Length","Sleeve Round")
    "Kurti" -> listOf("Blouse Cut","U Bust","Bust","Waist","Armhole","Shoulder","Blouse","F Neck","B Neck","Sleeve Length","Sleeve Round")
    "Pant" -> listOf("Waist","Hip","Length","Thigh Round","Knee Round","Bottom","Inseam")
    "Frock" -> listOf("Waist","Frock Length","Yoke Length")
    "Crop Blouse and Skirt" -> listOf("Blouse Waist","Blouse Length","Skirt Length","Waist Length")
    "Kids Boy" -> listOf("Chest","Waist","Length","Shoulder","Sleeve Length","Pant Length","Pant Waist")
    else -> emptyList()
}

@Composable
private fun TagChipsRow(
    tags: List<TagType>,
    isSelected: (TagType) -> Boolean,
    onTagClick: (TagType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
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
