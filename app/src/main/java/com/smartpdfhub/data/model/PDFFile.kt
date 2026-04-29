package com.smartpdfhub.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

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
) {
    // These "getters" fix the errors in your PDFAdapter
    val displayName: String get() = name
    
    val formattedSize: String get() {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        return if (mb > 1) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
    }

    val formattedDate: String get() {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(lastModified))
    }
}

enum class SourceType {
    WHATSAPP, DOWNLOADS, TELEGRAM, OTHER
}
