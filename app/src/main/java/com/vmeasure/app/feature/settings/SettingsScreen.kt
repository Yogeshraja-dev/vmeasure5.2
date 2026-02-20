package com.vmeasure.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.*
//import androidx.activity.result.IntentSenderRequest
import androidx.compose.ui.unit.dp


@Composable
fun SettingsScreen(vm: DriveSyncViewModel) {
    val ui by vm.state.collectAsState()

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        vm.onAuthorizationResult(res.data)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        vm.onSignInResult(res.data, authLauncher)
    }


//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text("Settings (later: Export/Import Drive)")
//    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {

        Text("Google Drive Sync", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { vm.onSaveToDriveClicked(signInLauncher, authLauncher) },
            enabled = !ui.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save to G Drive") }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { vm.onImportFromDriveClicked(signInLauncher, authLauncher) },
            enabled = !ui.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Import from G Drive") }

        if (ui.isLoading) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                CircularProgressIndicator()
                Spacer(Modifier.width(12.dp))
                Text("Please wait...")
            }
        }

        ui.message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it)
        }
    }

}
