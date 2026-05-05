package com.autodoc.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

fun formatDate(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

fun documentStatusText(daysLeft: Int): String {
    return when {
        daysLeft < 0 -> {
            val days = abs(daysLeft)
            if (days == 1) {
                localizedText("expirat de 1 zi", "expired 1 day ago")
            } else {
                localizedText("expirat de $days zile", "expired $days days ago")
            }
        }

        daysLeft == 0 -> localizedText("expiră azi", "expires today")
        daysLeft == 1 -> localizedText("expiră mâine", "expires tomorrow")
        else -> localizedText("expiră în $daysLeft zile", "expires in $daysLeft days")
    }
}

fun normalizeDocumentType(type: String): String {
    return when (type.trim().lowercase()) {
        "itp" -> "ITP"
        "rca" -> "RCA"
        "casco" -> "CASCO"
        "rovinieta" -> "Rovinieta"
        "revizie" -> "Revizie"
        else -> type.trim()
    }
}

fun parseDateToMillis(value: String): Long? {
    return parseDate(value)
        ?.atStartOfDay(ZoneId.systemDefault())
        ?.toInstant()
        ?.toEpochMilli()
}

fun parseDate(value: String): LocalDate? {
    val cleanValue = value.trim().replace("/", "-")

    if (cleanValue.isBlank()) {
        return null
    }

    val supportedFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy")
    )

    for (formatter in supportedFormats) {
        try {
            return LocalDate.parse(cleanValue, formatter)
        } catch (_: Exception) {
            // Try next format.
        }
    }

    return null
}