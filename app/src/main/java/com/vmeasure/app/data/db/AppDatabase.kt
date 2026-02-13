package com.vmeasure.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vmeasure.app.data.db.dao.SectionDao
import com.vmeasure.app.data.db.dao.UserDao
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.data.db.entity.UserEntity

@Database(
    entities = [UserEntity::class, MeasurementSectionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sectionDao(): SectionDao
}
