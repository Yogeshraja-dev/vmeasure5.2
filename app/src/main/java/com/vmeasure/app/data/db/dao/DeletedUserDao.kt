package com.vmeasure.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vmeasure.app.data.db.entity.DeletedUserEntity

@Dao
interface DeletedUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeletedUserEntity)

    @Query("SELECT publicUserId FROM deleted_users")
    suspend fun getAllDeletedIds(): List<String>

    @Query("SELECT COUNT(*) FROM deleted_users WHERE publicUserId = :id")
    suspend fun exists(id: String): Int
}