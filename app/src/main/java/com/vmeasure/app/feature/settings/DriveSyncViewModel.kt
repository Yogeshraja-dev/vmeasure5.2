package com.vmeasure.app.feature.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.vmeasure.app.sync.drive.DriveAuthManager
import com.vmeasure.app.sync.drive.DriveSyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DriveSyncUiState(
    val isLoading: Boolean = false,
    val message: String? = null
)

class DriveSyncViewModel(
    private val activity: Activity,
    private val driveRepo: DriveSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriveSyncUiState())
    val uiState = _uiState.asStateFlow()

    private val auth = DriveAuthManager(activity)

    private var pendingAction: PendingAction? = null

    private enum class PendingAction { EXPORT, IMPORT }

    fun startExport(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        pendingAction = PendingAction.EXPORT
        authorize(launcher)
    }

    fun startImport(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        pendingAction = PendingAction.IMPORT
        authorize(launcher)
    }

    private fun authorize(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            _uiState.value = DriveSyncUiState(isLoading = true, message = null)
            try {
                // If already authorized, this returns immediately.
                val result = auth.authorizeForDriveFileScope(launcher)
                // If we got here with a token, execute
                executeWithAuth(result)
            } catch (e: Exception) {
                _uiState.value = DriveSyncUiState(isLoading = false, message = e.message)
            }
        }
    }

    fun onAuthorizationActivityResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val result = auth.extractResultFromIntent(data)
                executeWithAuth(result)
            } catch (e: Exception) {
                _uiState.value = DriveSyncUiState(isLoading = false, message = e.message)
            }
        }
    }

    private suspend fun executeWithAuth(result: AuthorizationResult) {
        val token = result.accessToken
        if (token.isNullOrBlank()) {
            _uiState.value = DriveSyncUiState(isLoading = false, message = "No access token returned.")
            return
        }

        try {
            when (pendingAction) {
                PendingAction.EXPORT -> {
                    driveRepo.exportToDrive(token)
                    _uiState.value = DriveSyncUiState(isLoading = false, message = "Export completed")
                }
                PendingAction.IMPORT -> {
                    driveRepo.importLatestFromDrive(token)
                    _uiState.value = DriveSyncUiState(isLoading = false, message = "Import completed")
                }
                null -> {
                    _uiState.value = DriveSyncUiState(isLoading = false, message = null)
                }
            }
        } catch (e: Exception) {
            _uiState.value = DriveSyncUiState(isLoading = false, message = e.message)
        } finally {
            pendingAction = null
        }
    }
}
