package com.vmeasure.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtil {
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)
    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)

    fun nowEpochMillis(): Long = System.currentTimeMillis()

    fun formatDate(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val ld = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
        return ld.format(dateFmt)
    }

    fun formatDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val ldt = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        // "a" gives AM/PM uppercase in EN locale
        return ldt.format(dateTimeFmt)
    }

    // Parses dd/MM/yyyy; returns null if invalid
    fun parseDateToEpochDayStart(dateText: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        return try {
            val ld = LocalDate.parse(dateText.trim(), dateFmt)
            ld.atStartOfDay(zoneId).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
