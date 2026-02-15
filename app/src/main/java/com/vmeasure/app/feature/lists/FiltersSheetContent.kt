package com.vmeasure.app.feature.lists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vmeasure.app.core.util.DateTimeUtil
import com.vmeasure.app.domain.model.TagType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheetContent(
    filters: ListFilters,
    onChange: (ListFilters) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    // Apply enabled only when at least one non-default filter is applied
    val applyEnabled = remember(filters) { !filters.isDefault() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight() // parent controls 70–80% height
    ) {

        // --- Header (fixed) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filters", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close")
            }
        }

        Divider()

        // --- Scrollable content ---
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {

            Text("Sort by Date", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            DateSortRadio(
                label = "Custom edited date",
                selected = filters.dateSort == DateSortOption.CUSTOM_EDITED_DATE,
                onSelect = {
                    onChange(filters.copy(dateSort = DateSortOption.CUSTOM_EDITED_DATE))
                }
            )

            DateSortRadio(
                label = "Recent edited date",
                selected = filters.dateSort == DateSortOption.RECENT_EDITED_DATE,
                onSelect = {
                    // Hide & reset custom edited dates
                    onChange(
                        filters.copy(
                            dateSort = DateSortOption.RECENT_EDITED_DATE,
                            editedFrom = "",
                            editedTo = ""
                        )
                    )
                }
            )

            DateSortRadio(
                label = "Last updated date",
                selected = filters.dateSort == DateSortOption.LAST_UPDATED_DATE,
                onSelect = {
                    // Hide & reset custom edited dates
                    onChange(
                        filters.copy(
                            dateSort = DateSortOption.LAST_UPDATED_DATE,
                            editedFrom = "",
                            editedTo = ""
                        )
                    )
                }
            )

            // --- Custom Edited Date fields (only when selected) ---
            if (filters.dateSort == DateSortOption.CUSTOM_EDITED_DATE) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DatePickerField(
                        label = "From Date",
                        value = filters.editedFrom,
                        placeholder = "dd/mm/yyyy",
                        onValueChange = { onChange(filters.copy(editedFrom = it)) },
                        modifier = Modifier.weight(1f)
                    )
                    DatePickerField(
                        label = "To Date",
                        value = filters.editedTo,
                        placeholder = "dd/mm/yyyy",
                        onValueChange = { onChange(filters.copy(editedTo = it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("Sorting by name", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            RadioRow(
                label = "A–Z",
                selected = filters.nameSort == NameSortOption.A_Z,
                onSelect = { onChange(filters.copy(nameSort = NameSortOption.A_Z)) }
            )
            RadioRow(
                label = "Z–A",
                selected = filters.nameSort == NameSortOption.Z_A,
                onSelect = { onChange(filters.copy(nameSort = NameSortOption.Z_A)) }
            )

            Spacer(Modifier.height(16.dp))

            Text("Type", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            TypeChipsAnd(
                selected = filters.typesAnd,
                onToggle = { tag ->
                    val next = filters.typesAnd.toMutableSet().apply {
                        if (contains(tag)) remove(tag) else add(tag)
                    }
                    onChange(filters.copy(typesAnd = next))
                }
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Favourite", modifier = Modifier.weight(1f))
                Switch(
                    checked = filters.favouriteOnly,
                    onCheckedChange = { onChange(filters.copy(favouriteOnly = it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pinned", modifier = Modifier.weight(1f))
                Switch(
                    checked = filters.pinnedOnly,
                    onCheckedChange = { onChange(filters.copy(pinnedOnly = it)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Special Date", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = "From Date",
                    value = filters.specialFrom,
                    placeholder = "dd/mm/yyyy",
                    onValueChange = { onChange(filters.copy(specialFrom = it)) },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "To Date",
                    value = filters.specialTo,
                    placeholder = "dd/mm/yyyy",
                    onValueChange = { onChange(filters.copy(specialTo = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(22.dp))
        }

        // --- Fixed Bottom Row (Apply + Reset) ---
        Divider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Reset Filter")
            }

            Button(
                onClick = onApply,
                enabled = applyEnabled,
                modifier = Modifier.weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text("Apply Filter")
            }
        }
    }
}

@Composable
private fun DateSortRadio(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun TypeChipsAnd(
    selected: Set<TagType>,
    onToggle: (TagType) -> Unit
) {
    Column {
        TagType.fixedOrder.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { tag ->
                    FilterChip(
                        selected = selected.contains(tag),
                        onClick = { onToggle(tag) },
                        label = { Text(tag.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Date picker field UI aligned with Add/Edit screen approach:
 * - OutlinedTextField (readOnly)
 * - DatePickerDialog
 * - dd/MM/yyyy formatting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var show by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { show = true }) {
                    Icon(Icons.Outlined.DateRange, contentDescription = "Pick date")
                }
            }
        )
    }

    if (show) {
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
