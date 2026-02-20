package com.vmeasure.app.feature.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vmeasure.app.sync.drive.DriveSignInManager
import com.vmeasure.app.sync.drive.DriveSyncRepository
import com.vmeasure.app.sync.drive.DriveTokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class DriveSyncState(
    val isLoading: Boolean = false,
    val message: String? = null
)

class DriveSyncViewModel(
    private val activity: Activity,
    private val repo: DriveSyncRepository
) : ViewModel() {

    private val signIn = DriveSignInManager(activity)
    private val tokenProvider = DriveTokenProvider(activity)

    private val _state = MutableStateFlow(DriveSyncState())
    val state = _state.asStateFlow()

    private var pendingAction: Action? = null

    enum class Action { EXPORT, IMPORT }

    fun onSaveToDriveClicked(signInLauncher: ActivityResultLauncher<Intent>, authLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        pendingAction = Action.EXPORT
        ensureSignedInThenAuthorize(signInLauncher, authLauncher)
    }

    fun onImportFromDriveClicked(signInLauncher: ActivityResultLauncher<Intent>, authLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        pendingAction = Action.IMPORT
        ensureSignedInThenAuthorize(signInLauncher, authLauncher)
    }

    private fun ensureSignedInThenAuthorize(signInLauncher: ActivityResultLauncher<Intent>, authLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        // Must be vmeasure@gmail.com
        if (!signIn.isRequiredAccountSignedIn()) {
            _state.value = DriveSyncState(isLoading = false, message = "Please sign in with ${DriveSignInManager.REQUIRED_EMAIL}")
            signInLauncher.launch(signIn.signInIntent())
            return
        }
        authorize(authLauncher)
    }

    private fun authorize(authLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            _state.value = DriveSyncState(isLoading = true, message = null)
            try {
                val result = tokenProvider.beginAuthorize(authLauncher)
                // If there is no resolution, token may already be present
                val token = result.accessToken
                if (!token.isNullOrBlank()) {
                    runDriveAction(token)
                }
            } catch (e: Exception) {
                _state.value = DriveSyncState(isLoading = false, message = "Failed: ${e.message}")
                pendingAction = null
            }
        }
    }

    fun onSignInResult(data: Intent?, authLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        try {
            signIn.handleSignInResult(data)
        } catch (e: Exception) {
            _state.value = DriveSyncState(isLoading = false, message = "Sign-in failed: ${e.message}")
            pendingAction = null
            return
        }

        if (!signIn.isRequiredAccountSignedIn()) {
            _state.value = DriveSyncState(isLoading = false, message = "Please sign in with ${DriveSignInManager.REQUIRED_EMAIL}")
            signIn.signOut()
            pendingAction = null
            return
        }

        authorize(authLauncher)
    }

    fun onAuthorizationResult(data: Intent?) {
        viewModelScope.launch {
            try {
                val result = tokenProvider.getResultFromIntent(data)
                val token = result.accessToken
                if (token.isNullOrBlank()) {
                    _state.value = DriveSyncState(isLoading = false, message = "Failed: No access token")
                    pendingAction = null
                    return@launch
                }
                runDriveAction(token)
            } catch (e: Exception) {
                _state.value = DriveSyncState(isLoading = false, message = "Failed: ${e.message}")
                pendingAction = null
            }
        }
    }

    private suspend fun runDriveAction(accessToken: String) {
        val action = pendingAction ?: return

        try {
            withTimeout(30_000) {
                when (action) {
                    Action.EXPORT -> repo.export(accessToken)
                    Action.IMPORT -> repo.importLatest(accessToken)
                }
            }
            _state.value = DriveSyncState(isLoading = false, message = "${action.name.lowercase().replaceFirstChar { it.uppercase() }} completed")
        } catch (e: Exception) {
            val msg = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                "Time out / Failed to ${action.name.lowercase()}"
            } else {
                "Failed to ${action.name.lowercase()}: ${e.message}"
            }
            _state.value = DriveSyncState(isLoading = false, message = msg)
        } finally {
            pendingAction = null
        }
    }
}
