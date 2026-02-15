package com.vmeasure.app.data.db.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.vmeasure.app.data.db.entity.UserEntity
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("DELETE FROM users WHERE publicUserId = :publicUserId")
    suspend fun deleteByPublicId(publicUserId: String)

//    @Query("SELECT * FROM users WHERE publicUserId = :publicUserId LIMIT 1")
//    suspend fun getByPublicId(publicUserId: String): UserEntity?

    @Query("SELECT * FROM users WHERE publicUserId = :publicUserId LIMIT 1")
    suspend fun getByPublicId(publicUserId: String): com.vmeasure.app.data.db.entity.UserEntity?

    /**
     * Base list query with pin-top and name sorting.
     * Search is applied by name/contact contains (case-insensitive using normalized fields).
     */
    @Query(
        """
        SELECT * FROM users
        WHERE (:search IS NULL OR :search = '' 
              OR nameNormalized LIKE '%' || :search || '%' 
              OR contactNumber LIKE '%' || :search || '%')
        ORDER BY 
            isPinned DESC,
            CASE WHEN :nameSortAsc = 1 THEN nameNormalized END ASC,
            CASE WHEN :nameSortAsc = 0 THEN nameNormalized END DESC,
            createdAtEpoch ASC
        """
    )
    fun pagingUsersBase(
        search: String?,
        nameSortAsc: Int
    ): PagingSource<Int, UserEntity>

    @Query(
        """
        SELECT * FROM users
        WHERE editedAtEpoch IS NOT NULL
        ORDER BY editedAtEpoch DESC, isPinned DESC, nameNormalized ASC, createdAtEpoch ASC
        """
    )
    fun pagingUsersByEditedDesc(): PagingSource<Int, UserEntity>

    @Query(
        """
        SELECT * FROM users
        WHERE editedAtEpoch IS NOT NULL
          AND editedAtEpoch BETWEEN :fromEpoch AND :toEpoch
        ORDER BY editedAtEpoch DESC, isPinned DESC, nameNormalized ASC, createdAtEpoch ASC
        """
    )
    fun pagingUsersByEditedRange(fromEpoch: Long, toEpoch: Long): PagingSource<Int, UserEntity>

    @Query(
        """
    SELECT 
        u.publicUserId AS publicUserId,
        u.name AS name,
        u.nameNormalized AS nameNormalized,
        u.isPinned AS isPinned,
        u.isFavorite AS isFavorite,
        u.createdAtEpoch AS createdAtEpoch,
        GROUP_CONCAT(DISTINCT s.type) AS tagsCsv
    FROM users u
    LEFT JOIN measurement_sections s
      ON s.publicUserId = u.publicUserId
    WHERE (
        :search IS NULL OR :search = '' 
        OR u.nameNormalized LIKE '%' || :search || '%'
        OR u.contactNumber LIKE '%' || :search || '%'
    )
    GROUP BY u.publicUserId
    ORDER BY
        u.isPinned DESC,
        CASE WHEN :nameSortAsc = 1 THEN u.nameNormalized END ASC,
        CASE WHEN :nameSortAsc = 0 THEN u.nameNormalized END DESC,
        u.createdAtEpoch ASC
    """
    )
    fun pagingUsersWithTags(
        search: String?,
        nameSortAsc: Int
    ): PagingSource<Int, UserWithTagsRow>

    @Query("SELECT * FROM users WHERE publicUserId = :publicUserId LIMIT 1")
    suspend fun getUserByPublicId(publicUserId: String): com.vmeasure.app.data.db.entity.UserEntity?

    @RawQuery(
        observedEntities = [
            com.vmeasure.app.data.db.entity.UserEntity::class,
            com.vmeasure.app.data.db.entity.MeasurementSectionEntity::class
        ]
    )
    fun pagingUsersWithTagsObserved(query: SupportSQLiteQuery): PagingSource<Int, UserWithTagsRow>

//    @Query("SELECT * FROM users WHERE publicUserId = :publicUserId LIMIT 1")
//
//    suspend fun getByPublicId(publicUserId: String): com.vmeasure.app.data.db.entity.UserEntity?

    @Query("""
    UPDATE users SET
      name = :name,
      nameNormalized = :nameNormalized,
      dateOfBirth = :dateOfBirth,
      specialDate = :specialDate,
      specialDateEpoch = :specialDateEpoch,
      contactNumber = :contactNumber,
      instagramId = :instagramId,
      otherMedia = :otherMedia,
      location = :location,
      isFavorite = :isFavorite,
      isPinned = :isPinned,
      editedAtEpoch = :editedAtEpoch
    WHERE publicUserId = :publicUserId
""")
    suspend fun updateAllFieldsByPublicId(
        publicUserId: String,
        name: String,
        nameNormalized: String,
        dateOfBirth: String,
        specialDate: String,
        specialDateEpoch: Long?,
        contactNumber: String,
        instagramId: String,
        otherMedia: String,
        location: String,
        isFavorite: Boolean,
        isPinned: Boolean,
        editedAtEpoch: Long?
    )

}
//
//listing page- pinned,
//        reflected in view or edit user screen.
//
//        unnpined in edit user screen,
//        not reflected in listing screen.
//
//        pinned in edit user screen,
//        reflected in listing screen.
//
//        unpinned in listing screen,
//        reflected in  view/edit screen.
//
//        -----
//        fav in listing,
//        reflected in view/edit
//
//        unfav in edit,
//        not reflcted in listing.
//
//
//        All the above solution is working. Thank you for that.
//        After I did testing, I found some bugs.
//        1. Pin/unpin activity in listing or view/edited screen not reflcted. Other edit data is reflected properly.
//        2. Fav/unfavourite activity in listing or view/edit screen not reflcted. Other edit data is reflected properly.
//        3. Filters-> Sort by name-> A-Z, Z-A is not working.
//
