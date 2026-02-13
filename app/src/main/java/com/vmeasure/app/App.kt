package com.vmeasure.app

import android.app.Application
import androidx.room.Room
import com.vmeasure.app.data.db.AppDatabase

class App : Application() {

    lateinit var db: AppDatabase
        private set

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
