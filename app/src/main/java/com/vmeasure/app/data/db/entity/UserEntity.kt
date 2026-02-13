package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["publicUserId"], unique = true),
        Index(value = ["isPinned", "nameNormalized"]),
        Index(value = ["nameNormalized"]),
        Index(value = ["contactNumber"]),
        Index(value = ["isFavorite"]),
        Index(value = ["editedAtEpoch"]),
        Index(value = ["specialDateEpoch"]),
        Index(value = ["createdAtEpoch"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val pk: Long = 0L,

    // 6-digit public ID used for sync/merge
    val publicUserId: String,

    val name: String,
    // store a normalized copy for case-insensitive sorting/search
    val nameNormalized: String,

    // dd/MM/yyyy (as requested)
    val dateOfBirth: String,
    val specialDate: String,

    // For range filtering specialDate; null if specialDate empty/invalid
    val specialDateEpoch: Long?,

    val isFavorite: Boolean,
    val isPinned: Boolean,

    val contactNumber: String,
    val instagramId: String,
    val otherMedia: String,
    val location: String,

    val createdAtEpoch: Long,
    val editedAtEpoch: Long?
)
