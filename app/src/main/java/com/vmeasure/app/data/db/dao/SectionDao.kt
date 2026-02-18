package com.vmeasure.app.data.db.dao

import androidx.room.*
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity

@Dao
interface SectionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(section: MeasurementSectionEntity): Long

    @Update
    suspend fun update(section: MeasurementSectionEntity)

    @Query("DELETE FROM measurement_sections WHERE publicUserId = :publicUserId")
    suspend fun deleteAllForUser(publicUserId: String)

    @Query("DELETE FROM measurement_sections WHERE publicUserId = :publicUserId AND sectionId = :sectionId")
    suspend fun deleteByUserAndSectionId(publicUserId: String, sectionId: String)

    @Query(
        """
        SELECT * FROM measurement_sections
        WHERE publicUserId = :publicUserId
        ORDER BY createdAtEpoch ASC
        """
    )
    suspend fun getAllForUser(publicUserId: String): List<MeasurementSectionEntity>

    @Query(
        """
        SELECT DISTINCT type FROM measurement_sections
        WHERE publicUserId = :publicUserId
        """
    )
    suspend fun getTypesForUser(publicUserId: String): List<String>

    @Query("SELECT * FROM measurement_sections")
    suspend fun getAllSections(): List<com.vmeasure.app.data.db.entity.MeasurementSectionEntity>

//    @Query("SELECT * FROM measurement_sections WHERE publicUserId = :publicUserId")
//    suspend fun getAllForUser(publicUserId: String): List<com.vmeasure.app.data.db.entity.MeasurementSectionEntity>

    @Query("SELECT * FROM measurement_sections WHERE publicUserId = :publicUserId")
    suspend fun getAllForUserUnordered(publicUserId: String): List<MeasurementSectionEntity>


}
