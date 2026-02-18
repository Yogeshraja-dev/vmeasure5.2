package com.vmeasure.app.sync.drive

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
//import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DriveAuthManager(
    private val activity: Activity
) {
    private val authClient: AuthorizationClient = Identity.getAuthorizationClient(activity)

    /**
     * Requests/refreshes an OAuth access token for Drive.
     * Caller must use the returned authorizationIntent (if present) via ActivityResultLauncher.
     */
    suspend fun authorizeForDriveFileScope(
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): AuthorizationResult {
//        val scopes = listOf(Scope(DriveScopes.DRIVE_FILE))

        val scopes = listOf(
            Scope("https://www.googleapis.com/auth/drive.file")
        )
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(scopes)
            .build()

        return suspendCancellableCoroutine { cont ->
            authClient.authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        // Need user interaction (consent/account picker)
                        val intentSender = result.pendingIntent!!.intentSender
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                        // result will come later through launcher callback
                        // so we don't resume here
                    } else {
                        // Token already available
                        cont.resume(result)
                    }
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    fun extractResultFromIntent(data: Intent?): AuthorizationResult {
        return authClient.getAuthorizationResultFromIntent(data)
    }
}
