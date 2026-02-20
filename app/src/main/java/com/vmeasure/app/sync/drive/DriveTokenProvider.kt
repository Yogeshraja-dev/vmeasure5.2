package com.vmeasure.app.sync.drive

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlin.coroutines.resumeWithException

class DriveTokenProvider(private val activity: Activity) {

    private val authClient: AuthorizationClient = Identity.getAuthorizationClient(activity)

    companion object {
        private val DRIVE_FILE_SCOPE = Scope("https://www.googleapis.com/auth/drive.file")
    }

    suspend fun beginAuthorize(
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(DRIVE_FILE_SCOPE))
            .build()

        val result = authClient.authorize(request).await()

        if (result.hasResolution()) {
            launcher.launch(
                IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
            )
        }
        return result
    }

    fun getResultFromIntent(data: Intent?): AuthorizationResult {
        return authClient.getAuthorizationResultFromIntent(data)
    }
}

/**
 * Minimal Task await helper (no extra dependency).
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) {} }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
