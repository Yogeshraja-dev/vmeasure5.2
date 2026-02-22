package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_users")
data class DeletedUserEntity(
    @PrimaryKey val publicUserId: String,
    val deletedAtEpoch: Long
)