package com.vmeasure.app.sync.drive

import kotlinx.serialization.Serializable

@Serializable
data class BackupFileDto(
    val exportedAtEpoch: Long,
    val users: List<UserDto>,
    val deletedUserIds: List<String>
)

@Serializable
data class UserDto(
    val id: String, // publicUserId
    val name: String,
    val dateOfBirth: String = "",
    val specialDate: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val contactNumber: String = "",
    val instagramId: String = "",
    val otherMedia: String = "",
    val location: String = "",
    val createdAtEpoch: Long,
    val editedAtEpoch: Long? = null,
    val isDeleted : Boolean = false,
    val deletedAtEpoch : Long? = null,
    val measurementSections: List<SectionDto> = emptyList()
)

@Serializable
data class SectionDto(
    val id: String,      // sectionId
    val type: String,    // "Kurti" etc
    val createdAtEpoch: Long,
    val editedAtEpoch: Long? = null,

    val notes: String? = null,

    // blouse
    val blouse_uBust: String? = null,
    val blouse_bust: String? = null,
    val blouse_waist: String? = null,
    val blouse_hip: String? = null,
    val blouse_armhole: String? = null,
    val blouse_shoulder: String? = null,
    val blouse_length: String? = null,
    val blouse_fNeck: String? = null,
    val blouse_bNeck: String? = null,
    val blouse_sleeveLength: String? = null,
    val blouse_sleeveRound: String? = null,

    // kurti
    val kurti_blouseCut: String? = null,
    val kurti_uBust: String? = null,
    val kurti_bust: String? = null,
    val kurti_waist: String? = null,
    val kurti_armhole: String? = null,
    val kurti_shoulder: String? = null,
    val kurti_blouse: String? = null,
    val kurti_fNeck: String? = null,
    val kurti_bNeck: String? = null,
    val kurti_sleeveLength: String? = null,
    val kurti_sleeveRound: String? = null,

    // pant
    val pant_waist: String? = null,
    val pant_hip: String? = null,
    val pant_length: String? = null,
    val pant_thighRound: String? = null,
    val pant_kneeRound: String? = null,
    val pant_bottom: String? = null,
    val pant_inseam: String? = null,

    // frock
    val frock_waist: String? = null,
    val frock_frockLength: String? = null,
    val frock_yokeLength: String? = null,

    // crop
    val crop_blouseWaist: String? = null,
    val crop_blouseLength: String? = null,
    val crop_skirtLength: String? = null,
    val crop_waistLength: String? = null,

    // kids
    val kids_chest: String? = null,
    val kids_waist: String? = null,
    val kids_length: String? = null,
    val kids_shoulder: String? = null,
    val kids_sleeveLength: String? = null,
    val kids_pantLength: String? = null,
    val kids_pantWaist: String? = null
)
