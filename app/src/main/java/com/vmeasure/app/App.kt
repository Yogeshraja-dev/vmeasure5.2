package com.vmeasure.app

import android.app.Application
import androidx.room.Room
import com.vmeasure.app.data.db.AppDatabase
import com.vmeasure.app.sync.drive.DriveApiClient
import com.vmeasure.app.sync.drive.DrivePrefs
import com.vmeasure.app.sync.drive.DriveSyncRepository

class App : Application() {

    lateinit var db: AppDatabase
        private set

    val driveSyncRepo: DriveSyncRepository by lazy {
        DriveSyncRepository(
            db = db,
            prefs = DrivePrefs(this),
            api = DriveApiClient()
        )
    }

//    override fun onCreate() {
//        super.onCreate()
//        db = AppDatabase.getInstance(this) // use YOUR existing db init
//    }

    override fun onCreate() {
        super.onCreate()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "vmeasure.db"
        )
            .fallbackToDestructiveMigration() // OK for now; later we’ll add proper migrations
            .build()
    }
}
