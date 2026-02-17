package com.vmeasure.app.data.repository

import androidx.paging.PagingSource
import com.vmeasure.app.core.util.DateTimeUtil
import com.vmeasure.app.data.db.dao.UserWithTagsRow
import com.vmeasure.app.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.vmeasure.app.core.util.IdGenerator
import com.vmeasure.app.data.db.AppDatabase
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.data.db.entity.UserEntity
import com.vmeasure.app.feature.userform.SectionForm
import kotlinx.coroutines.delay
import com.vmeasure.app.feature.userform.UserFormUiState
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.vmeasure.app.feature.lists.DateSortOption
import com.vmeasure.app.feature.lists.ListFilters
import com.vmeasure.app.feature.lists.NameSortOption

class UserRepositoryImpl(
//    private val userDao: UserDao,
//    private val sectionDao: SectionDao,
    private val db: AppDatabase
) : UserRepository {

    private val userDao = db.userDao()
    private val sectionDao = db.sectionDao()

//    override fun pagingUserSummaries(search: String?, nameSortAsc: Boolean): PagingSource<Int, UserSummary> {
//        val src = userDao.pagingUsersWithTags(
//            search = search?.trim()?.lowercase(),
//            nameSortAsc = if (nameSortAsc) 1 else 0
//        )
//        return MappingPagingSource(src)
//    }

//    override fun pagingUserSummaries(search: String?, nameSortAsc: Boolean): PagingSource<Int, UserSummary> {
//        val sql = StringBuilder()
//        val args = mutableListOf<Any>()
//
//        sql.append(
//            """
//        SELECT
//            u.publicUserId AS publicUserId,
//            u.name AS name,
//            u.nameNormalized AS nameNormalized,
//            u.isPinned AS isPinned,
//            u.isFavorite AS isFavorite,
//            u.createdAtEpoch AS createdAtEpoch,
//            GROUP_CONCAT(DISTINCT s.type) AS tagsCsv
//        FROM users u
//        LEFT JOIN measurement_sections s
//          ON s.publicUserId = u.publicUserId
//        """
//        )
//
//        if (!search.isNullOrBlank()) {
//            sql.append(
//                """
//            WHERE (
//                u.nameNormalized LIKE '%' || ? || '%'
//                OR u.contactNumber LIKE '%' || ? || '%'
//            )
//            """
//            )
//            val s = search.trim().lowercase()
//            args.add(s)
//            args.add(s)
//        }
//
//        sql.append(" GROUP BY u.publicUserId ")
//
//        sql.append(
//            """
//        ORDER BY
//            u.isPinned DESC,
//        """
//        )
//
//        if (nameSortAsc) {
//            sql.append(" u.nameNormalized ASC, ")
//        } else {
//            sql.append(" u.nameNormalized DESC, ")
//        }
//
//        sql.append(" u.createdAtEpoch ASC ")
//
//        val query = SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
//        val src = userDao.pagingUsersWithTagsObserved(query)
//        return MappingPagingSource(src)
//    }

    override suspend fun setPinned(publicUserId: String, pinned: Boolean) {
        withContext(Dispatchers.IO) {
            val user = userDao.getByPublicId(publicUserId) ?: return@withContext
            userDao.update(user.copy(isPinned = pinned))
        }
    }

    override suspend fun setFavorite(publicUserId: String, favorite: Boolean) {
        withContext(Dispatchers.IO) {
            val user = userDao.getByPublicId(publicUserId) ?: return@withContext
            userDao.update(user.copy(isFavorite = favorite))
        }
    }

    override suspend fun deleteUser(publicUserId: String) {
        withContext(Dispatchers.IO) {
            sectionDao.deleteAllForUser(publicUserId)
            userDao.deleteByPublicId(publicUserId)
        }
    }

    override suspend fun buildShareReport(publicUserId: String): String {
        return withContext(Dispatchers.IO) {
            val user = userDao.getByPublicId(publicUserId) ?: return@withContext "Customer not found."
            val sections = sectionDao.getAllForUser(publicUserId)

            fun line(label: String, value: String?): String? {
                val v = value?.trim().orEmpty()
                return if (v.isBlank()) null else "$label: $v"
            }

            val sb = StringBuilder()
            sb.appendLine("Vmeasure — Customer Details")
            sb.appendLine()
            sb.appendLine("Name: ${user.name}")
            sb.appendLine("Customer ID: ${user.publicUserId}")

            line("Date of Birth", user.dateOfBirth)?.let(sb::appendLine)
            line("Special Date", user.specialDate)?.let(sb::appendLine)

            sb.appendLine("Favourite: ${if (user.isFavorite) "Yes" else "No"}")
            sb.appendLine("Pinned: ${if (user.isPinned) "Yes" else "No"}")

            line("Contact Number", user.contactNumber)?.let(sb::appendLine)
            line("Instagram", user.instagramId)?.let(sb::appendLine)
            line("Other Media", user.otherMedia)?.let(sb::appendLine)
            line("Location", user.location)?.let(sb::appendLine)

            sb.appendLine()
            sb.appendLine("Measurements")
            sb.appendLine("-----------")

            if (sections.isEmpty()) {
                sb.appendLine("No measurement sections.")
                return@withContext sb.toString()
            }

            // Group by type, preserve creation order within each group
            val grouped = sections.groupBy { it.type }

            grouped.forEach { (type, list) ->
                sb.appendLine()
                sb.appendLine(type)

                list.sortedBy { it.createdAtEpoch }.forEachIndexed { idx, sec ->
                    val header = "Section ${idx + 1} — ${DateTimeUtil.formatDateTime(sec.createdAtEpoch)}"
                    sb.appendLine(header)

                    val fields = extractNonEmptyFields(sec)
                    if (fields.isEmpty()) {
                        sb.appendLine("  (No values)")
                    } else {
                        fields.forEach { (k, v) ->
                            sb.appendLine("  $k: $v")
                        }
                    }
                    sb.appendLine()
                }
            }

            sb.toString().trimEnd()
        }
    }

    private fun extractNonEmptyFields(sec: MeasurementSectionEntity): List<Pair<String, String>> {
        fun add(list: MutableList<Pair<String, String>>, label: String, value: String?) {
            val v = value?.trim().orEmpty()
            if (v.isNotBlank()) list.add(label to v)
        }

        val out = mutableListOf<Pair<String, String>>()

        // Shared
        add(out, "Notes", sec.notes)

        when (sec.type.trim()) {
            "Blouse" -> {
                add(out, "U Bust", sec.blouse_uBust)
                add(out, "Bust", sec.blouse_bust)
                add(out, "Waist", sec.blouse_waist)
                add(out, "Hip", sec.blouse_hip)
                add(out, "Armhole", sec.blouse_armhole)
                add(out, "Shoulder", sec.blouse_shoulder)
                add(out, "Length", sec.blouse_length)
                add(out, "F Neck", sec.blouse_fNeck)
                add(out, "B Neck", sec.blouse_bNeck)
                add(out, "Sleeve Length", sec.blouse_sleeveLength)
                add(out, "Sleeve Round", sec.blouse_sleeveRound)
            }
            "Kurti" -> {
                add(out, "Blouse Cut", sec.kurti_blouseCut)
                add(out, "U Bust", sec.kurti_uBust)
                add(out, "Bust", sec.kurti_bust)
                add(out, "Waist", sec.kurti_waist)
                add(out, "Armhole", sec.kurti_armhole)
                add(out, "Shoulder", sec.kurti_shoulder)
                add(out, "Blouse", sec.kurti_blouse)
                add(out, "F Neck", sec.kurti_fNeck)
                add(out, "B Neck", sec.kurti_bNeck)
                add(out, "Sleeve Length", sec.kurti_sleeveLength)
                add(out, "Sleeve Round", sec.kurti_sleeveRound)
            }
            "Pant" -> {
                add(out, "Waist", sec.pant_waist)
                add(out, "Hip", sec.pant_hip)
                add(out, "Length", sec.pant_length)
                add(out, "Thigh Round", sec.pant_thighRound)
                add(out, "Knee Round", sec.pant_kneeRound)
                add(out, "Bottom", sec.pant_bottom)
                add(out, "Inseam", sec.pant_inseam)
            }
            "Frock" -> {
                add(out, "Waist", sec.frock_waist)
                add(out, "Frock Length", sec.frock_frockLength)
                add(out, "Yoke Length", sec.frock_yokeLength)
            }
            "Crop Blouse and Skirt" -> {
                add(out, "Blouse Waist", sec.crop_blouseWaist)
                add(out, "Blouse Length", sec.crop_blouseLength)
                add(out, "Skirt Length", sec.crop_skirtLength)
                add(out, "Waist Length", sec.crop_waistLength)
            }
            "Kids Boy" -> {
                add(out, "Chest", sec.kids_chest)
                add(out, "Waist", sec.kids_waist)
                add(out, "Length", sec.kids_length)
                add(out, "Shoulder", sec.kids_shoulder)
                add(out, "Sleeve Length", sec.kids_sleeveLength)
                add(out, "Pant Length", sec.kids_pantLength)
                add(out, "Pant Waist", sec.kids_pantWaist)
            }
            else -> {
                // Unknown type: still share notes if present
            }
        }

        return out
    }

//    override fun pagingUserRows(search: String?, nameSortAsc: Boolean)
//            : PagingSource<Int, UserWithTagsRow> {
//
//        return userDao.pagingUsersWithTags(
//            search = search?.trim()?.lowercase(),
//            nameSortAsc = if (nameSortAsc) 1 else 0
//        )
//    }

    override fun pagingUserRows(search: String?, filters: ListFilters): PagingSource<Int, UserWithTagsRow> {
        val sql = StringBuilder()
        val args = mutableListOf<Any>()

        sql.append(
            """
        SELECT 
            u.publicUserId AS publicUserId,
            u.name AS name,
            u.nameNormalized AS nameNormalized,
            u.isPinned AS isPinned,
            u.isFavorite AS isFavorite,
            u.createdAtEpoch AS createdAtEpoch,
            u.editedAtEpoch AS editedAtEpoch,
            GROUP_CONCAT(DISTINCT s.type) AS tagsCsv
        FROM users u
        LEFT JOIN measurement_sections s
          ON s.publicUserId = u.publicUserId
        """.trimIndent()
        )

        val where = mutableListOf<String>()

        // Search: name OR contact number, case-insensitive for nameNormalized
        if (!search.isNullOrBlank()) {
            where += """
            (
              u.nameNormalized LIKE '%' || ? || '%'
              OR u.contactNumber LIKE '%' || ? || '%'
            )
        """.trimIndent()
            val q = search.trim().lowercase()
            args += q
            args += q
        }

        if (filters.favouriteOnly) where += "u.isFavorite = 1"
        if (filters.pinnedOnly) where += "u.isPinned = 1"

        // --- Special Date filter (specialDateEpoch) ---
        val specialFromEpoch = DateTimeUtil.parseDateToEpochDayStartOrNull(filters.specialFrom)
        val specialToEpoch = when {
            filters.specialTo.trim().isNotEmpty() -> DateTimeUtil.parseDateToEpochDayEndOrNull(filters.specialTo)
            specialFromEpoch != null -> DateTimeUtil.nowEpochMillis() // To empty => today
            else -> null
        }

        if (specialFromEpoch != null) {
            where += "u.specialDateEpoch IS NOT NULL AND u.specialDateEpoch >= ?"
            args += specialFromEpoch
        }
        if (specialToEpoch != null) {
            where += "u.specialDateEpoch IS NOT NULL AND u.specialDateEpoch <= ?"
            args += specialToEpoch
        }

        // --- Custom Edited Date filter (editedAtEpoch) ---
        // Applies ONLY when dateSort = CUSTOM_EDITED_DATE
        val editedFromEpoch =
            if (filters.dateSort == DateSortOption.CUSTOM_EDITED_DATE)
                DateTimeUtil.parseDateToEpochDayStartOrNull(filters.editedFrom)
            else null

        val editedToEpoch = when {
            filters.dateSort != DateSortOption.CUSTOM_EDITED_DATE -> null
            filters.editedTo.trim().isNotEmpty() -> DateTimeUtil.parseDateToEpochDayEndOrNull(filters.editedTo)
            editedFromEpoch != null -> DateTimeUtil.nowEpochMillis() // To empty => today
            else -> null
        }

        if (filters.dateSort == DateSortOption.CUSTOM_EDITED_DATE) {
            if (editedFromEpoch != null) {
                where += "u.editedAtEpoch IS NOT NULL AND u.editedAtEpoch >= ?"
                args += editedFromEpoch
            }
            if (editedToEpoch != null) {
                where += "u.editedAtEpoch IS NOT NULL AND u.editedAtEpoch <= ?"
                args += editedToEpoch
            }
        }

        if (where.isNotEmpty()) {
            sql.append(" WHERE ")
            sql.append(where.joinToString(" AND "))
        }

        sql.append(" GROUP BY u.publicUserId ")

        // Type AND filter: user must have ALL selected types
        if (filters.typesAnd.isNotEmpty()) {
            val selected = filters.typesAnd.map { it.displayName }
            val placeholders = selected.joinToString(",") { "?" }
            sql.append(
                """
            HAVING COUNT(DISTINCT CASE WHEN s.type IN ($placeholders) THEN s.type END) = ?
            """.trimIndent()
            )
//            args += selected
//            args += selected.size
            args.addAll(selected)
            args.add(selected.size)
        }

        // ORDER BY
        // Pinned always on top
        sql.append(" ORDER BY u.isPinned DESC, ")

        when (filters.dateSort) {
            DateSortOption.RECENT_EDITED_DATE -> {
                sql.append(" COALESCE(u.editedAtEpoch, u.createdAtEpoch) DESC, ")
            }
            DateSortOption.LAST_UPDATED_DATE -> {
                // opposite order compared to RECENT_EDITED_DATE
                sql.append(" COALESCE(u.editedAtEpoch, u.createdAtEpoch) ASC, ")
            }
            DateSortOption.CUSTOM_EDITED_DATE -> {
                // within custom range, show newest first
                sql.append(" COALESCE(u.editedAtEpoch, u.createdAtEpoch) DESC, ")
            }
        }

        // Date sort – all options sort by editedAt desc behavior
        // IMPORTANT FIX: COALESCE makes new users (editedAt null) appear as recent by createdAt.
        sql.append(" COALESCE(u.editedAtEpoch, u.createdAtEpoch) DESC, ")

        // Name sort
        when (filters.nameSort) {
            NameSortOption.A_Z -> sql.append(" u.nameNormalized ASC, ")
            NameSortOption.Z_A -> sql.append(" u.nameNormalized DESC, ")
        }

        // Tie-breaker: createdAt (your rule)
        sql.append(" u.createdAtEpoch ASC ")

        val query = SimpleSQLiteQuery(sql.toString(), args.toTypedArray())
        return userDao.pagingUsersWithTagsObserved(query)
    }

//    private class MappingPagingSource(
//        private val delegate: PagingSource<Int, UserWithTagsRow>
//    ) : PagingSource<Int, UserSummary>() {
//
//        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, UserSummary> {
//            return when (val res = delegate.load(params)) {
//                is LoadResult.Page -> {
//                    val mapped = res.data.map { row ->
//                        val tags = row.tagsCsv
//                            ?.split(",")
//                            ?.map { it.trim() }
//                            ?.filter { it.isNotEmpty() }
//                            ?: emptyList()
//
//                        UserSummary(
//                            publicUserId = row.publicUserId,
//                            name = row.name,
//                            isPinned = row.isPinned,
//                            isFavorite = row.isFavorite,
//                            createdAtEpoch = row.createdAtEpoch,
//                            tags = tags
//                        )
//                    }
//                    LoadResult.Page(
//                        data = mapped,
//                        prevKey = res.prevKey,
//                        nextKey = res.nextKey
//                    )
//                }
//                is LoadResult.Error -> LoadResult.Error(res.throwable)
//                is LoadResult.Invalid -> LoadResult.Invalid()
//            }
//        }
//
//        override fun getRefreshKey(state: PagingState<Int, UserSummary>): Int? = null
//    }

    override suspend fun createUser(
        name: String,
        dateOfBirth: String,
        specialDate: String,
        isFavorite: Boolean,
        isPinned: Boolean,
        contactNumber: String,
        instagramId: String,
        otherMedia: String,
        location: String,
        sections: List<SectionForm>
    ): String {
        return withContext(Dispatchers.IO) {
            db.withTransaction {
                val now = DateTimeUtil.nowEpochMillis()

                // Generate unique publicUserId (retry on collision)
                var publicUserId: String
                while (true) {
                    publicUserId = IdGenerator.random6Digits()
                    val exists = userDao.getByPublicId(publicUserId) != null
                    if (!exists) break
                }

                val trimmedName = name.trim()
                val normalizedName = trimmedName.lowercase()

                val specialEpoch = com.vmeasure.app.core.util.DateTimeUtil.parseDateToEpochDayStart(specialDate)

                val user = UserEntity(
                    publicUserId = publicUserId,
                    name = trimmedName,
                    nameNormalized = normalizedName,
                    dateOfBirth = dateOfBirth.trim(),
                    specialDate = specialDate.trim(),
                    specialDateEpoch = specialEpoch,
                    isFavorite = isFavorite,
                    isPinned = isPinned,
                    contactNumber = contactNumber.trim(),
                    instagramId = instagramId.trim(),
                    otherMedia = otherMedia.trim(),
                    location = location.trim(),
                    createdAtEpoch = now,
                    editedAtEpoch = null
                )

                userDao.insert(user)

                // Insert sections (each row separately)
                sections.forEach { sf ->
                    val sectionId = ensureUniqueSectionId(publicUserId)
                    val secNow = DateTimeUtil.nowEpochMillis()

                    val entity = mapSectionFormToEntity(
                        publicUserId = publicUserId,
                        sectionId = sectionId,
                        createdAtEpoch = secNow,
                        sf = sf
                    )

                    sectionDao.insert(entity)
                }

                publicUserId
            }
        }
    }

    private suspend fun ensureUniqueSectionId(publicUserId: String): String {
        while (true) {
            val sid = IdGenerator.random6Digits()
            // composite unique (publicUserId, sectionId) is enforced in DB,
            // but we also try to avoid collisions proactively
            val existing = sectionDao.getAllForUser(publicUserId).any { it.sectionId == sid }
            if (!existing) return sid
            delay(5)
        }
    }

    private fun mapSectionFormToEntity(
        publicUserId: String,
        sectionId: String,
        createdAtEpoch: Long,
        sf: SectionForm
    ): MeasurementSectionEntity {
        // Notes shared; rest depends on type
        return when (sf.type) {
            "Blouse" -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes,
                blouse_uBust = sf.values["U Bust"],
                blouse_bust = sf.values["Bust"],
                blouse_waist = sf.values["Waist"],
                blouse_hip = sf.values["Hip"],
                blouse_armhole = sf.values["Armhole"],
                blouse_shoulder = sf.values["Shoulder"],
                blouse_length = sf.values["Length"],
                blouse_fNeck = sf.values["F Neck"],
                blouse_bNeck = sf.values["B Neck"],
                blouse_sleeveLength = sf.values["Sleeve Length"],
                blouse_sleeveRound = sf.values["Sleeve Round"]
            )

            "Kurti" -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes,
                kurti_blouseCut = sf.values["Blouse Cut"],
                kurti_uBust = sf.values["U Bust"],
                kurti_bust = sf.values["Bust"],
                kurti_waist = sf.values["Waist"],
                kurti_armhole = sf.values["Armhole"],
                kurti_shoulder = sf.values["Shoulder"],
                kurti_blouse = sf.values["Blouse"],
                kurti_fNeck = sf.values["F Neck"],
                kurti_bNeck = sf.values["B Neck"],
                kurti_sleeveLength = sf.values["Sleeve Length"],
                kurti_sleeveRound = sf.values["Sleeve Round"]
            )

            "Pant" -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes,
                pant_waist = sf.values["Waist"],
                pant_hip = sf.values["Hip"],
                pant_length = sf.values["Length"],
                pant_thighRound = sf.values["Thigh Round"],
                pant_kneeRound = sf.values["Knee Round"],
                pant_bottom = sf.values["Bottom"],
                pant_inseam = sf.values["Inseam"]
            )

            "Frock" -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes,
                frock_waist = sf.values["Waist"],
                frock_frockLength = sf.values["Frock Length"],
                frock_yokeLength = sf.values["Yoke Length"]
            )

            "Crop Blouse and Skirt" -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes,
                crop_blouseWaist = sf.values["Blouse Waist"],
                crop_blouseLength = sf.values["Blouse Length"],
                crop_skirtLength = sf.values["Skirt Length"],
                crop_waistLength = sf.values["Waist Length"]
            )

            "Kids Boy" -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes,
                kids_chest = sf.values["Chest"],
                kids_waist = sf.values["Waist"],
                kids_length = sf.values["Length"],
                kids_shoulder = sf.values["Shoulder"],
                kids_sleeveLength = sf.values["Sleeve Length"],
                kids_pantLength = sf.values["Pant Length"],
                kids_pantWaist = sf.values["Pant Waist"]
            )

            else -> MeasurementSectionEntity(
                publicUserId = publicUserId,
                sectionId = sectionId,
                type = sf.type,
                createdAtEpoch = createdAtEpoch,
                editedAtEpoch = null,
                notes = sf.notes
            )
        }
    }

    override suspend fun loadUserWithSections(publicUserId: String): Pair<UserEntity, List<MeasurementSectionEntity>> {
        return withContext(Dispatchers.IO) {
            val user = userDao.getByPublicId(publicUserId)
                ?: throw IllegalStateException("User not found")

            val sections = sectionDao.getAllForUser(publicUserId)
            user to sections
        }
    }

    override suspend fun saveUserEdits(
        publicUserId: String,
        originalUser: UserEntity,
        originalSections: List<MeasurementSectionEntity>,
        updatedForm: UserFormUiState,
        updatedSections: List<SectionForm>
    ) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val now = DateTimeUtil.nowEpochMillis()

                // --- USER change detection ---
                val newName = updatedForm.name.trim()
                val newNameNorm = newName.lowercase()
                val newDob = updatedForm.dateOfBirth.trim()
                val newSpecial = updatedForm.specialDate.trim()
                val newSpecialEpoch = DateTimeUtil.parseDateToEpochDayStart(newSpecial)
                val newContact = updatedForm.contactNumber.trim()
                val newIg = updatedForm.instagramId.trim()
                val newOther = updatedForm.otherMedia.trim()
                val newLoc = updatedForm.location.trim()
                val newFav = updatedForm.isFavorite
                val newPin = updatedForm.isPinned

                val userChangedExceptPinFav =
                    (originalUser.name.trim() != newName) ||
                            (originalUser.dateOfBirth.trim() != newDob) ||
                            (originalUser.specialDate.trim() != newSpecial) ||
                            (originalUser.contactNumber.trim() != newContact) ||
                            (originalUser.instagramId.trim() != newIg) ||
                            (originalUser.otherMedia.trim() != newOther) ||
                            (originalUser.location.trim() != newLoc)

                val shouldUpdateUserEditedAt = userChangedExceptPinFav

                val updatedUser = originalUser.copy(
                    name = newName,
                    nameNormalized = newNameNorm,
                    dateOfBirth = newDob,
                    specialDate = newSpecial,
                    specialDateEpoch = newSpecialEpoch,
                    contactNumber = newContact,
                    instagramId = newIg,
                    otherMedia = newOther,
                    location = newLoc,
                    isFavorite = newFav,
                    isPinned = newPin,
                    editedAtEpoch = if (shouldUpdateUserEditedAt) now else originalUser.editedAtEpoch
                )
                userDao.update(updatedUser)

                // --- SECTIONS diff rules ---
                val originalById = originalSections.associateBy { it.sectionId }

                val updatedExisting = updatedSections.filter { it.sectionId != null }
                val updatedExistingIds = updatedExisting.map { it.sectionId!! }.toSet()

                // Rule: do not delete sections that exist locally but are missing in updated list? (In Edit flow, user can delete sections)
                // Your spec: Delete Section removes that section. So here we DO delete those removed in UI.
                val removedIds = originalById.keys - updatedExistingIds
                removedIds.forEach { removedId ->
                    sectionDao.deleteByUserAndSectionId(publicUserId, removedId)
                }

                // Insert new sections (duplicates/new in edit)
                val newOnes = updatedSections.filter { it.sectionId == null }
                newOnes.forEach { sf ->
                    val newSectionId = ensureUniqueSectionId(publicUserId)
                    val createdAt = sf.createdAtEpoch
                    val entity = mapSectionFormToEntity(
                        publicUserId = publicUserId,
                        sectionId = newSectionId,
                        createdAtEpoch = createdAt,
                        sf = sf
                    )
                    sectionDao.insert(entity)
                }

                // Update existing sections and set editedAt only if changed
                updatedExisting.forEach { sf ->
                    val id = sf.sectionId!!
                    val old = originalById[id] ?: return@forEach

                    val newEntityNoEditedAt = mapSectionFormToEntity(
                        publicUserId = publicUserId,
                        sectionId = id,
                        createdAtEpoch = old.createdAtEpoch, // keep createdAt
                        sf = sf
                    ).copy(
                        pk = old.pk,
                        editedAtEpoch = old.editedAtEpoch
                    )

                    val changed = sectionChanged(old, newEntityNoEditedAt)
                    val finalEntity = if (changed) newEntityNoEditedAt.copy(editedAtEpoch = now) else newEntityNoEditedAt
                    sectionDao.update(finalEntity)
                }
            }
        }
    }

    private fun sectionChanged(old: MeasurementSectionEntity, newE: MeasurementSectionEntity): Boolean {
        // createdAt and ids ignored; compare content fields and type/notes
        return old.type != newE.type ||
                (old.notes ?: "") != (newE.notes ?: "") ||

                (old.blouse_uBust ?: "") != (newE.blouse_uBust ?: "") ||
                (old.blouse_bust ?: "") != (newE.blouse_bust ?: "") ||
                (old.blouse_waist ?: "") != (newE.blouse_waist ?: "") ||
                (old.blouse_hip ?: "") != (newE.blouse_hip ?: "") ||
                (old.blouse_armhole ?: "") != (newE.blouse_armhole ?: "") ||
                (old.blouse_shoulder ?: "") != (newE.blouse_shoulder ?: "") ||
                (old.blouse_length ?: "") != (newE.blouse_length ?: "") ||
                (old.blouse_fNeck ?: "") != (newE.blouse_fNeck ?: "") ||
                (old.blouse_bNeck ?: "") != (newE.blouse_bNeck ?: "") ||
                (old.blouse_sleeveLength ?: "") != (newE.blouse_sleeveLength ?: "") ||
                (old.blouse_sleeveRound ?: "") != (newE.blouse_sleeveRound ?: "") ||

                (old.kurti_blouseCut ?: "") != (newE.kurti_blouseCut ?: "") ||
                (old.kurti_uBust ?: "") != (newE.kurti_uBust ?: "") ||
                (old.kurti_bust ?: "") != (newE.kurti_bust ?: "") ||
                (old.kurti_waist ?: "") != (newE.kurti_waist ?: "") ||
                (old.kurti_armhole ?: "") != (newE.kurti_armhole ?: "") ||
                (old.kurti_shoulder ?: "") != (newE.kurti_shoulder ?: "") ||
                (old.kurti_blouse ?: "") != (newE.kurti_blouse ?: "") ||
                (old.kurti_fNeck ?: "") != (newE.kurti_fNeck ?: "") ||
                (old.kurti_bNeck ?: "") != (newE.kurti_bNeck ?: "") ||
                (old.kurti_sleeveLength ?: "") != (newE.kurti_sleeveLength ?: "") ||
                (old.kurti_sleeveRound ?: "") != (newE.kurti_sleeveRound ?: "") ||

                (old.pant_waist ?: "") != (newE.pant_waist ?: "") ||
                (old.pant_hip ?: "") != (newE.pant_hip ?: "") ||
                (old.pant_length ?: "") != (newE.pant_length ?: "") ||
                (old.pant_thighRound ?: "") != (newE.pant_thighRound ?: "") ||
                (old.pant_kneeRound ?: "") != (newE.pant_kneeRound ?: "") ||
                (old.pant_bottom ?: "") != (newE.pant_bottom ?: "") ||
                (old.pant_inseam ?: "") != (newE.pant_inseam ?: "") ||

                (old.frock_waist ?: "") != (newE.frock_waist ?: "") ||
                (old.frock_frockLength ?: "") != (newE.frock_frockLength ?: "") ||
                (old.frock_yokeLength ?: "") != (newE.frock_yokeLength ?: "") ||

                (old.crop_blouseWaist ?: "") != (newE.crop_blouseWaist ?: "") ||
                (old.crop_blouseLength ?: "") != (newE.crop_blouseLength ?: "") ||
                (old.crop_skirtLength ?: "") != (newE.crop_skirtLength ?: "") ||
                (old.crop_waistLength ?: "") != (newE.crop_waistLength ?: "") ||

                (old.kids_chest ?: "") != (newE.kids_chest ?: "") ||
                (old.kids_waist ?: "") != (newE.kids_waist ?: "") ||
                (old.kids_length ?: "") != (newE.kids_length ?: "") ||
                (old.kids_shoulder ?: "") != (newE.kids_shoulder ?: "") ||
                (old.kids_sleeveLength ?: "") != (newE.kids_sleeveLength ?: "") ||
                (old.kids_pantLength ?: "") != (newE.kids_pantLength ?: "") ||
                (old.kids_pantWaist ?: "") != (newE.kids_pantWaist ?: "")
    }

    suspend fun setUserPinned(publicUserId: String, pinned: Boolean) {
        userDao.setPinned(publicUserId, pinned)
    }

    suspend fun setUserFavorite(publicUserId: String, favorite: Boolean) {
        userDao.setFavorite(publicUserId, favorite)
    }


}
