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
    private val drivePrefs: DrivePrefs,
    private val driveApi: DriveApiClient,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val userDao = db.userDao()
    private val sectionDao = db.sectionDao()

    private val folderName = "VmeasureBackups"

    suspend fun exportToDrive(accessToken: String): String = withContext(Dispatchers.IO) {
        val folderId = ensureFolder(accessToken)

        val users = userDao.getAllUsers()
        val sections = sectionDao.getAllSections().groupBy { it.publicUserId }

        val dto = DriveBackupDto(
            exportedAtEpoch = System.currentTimeMillis(),
            users = users.map { u ->
                val userSections = sections[u.publicUserId].orEmpty().sortedBy { it.createdAtEpoch }
                u.toBackupDto(userSections)
            }
        )

        val bytes = json.encodeToString(DriveBackupDto.serializer(), dto).toByteArray()
        val fileName = "vmeasure_backup_${timestampForName()}.json"

        // Do NOT overwrite — always create new file
        driveApi.uploadJsonFile(accessToken, folderId, fileName, bytes)
    }

    suspend fun importLatestFromDrive(accessToken: String) = withContext(Dispatchers.IO) {
        val folderId = ensureFolder(accessToken)
        val latestId = driveApi.listLatestBackupFileId(accessToken, folderId) ?: return@withContext

        val bytes = driveApi.downloadFileBytes(accessToken, latestId)
        val dto = json.decodeFromString(DriveBackupDto.serializer(), bytes.toString(Charsets.UTF_8))

        db.withTransaction {
            for (u in dto.users) {
                upsertUserAndSectionsFromImport(u)
            }
        }
    }

    private suspend fun ensureFolder(accessToken: String): String {
        val cached = drivePrefs.getFolderId()
        if (!cached.isNullOrBlank()) return cached

        val folderId = driveApi.findOrCreateFolder(accessToken, folderName)
        drivePrefs.setFolderId(folderId)
        return folderId
    }

    private suspend fun upsertUserAndSectionsFromImport(imported: UserBackupDto) {
        val existing = userDao.getByPublicId(imported.id)

//        val importedUserEntity = imported.toUserEntity(existingInternalId = existing?.internalId)
        val importedUserEntity = imported.toUserEntity()


        if (existing == null) {
            userDao.insert(importedUserEntity)
        } else {
            // Prefer imported user data (your rule)
            userDao.update(importedUserEntity)
        }

        // Sections merge:
        // - match by sectionId
        // - update existing sections with imported values
        // - insert missing imported sections
        // - never delete local sections not in import
        val localSections = sectionDao.getAllForUser(imported.id)
        val localBySectionId = localSections.associateBy { it.sectionId }

        for (s in imported.measurementSections) {
            val local = localBySectionId[s.id]
            if (local != null) {
                // Update fields of existing row (prefer imported)
//                sectionDao.update(s.toSectionEntity(imported.id, local.internalId))
                sectionDao.update(
                    s.toSectionEntity(imported.id)
                )
            } else {
                // Insert new. If collision occurs, generate new sectionId and append.
                var tries = 0
                var sectionId = s.id
                while (true) {
                    try {
//                        sectionDao.insert(s.toSectionEntity(imported.id, existingInternalId = null, overrideSectionId = sectionId))
                        sectionDao.insert(
                            s.toSectionEntity(
                                publicUserId = imported.id,
                                overrideSectionId = sectionId
                            )
                        )

                        break
                    } catch (e: Exception) {
                        // collision -> append with a fresh id
                        tries++
                        if (tries >= 5) throw e
                        sectionId = generate6DigitId()
                    }
                }
            }
        }
    }

    private fun UserEntity.toBackupDto(sections: List<MeasurementSectionEntity>): UserBackupDto {
        return UserBackupDto(
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
            measurementSections = sections.map { it.toBackupDto() }
        )
    }

    private fun MeasurementSectionEntity.toBackupDto(): SectionBackupDto {
        return SectionBackupDto(
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

//    private fun UserBackupDto.toUserEntity(existingInternalId: Long?): UserEntity {
        private fun UserBackupDto.toUserEntity(): UserEntity {
    // IMPORTANT: Use your actual UserEntity fields.
        // Assumes you have:
        // - internalId: Long? (PK)
        // - publicUserId: String
        // - nameNormalized: String
        return UserEntity(
//            internalId = existingInternalId,
            publicUserId = id,
            name = name,
            nameNormalized = name.trim().lowercase(),
            dateOfBirth = dateOfBirth,
            specialDate = specialDate,
            specialDateEpoch = null, // if you store this, parse here
            isFavorite = isFavorite,
            isPinned = isPinned,
            contactNumber = contactNumber,
            instagramId = instagramId,
            otherMedia = otherMedia,
            location = location,
            createdAtEpoch = createdAtEpoch,
            editedAtEpoch = editedAtEpoch
        )
    }

//    private fun SectionBackupDto.toSectionEntity(
//        publicUserId: String,
//        existingInternalId: Long?,
//        overrideSectionId: String? = null
//    ): MeasurementSectionEntity {
    private fun SectionBackupDto.toSectionEntity(
        publicUserId: String,
        overrideSectionId: String? = null
    ): MeasurementSectionEntity {
    val secId = overrideSectionId ?: id

        return MeasurementSectionEntity(
//            internalId = existingInternalId,
            publicUserId = publicUserId,
            sectionId = secId,
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

    private fun timestampForName(): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return fmt.format(Date())
    }

    private fun generate6DigitId(): String {
        return (100000 + Random.nextInt(900000)).toString()
    }
}
