package com.autodoc.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = CarEntity::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val carId: Int,
    val type: String,
    val expiryDate: Long,
    val reminderDaysBefore: Int = 7,

    // tracking notificari automate
    val notifiedExpired: Boolean = false,
    val notifiedToday: Boolean = false,
    val notifiedTomorrow: Boolean = false,
    val notifiedReminder: Boolean = false,

    // tracking notificare manuala client
    val manuallyNotified: Boolean = false
)