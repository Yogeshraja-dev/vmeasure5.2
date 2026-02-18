package com.vmeasure.app.sync.drive

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.driveDataStore by preferencesDataStore("drive_prefs")

class DrivePrefs(private val context: Context) {

    private val KEY_FOLDER_ID = stringPreferencesKey("drive_folder_id")

    suspend fun getFolderId(): String? {
        return context.driveDataStore.data.map { it[KEY_FOLDER_ID] }.first()
    }

    suspend fun setFolderId(id: String) {
        context.driveDataStore.edit { it[KEY_FOLDER_ID] = id }
    }
}
