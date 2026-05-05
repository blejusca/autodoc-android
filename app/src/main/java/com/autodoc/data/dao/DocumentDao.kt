package com.autodoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autodoc.data.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Query("SELECT * FROM documents ORDER BY expiryDate ASC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE carId = :carId ORDER BY expiryDate ASC")
    fun observeDocumentsForCar(carId: Int): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY expiryDate ASC")
    suspend fun getAllDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents")
    suspend fun getAllDocumentsSync(): List<DocumentEntity>

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun deleteById(documentId: Int)

    @Query("SELECT * FROM documents WHERE carId = :carId AND type = :type LIMIT 1")
    suspend fun getDocumentByCarIdAndType(
        carId: Int,
        type: String
    ): DocumentEntity?

    @Query(
        """
        UPDATE documents 
        SET 
            expiryDate = :expiryDateMillis,
            notifiedExpired = 0,
            notifiedToday = 0,
            notifiedTomorrow = 0,
            notifiedReminder = 0,
            manuallyNotified = 0
        WHERE id = :documentId
        """
    )
    suspend fun updateExpiryDate(
        documentId: Int,
        expiryDateMillis: Long
    )

    @Query("UPDATE documents SET notifiedExpired = 1 WHERE id = :id")
    suspend fun markExpiredNotified(id: Int)

    @Query("UPDATE documents SET notifiedToday = 1 WHERE id = :id")
    suspend fun markTodayNotified(id: Int)

    @Query("UPDATE documents SET notifiedTomorrow = 1 WHERE id = :id")
    suspend fun markTomorrowNotified(id: Int)

    @Query("UPDATE documents SET notifiedReminder = 1 WHERE id = :id")
    suspend fun markReminderNotified(id: Int)

    // 🔥 NOU — notificare manuala client (WhatsApp / email)
    @Query("UPDATE documents SET manuallyNotified = 1 WHERE id = :id")
    suspend fun markManuallyNotified(id: Int)

    @Query("DELETE FROM documents")
    suspend fun deleteAll()
}