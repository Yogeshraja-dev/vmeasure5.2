package com.vmeasure.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_sections",
    indices = [
        Index(value = ["publicUserId"]),
        Index(value = ["publicUserId", "type"]),
        Index(value = ["publicUserId", "sectionId"], unique = true),
        Index(value = ["createdAtEpoch"])
    ]
)
data class MeasurementSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val pk: Long = 0L,

    // FK by publicUserId (simplifies sync merge by your rule)
    val publicUserId: String,

    // 6-digit section ID (unique per user)
    val sectionId: String,

    // One of TagType.displayName values (e.g., "Blouse")
    val type: String,

    val createdAtEpoch: Long,
    val editedAtEpoch: Long?,

    // Shared notes (all types)
    val notes: String? = null,

    // ===== BLOUSE =====
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

    // ===== KURTI =====
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

    // ===== PANT =====
    val pant_waist: String? = null,
    val pant_hip: String? = null,
    val pant_length: String? = null,
    val pant_thighRound: String? = null,
    val pant_kneeRound: String? = null,
    val pant_bottom: String? = null,
    val pant_inseam: String? = null,

    // ===== FROCK =====
    val frock_waist: String? = null,
    val frock_frockLength: String? = null,
    val frock_yokeLength: String? = null,

    // ===== CROP BLOUSE & SKIRT =====
    val crop_blouseWaist: String? = null,
    val crop_blouseLength: String? = null,
    val crop_skirtLength: String? = null,
    val crop_waistLength: String? = null,

    // ===== KIDS BOY =====
    val kids_chest: String? = null,
    val kids_waist: String? = null,
    val kids_length: String? = null,
    val kids_shoulder: String? = null,
    val kids_sleeveLength: String? = null,
    val kids_pantLength: String? = null,
    val kids_pantWaist: String? = null
)
