package com.vmeasure.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Button
import androidx.compose.runtime.collectAsState

@Composable
fun SettingsScreen(vm: DriveSyncViewModel) {

    val ui = vm.uiState.collectAsState().value
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        vm.onAuthorizationActivityResult(res.data)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings (later: Export/Import Drive)")
    }

    Button(
        onClick = { vm.startExport(launcher) },
        enabled = !ui.isLoading
    ) { Text("Export to Google Drive") }

    Button(
        onClick = { vm.startImport(launcher) },
        enabled = !ui.isLoading
    ) { Text("Import from Google Drive") }

}

