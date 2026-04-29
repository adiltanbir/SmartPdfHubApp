package com.smartpdfhub.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "pdf_files")
data class PDFFile(
    @PrimaryKey val id: String,
    val displayName: String,   // BUG FIX #1: was 'name' — DataSource used named param 'displayName' which didn't exist
    val path: String,
    val size: Long,
    val lastModified: Long,
    val sourceType: SourceType,
    val isFavorite: Boolean = false,
    val lastOpened: Long? = null
) {
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

// BUG FIX #2: Added missing BLUETOOTH and DOCUMENTS values used by PDFDataSource
enum class SourceType(val displayName: String) {
    WHATSAPP("WhatsApp"),
    DOWNLOADS("Downloads"),
    TELEGRAM("Telegram"),
    DOCUMENTS("Documents"),
    BLUETOOTH("Bluetooth"),
    OTHER("Other")
}
