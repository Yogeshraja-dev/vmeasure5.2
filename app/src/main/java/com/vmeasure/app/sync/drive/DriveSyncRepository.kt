package com.vmeasure.app.sync.drive

import androidx.room.withTransaction
import com.vmeasure.app.data.db.AppDatabase
import com.vmeasure.app.data.db.entity.MeasurementSectionEntity
import com.vmeasure.app.data.db.entity.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class DriveSyncRepository(
    private val db: AppDatabase,
    private val prefs: DrivePrefs,
    private val api: DriveApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val userDao = db.userDao()
    private val sectionDao = db.sectionDao()
    private val deletedUserDao = db.deletedUserDao()

    private val ROOT_FOLDER_NAME = "vmeasureback"
    private val FILE_NAME = "vmeasure_backup.json"

    suspend fun export(accessToken: String): Unit = withContext(Dispatchers.IO) {
        val rootId = ensureRootFolder(accessToken)
        val subFolderName = "vmeasureback_${timestamp()}"
        val subFolderId = api.findOrCreateFolder(accessToken, subFolderName, parentId = rootId)

        val users = userDao.getAllUsers()
        val allSections = sectionDao.getAllSections().groupBy { it.publicUserId }
        val deletedIds = deletedUserDao.getAllDeletedIds()
        val dto = BackupFileDto(
            exportedAtEpoch = System.currentTimeMillis(),
            users = users.map { u ->
                val sec = allSections[u.publicUserId].orEmpty().sortedBy { it.createdAtEpoch }
                u.toDto(sec)
            },
            deletedUserIds = deletedIds,
        )

        val bytes = json.encodeToString(BackupFileDto.serializer(), dto).toByteArray()
        api.uploadJson(accessToken, subFolderId, FILE_NAME, bytes) // never overwrites (new folder)
    }

    suspend fun importLatest(accessToken: String): Unit = withContext(Dispatchers.IO) {
        val rootId = ensureRootFolder(accessToken)

        val subfolders = api.listSubfoldersLatestFirst(accessToken, rootId)
        val latestFolderId = subfolders.firstOrNull()?.id ?: return@withContext

        val files = api.listFilesLatestFirst(accessToken, latestFolderId)
        val latestFileId = files.firstOrNull()?.id ?: return@withContext

        val bytes = api.downloadBytes(accessToken, latestFileId)
        val dto = json.decodeFromString(BackupFileDto.serializer(), bytes.toString(Charsets.UTF_8))

        db.withTransaction {
            for (u in dto.users) {
                mergeUserAndSections(u)
            }
        }
    }

    /**
     * Merge rules (your decisions):
     * - Match by userId (publicUserId)
     * - If user exists locally -> prefer imported user fields
     * - Never delete local user data
     * - Sections match by sectionId
     * - If imported has fewer sections -> never delete local
     * - If imported contains a new section -> add/replace by sectionId
     * - If sectionId collision occurs -> generate new id and append
     */
//    private suspend fun mergeUserAndSections(imported: UserDto) {
//        val existing = userDao.getByPublicId(imported.id)
//
//        val importedUserEntity = imported.toUserEntity()
//
//        if (existing == null) {
//            userDao.insertOrReplace(importedUserEntity)
//        } else {
//            // prefer imported data
//            userDao.update(importedUserEntity)
//        }
//
//        val localSections = sectionDao.getAllForUser(imported.id)
//        val localById = localSections.associateBy { it.sectionId }
//
//        imported.measurementSections.forEach { s ->
//            val local = localById[s.id]
//            if (local != null) {
//                // replace existing values with imported
//                sectionDao.update(s.toSectionEntity(imported.id))
//            } else {
//                // handle collision (in case sectionId is globally unique and already exists)
//                var sectionId = s.id
//                var tries = 0
//                while (sectionDao.countBySectionId(sectionId) > 0) {
//                    tries++
//                    sectionId = generate6DigitId()
//                    if (tries >= 10) break
//                }
//                sectionDao.insertOrReplace(s.toSectionEntity(imported.id, overrideSectionId = sectionId))
//            }
//        }
//        // never delete local extra sections (rule satisfied)
//    }

    private suspend fun mergeUserAndSections(imported: UserDto) {
        val localUser = userDao.getByPublicId(imported.id)

        // -----------------------------
        // 1) If imported says "deleted" -> mark local deleted and remove sections
        // -----------------------------
        if (imported.isDeleted) {
            val now = System.currentTimeMillis()
            val deletedAt = imported.deletedAtEpoch ?: now
            val editedAt = imported.editedAtEpoch ?: deletedAt

            if (localUser == null) {
                // Insert a tombstone user row so future imports don't resurrect this user.
                // NOTE: This assumes your UserEntity supports isDeleted/deletedAtEpoch.
                val tombstone = imported.toTombstoneUserEntity(
                    fallbackCreatedAt = imported.createdAtEpoch.takeIf { it > 0L } ?: deletedAt,
                    editedAtEpoch = editedAt,
                    deletedAtEpoch = deletedAt
                )
                userDao.insertOrReplace(tombstone)
            } else if (!localUser.isDeleted) {
                // Mark existing local user as deleted
                val tombstone = localUser.copy(
                    isDeleted = true,
                    deletedAtEpoch = deletedAt,
                    editedAtEpoch = editedAt,
                    isPinned = false,
                    isFavorite = false
                )
                userDao.insertOrReplace(tombstone)
            } else {
                // local is already deleted -> do nothing
            }

            // Keep DB light: remove sections for deleted user
            sectionDao.deleteAllForUser(imported.id)

            // IMPORTANT: stop further merging for deleted users
            return
        }

        // -----------------------------
        // 2) If local user is deleted -> NEVER resurrect
        // -----------------------------
        if (localUser != null && localUser.isDeleted) {
            return
        }

        // -----------------------------
        // 3) Active user: upsert user (prefer imported data)
        // -----------------------------
        val importedUserEntity = imported.toActiveUserEntity()

        if (localUser == null) {
            userDao.insertOrReplace(importedUserEntity)
        } else {
            // Prefer imported data (your merge rule)
            userDao.update(importedUserEntity.copy(createdAtEpoch = localUser.createdAtEpoch))
        }

        // -----------------------------
        // 4) Merge sections (same as your existing logic)
        // -----------------------------
        val localSections = sectionDao.getAllForUser(imported.id)
        val localById = localSections.associateBy { it.sectionId }

        imported.measurementSections.forEach { s ->
            val local = localById[s.id]
            if (local != null) {
                sectionDao.update(s.toSectionEntity(imported.id))
            } else {
                var sectionId = s.id
                var tries = 0
                while (sectionDao.countBySectionId(sectionId) > 0) {
                    tries++
                    sectionId = generate6DigitId()
                    if (tries >= 10) break
                }
                sectionDao.insertOrReplace(
                    s.toSectionEntity(
                        publicUserId = imported.id,
                        overrideSectionId = sectionId
                    )
                )
            }
        }
        // never delete local extra sections (rule satisfied)
    }

    private fun UserDto.toActiveUserEntity(): UserEntity {
        val safeName = name.ifBlank { "Unknown" }
        return UserEntity(
            publicUserId = id,
            name = safeName,
            nameNormalized = safeName.trim().lowercase(),

            dateOfBirth = dateOfBirth,
            specialDate = specialDate,
            specialDateEpoch = null,

            contactNumber = contactNumber,
            instagramId = instagramId,
            otherMedia = otherMedia,
            location = location,

            isFavorite = isFavorite,
            isPinned = isPinned,

            createdAtEpoch = createdAtEpoch.takeIf { it > 0L } ?: System.currentTimeMillis(),
            editedAtEpoch = editedAtEpoch,

            // ✅ active
            isDeleted = false,
            deletedAtEpoch = null
        )
    }

    /**
     * Create a minimal "tombstone" row so deleted users don't resurrect on other devices.
     */
    private fun UserDto.toTombstoneUserEntity(
        fallbackCreatedAt: Long,
        editedAtEpoch: Long,
        deletedAtEpoch: Long
    ): UserEntity {
        val safeName = name.ifBlank { "Deleted User" }
        return UserEntity(
            publicUserId = id,
            name = safeName,
            nameNormalized = safeName.trim().lowercase(),

            dateOfBirth = "",
            specialDate = "",
            specialDateEpoch = null,

            contactNumber = "",
            instagramId = "",
            otherMedia = "",
            location = "",

            isFavorite = false,
            isPinned = false,

            createdAtEpoch = fallbackCreatedAt,
            editedAtEpoch = editedAtEpoch,

            // ✅ deleted
            isDeleted = true,
            deletedAtEpoch = deletedAtEpoch
        )
    }

    private suspend fun ensureRootFolder(accessToken: String): String {
        val cached = prefs.getRootFolderId()
        if (!cached.isNullOrBlank()) return cached

        val id = api.findOrCreateFolder(accessToken, ROOT_FOLDER_NAME, parentId = null)
        prefs.setRootFolderId(id)
        return id
    }

    // -------------------- Mapping --------------------

    private fun UserEntity.toDto(sections: List<MeasurementSectionEntity>): UserDto {
        return UserDto(
            id = publicUserId,
            name = name,
            dateOfBirth = dateOfBirth ?: "",
            specialDate = specialDate ?: "",
            isFavorite = isFavorite,
            isPinned = isPinned,
            contactNumber = contactNumber ?: "",
            instagramId = instagramId ?: "",
            otherMedia = otherMedia ?: "",
            location = location ?: "",
            createdAtEpoch = createdAtEpoch,
            editedAtEpoch = editedAtEpoch,
            measurementSections = sections.map { it.toDto() }
        )
    }

    private fun MeasurementSectionEntity.toDto(): SectionDto {
        return SectionDto(
            id = sectionId,
            type = type,
            createdAtEpoch = createdAtEpoch,
            editedAtEpoch = editedAtEpoch,
            notes = notes,

            blouse_uBust = blouse_uBust,
            blouse_bust = blouse_bust,
            blouse_waist = blouse_waist,
            blouse_hip = blouse_hip,
            blouse_armhole = blouse_armhole,
            blouse_shoulder = blouse_shoulder,
            blouse_length = blouse_length,
            blouse_fNeck = blouse_fNeck,
            blouse_bNeck = blouse_bNeck,
            blouse_sleeveLength = blouse_sleeveLength,
            blouse_sleeveRound = blouse_sleeveRound,

            kurti_blouseCut = kurti_blouseCut,
            kurti_uBust = kurti_uBust,
            kurti_bust = kurti_bust,
            kurti_waist = kurti_waist,
            kurti_armhole = kurti_armhole,
            kurti_shoulder = kurti_shoulder,
            kurti_blouse = kurti_blouse,
            kurti_fNeck = kurti_fNeck,
            kurti_bNeck = kurti_bNeck,
            kurti_sleeveLength = kurti_sleeveLength,
            kurti_sleeveRound = kurti_sleeveRound,

            pant_waist = pant_waist,
            pant_hip = pant_hip,
            pant_length = pant_length,
            pant_thighRound = pant_thighRound,
            pant_kneeRound = pant_kneeRound,
            pant_bottom = pant_bottom,
            pant_inseam = pant_inseam,

            frock_waist = frock_waist,
            frock_frockLength = frock_frockLength,
            frock_yokeLength = frock_yokeLength,

            crop_blouseWaist = crop_blouseWaist,
            crop_blouseLength = crop_blouseLength,
            crop_skirtLength = crop_skirtLength,
            crop_waistLength = crop_waistLength,

            kids_chest = kids_chest,
            kids_waist = kids_waist,
            kids_length = kids_length,
            kids_shoulder = kids_shoulder,
            kids_sleeveLength = kids_sleeveLength,
            kids_pantLength = kids_pantLength,
            kids_pantWaist = kids_pantWaist
        )
    }

    private fun UserDto.toUserEntity(): UserEntity {
        // Adjust if your entity constructor differs.
        return UserEntity(
            publicUserId = id,
            name = name,
            nameNormalized = name.trim().lowercase(),
            dateOfBirth = dateOfBirth,
            specialDate = specialDate,
            specialDateEpoch = null,
            contactNumber = contactNumber,
            instagramId = instagramId,
            otherMedia = otherMedia,
            location = location,
            isFavorite = isFavorite,
            isPinned = isPinned,
            createdAtEpoch = createdAtEpoch,
            editedAtEpoch = editedAtEpoch,
            isDeleted = false,
            deletedAtEpoch = null,
        )
    }

    private fun SectionDto.toSectionEntity(publicUserId: String, overrideSectionId: String? = null): MeasurementSectionEntity {
        val sid = overrideSectionId ?: id
        return MeasurementSectionEntity(
            publicUserId = publicUserId,
            sectionId = sid,
            type = type,
            createdAtEpoch = createdAtEpoch,
            editedAtEpoch = editedAtEpoch,
            notes = notes,

            blouse_uBust = blouse_uBust,
            blouse_bust = blouse_bust,
            blouse_waist = blouse_waist,
            blouse_hip = blouse_hip,
            blouse_armhole = blouse_armhole,
            blouse_shoulder = blouse_shoulder,
            blouse_length = blouse_length,
            blouse_fNeck = blouse_fNeck,
            blouse_bNeck = blouse_bNeck,
            blouse_sleeveLength = blouse_sleeveLength,
            blouse_sleeveRound = blouse_sleeveRound,

            kurti_blouseCut = kurti_blouseCut,
            kurti_uBust = kurti_uBust,
            kurti_bust = kurti_bust,
            kurti_waist = kurti_waist,
            kurti_armhole = kurti_armhole,
            kurti_shoulder = kurti_shoulder,
            kurti_blouse = kurti_blouse,
            kurti_fNeck = kurti_fNeck,
            kurti_bNeck = kurti_bNeck,
            kurti_sleeveLength = kurti_sleeveLength,
            kurti_sleeveRound = kurti_sleeveRound,

            pant_waist = pant_waist,
            pant_hip = pant_hip,
            pant_length = pant_length,
            pant_thighRound = pant_thighRound,
            pant_kneeRound = pant_kneeRound,
            pant_bottom = pant_bottom,
            pant_inseam = pant_inseam,

            frock_waist = frock_waist,
            frock_frockLength = frock_frockLength,
            frock_yokeLength = frock_yokeLength,

            crop_blouseWaist = crop_blouseWaist,
            crop_blouseLength = crop_blouseLength,
            crop_skirtLength = crop_skirtLength,
            crop_waistLength = crop_waistLength,

            kids_chest = kids_chest,
            kids_waist = kids_waist,
            kids_length = kids_length,
            kids_shoulder = kids_shoulder,
            kids_sleeveLength = kids_sleeveLength,
            kids_pantLength = kids_pantLength,
            kids_pantWaist = kids_pantWaist
        )
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun generate6DigitId(): String = (100000 + Random.nextInt(900000)).toString()
}
