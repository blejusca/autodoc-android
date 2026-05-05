package com.autodoc.domain.mapper

import com.autodoc.data.entity.CarEntity
import com.autodoc.data.entity.DocumentEntity
import com.autodoc.ui.CarUi
import com.autodoc.ui.DocumentUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun DocumentEntity.toUi(): DocumentUi {
    val expiryLocalDate = Instant.ofEpochMilli(expiryDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

    val today = LocalDate.now()

    val daysLeft = ChronoUnit.DAYS.between(today, expiryLocalDate).toInt()

    return DocumentUi(
        id = id,
        carId = carId,
        type = type,
        expiryDateMillis = expiryLocalDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
        daysLeft = daysLeft,
        reminderDaysBefore = reminderDaysBefore,
        notifiedExpired = notifiedExpired,
        notifiedToday = notifiedToday,
        notifiedTomorrow = notifiedTomorrow,
        notifiedReminder = notifiedReminder,
        manuallyNotified = manuallyNotified
    )
}

fun CarEntity.toUi(documents: List<DocumentEntity>): CarUi {
    return CarUi(
        id = id,
        brand = brand,
        model = model,
        plate = plate,
        year = year,
        engine = engine,
        ownerName = ownerName,
        ownerPhone = ownerPhone,
        ownerEmail = ownerEmail,
        ownerNotes = ownerNotes,
        documents = documents
            .filter { it.carId == id }
            .map { it.toUi() }
            .sortedBy { it.daysLeft }
    )
}