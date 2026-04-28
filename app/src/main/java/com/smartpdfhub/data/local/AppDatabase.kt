package com.smartpdfhub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartpdfhub.data.model.PDFFile

@Database(entities = [PDFFile::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfDao(): PDFDao
}    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE pdf_files SET lastOpened = :timestamp WHERE id = :id")
    suspend fun setLastOpened(id: String, timestamp: Long)

    @Query("SELECT isFavorite FROM pdf_files WHERE id = :id")
    suspend fun isFavorite(id: String): Boolean?
}

// app/src/main/java/com/smartpdfhub/data/local/Converters.kt
package com.smartpdfhub.data.local

import androidx.room.TypeConverter
import com.smartpdfhub.data.model.SourceType

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
}
