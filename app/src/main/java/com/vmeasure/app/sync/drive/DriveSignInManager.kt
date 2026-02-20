package com.vmeasure.app.sync.drive

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

class DriveSignInManager(
    private val activity: Activity
) {
    companion object {
        const val REQUIRED_EMAIL = "andavarpalanivel55@gmail.com"

        private val DRIVE_FILE_SCOPE = Scope("https://www.googleapis.com/auth/drive.file")
    }

    private val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_FILE_SCOPE)
            .build()
        GoogleSignIn.getClient(activity, gso)
    }

    fun getSignedInEmail(): String? {
        return GoogleSignIn.getLastSignedInAccount(activity)?.email
    }

    fun getAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(activity)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    fun signOut() {
        signInClient.signOut()
    }

    fun handleSignInResult(data: Intent?): GoogleSignInAccount {
        return GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
    }

    fun isRequiredAccountSignedIn(): Boolean {
        return getSignedInEmail()?.equals(REQUIRED_EMAIL, ignoreCase = true) == true
    }
}
