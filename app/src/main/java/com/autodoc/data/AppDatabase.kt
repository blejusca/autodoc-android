package com.autodoc.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.autodoc.data.dao.CarDao
import com.autodoc.data.dao.DocumentDao
import com.autodoc.data.entity.CarEntity
import com.autodoc.data.entity.DocumentEntity

@Database(
    entities = [CarEntity::class, DocumentEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun carDao(): CarDao
    abstract fun documentDao(): DocumentDao
}