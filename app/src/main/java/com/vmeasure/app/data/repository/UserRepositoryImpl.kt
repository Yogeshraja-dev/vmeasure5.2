package com.vmeasure.app.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.vmeasure.app.core.util.DateTimeUtil
import com.vmeasure.app.data.db.dao.SectionDao
import com.vmeasure.app.data.db.dao.UserDao
import com.vmeasure.app.data.db.dao.UserWithTagsRow
//import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.domain.model.UserSummary
import com.vmeasure.app.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.room.withTransaction
import com.vmeasure.app.core.util.IdGenerator
import com.vmeasure.app.data.db.AppDatabase
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.data.db.entity.UserEntity
import com.vmeasure.app.feature.userform.SectionForm
import kotlinx.coroutines.delay

import androidx.sqlite.db.SimpleSQLiteQuery

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

    override fun pagingUserRows(search: String?, nameSortAsc: Boolean)
            : PagingSource<Int, UserWithTagsRow> {

        return userDao.pagingUsersWithTags(
            search = search?.trim()?.lowercase(),
            nameSortAsc = if (nameSortAsc) 1 else 0
        )
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

}
