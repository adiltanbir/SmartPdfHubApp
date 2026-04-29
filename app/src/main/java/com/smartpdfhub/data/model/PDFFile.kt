package com.smartpdfhub.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PDFFile(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val sourceType: SourceType,
    val isFavorite: Boolean = false,
    val lastOpened: Long? = null
)

enum class SourceType {
    WHATSAPP, DOWNLOADS, TELEGRAM, OTHER
}
