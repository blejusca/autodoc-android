package com.autodoc.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autodoc.data.entity.CarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {

    @Insert
    suspend fun insert(car: CarEntity): Long

    @Query("SELECT * FROM cars ORDER BY brand ASC, model ASC")
    fun observeCars(): Flow<List<CarEntity>>

    @Query("SELECT * FROM cars")
    suspend fun getAllCarsSync(): List<CarEntity>

    @Query("""
        UPDATE cars 
        SET brand = :brand,
            model = :model,
            plate = :plate,
            year = :year,
            engine = :engine,
            ownerName = :ownerName,
            ownerPhone = :ownerPhone,
            ownerEmail = :ownerEmail,
            ownerNotes = :ownerNotes
        WHERE id = :carId
    """)
    suspend fun updateCar(
        carId: Int,
        brand: String,
        model: String,
        plate: String,
        year: Int,
        engine: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        ownerNotes: String
    )

    @Query("DELETE FROM cars WHERE id = :carId")
    suspend fun deleteCar(carId: Int)

    @Query("SELECT * FROM cars WHERE id = :carId LIMIT 1")
    suspend fun getCarById(carId: Int): CarEntity?

    @Query("DELETE FROM cars")
    suspend fun deleteAll()
}