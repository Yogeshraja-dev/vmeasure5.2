package com.vmeasure.app.debug

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

object SignDebug {
    fun logSigningSha1(context: Context) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                pi.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                val pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                pi.signatures
            }

            val md = MessageDigest.getInstance("SHA1")
            val sha1 = md.digest(signatures?.get(0)?.toByteArray())
                .joinToString(":") { "%02X".format(it) }

            Log.d("SIGN_DEBUG", "package=$pkg SHA1=$sha1")
        } catch (e: Exception) {
            Log.e("SIGN_DEBUG", "Failed to compute SHA1", e)
        }
    }
}