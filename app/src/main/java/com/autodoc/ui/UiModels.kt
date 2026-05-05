package com.autodoc.ui

data class CarUi(
    val id: Int,
    val brand: String,
    val model: String,
    val plate: String,
    val year: Int,
    val engine: String,

    val ownerName: String,
    val ownerPhone: String,
    val ownerEmail: String,
    val ownerNotes: String,

    val documents: List<DocumentUi> = emptyList()
)

data class DocumentUi(
    val id: Int,
    val carId: Int,
    val type: String,
    val expiryDateMillis: Long,
    val daysLeft: Int,
    val reminderDaysBefore: Int,

    val notifiedExpired: Boolean = false,
    val notifiedToday: Boolean = false,
    val notifiedTomorrow: Boolean = false,
    val notifiedReminder: Boolean = false,

    val manuallyNotified: Boolean = false
)

enum class DocumentSeverity {
    EXPIRED,
    CRITICAL,
    SOON,
    OK
}

fun DocumentUi.severity(): DocumentSeverity {
    return when {
        daysLeft < 0 -> DocumentSeverity.EXPIRED
        daysLeft <= 7 -> DocumentSeverity.CRITICAL
        daysLeft <= 30 -> DocumentSeverity.SOON
        else -> DocumentSeverity.OK
    }
}

fun DocumentUi.isManuallyNotified(): Boolean {
    return manuallyNotified
}

fun DocumentUi.shouldNotifyClient(): Boolean {
    return daysLeft < 0 || daysLeft <= 7
}