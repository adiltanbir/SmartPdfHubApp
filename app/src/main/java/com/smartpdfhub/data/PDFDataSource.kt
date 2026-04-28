// app/src/main/java/com/smartpdfhub/data/PDFDataSource.kt
package com.smartpdfhub.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.smartpdfhub.data.model.PDFFile
import com.smartpdfhub.data.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDFDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    suspend fun scanAllPDFs(): List<PDFFile> = withContext(Dispatchers.IO) {
        val pdfs = mutableListOf<PDFFile>()
        
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT.toString())
        
        // Better PDF detection using MIME type
        val mimeSelection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
        val mimeArgs = arrayOf("application/pdf", "%pdf%")

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val cursor = contentResolver.query(
            uri,
            projection,
            mimeSelection,
            mimeArgs,
            "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val dateIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val dataIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val pathIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)

            while (c.moveToNext()) {
                val id = c.getLong(idIndex)
                val name = c.getString(nameIndex) ?: continue
                val size = c.getLong(sizeIndex)
                val date = c.getLong(dateIndex) * 1000 // Convert to milliseconds
                val path = c.getString(dataIndex) ?: ""
                val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getString(pathIndex) ?: ""
                } else ""

                val contentUri = Uri.withAppendedPath(
                    MediaStore.Files.getContentUri("external"),
                    id.toString()
                )

                val sourceType = detectSourceType(path, relativePath)

                pdfs.add(
                    PDFFile(
                        id = contentUri.toString(),
                        displayName = name.removeSuffix(".pdf"),
                        size = size,
                        lastModified = date,
                        path = path,
                        sourceType = sourceType
                    )
                )
            }
        }

        // Also scan specific directories for Android 10+ scoped storage compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pdfs.addAll(scanScopedStoragePDFs())
        }

        pdfs.distinctBy { it.id }
    }

    private fun detectSourceType(path: String, relativePath: String): SourceType {
        val fullPath = path.lowercase()
        val relPath = relativePath.lowercase()

        return when {
            fullPath.contains("whatsapp") || relPath.contains("whatsapp") -> SourceType.WHATSAPP
            fullPath.contains("telegram") || relPath.contains("telegram") -> SourceType.TELEGRAM
            fullPath.contains("download") || relPath.contains("download") -> SourceType.DOWNLOADS
            fullPath.contains("bluetooth") || relPath.contains("bluetooth") -> SourceType.BLUETOOTH
            fullPath.contains("documents") || relPath.contains("documents") -> SourceType.DOCUMENTS
            else -> SourceType.OTHER
        }
    }

    private fun scanScopedStoragePDFs(): List<PDFFile> {
        val pdfs = mutableListOf<PDFFile>()
        val directories = listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DOCUMENTS
        )

        directories.forEach { dirType ->
            val dir = Environment.getExternalStoragePublicDirectory(dirType)
            dir?.listFiles { file -> 
                file.isFile && file.extension.equals("pdf", ignoreCase = true) 
            }?.forEach { file ->
                val uri = Uri.fromFile(file)
                pdfs.add(
                    PDFFile(
                        id = uri.toString(),
                        displayName = file.nameWithoutExtension,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        path = file.absolutePath,
                        sourceType = detectSourceType(file.absolutePath, "")
                    )
                )
            }
        }

        // Scan app-specific directories
        val appDirs = listOf(
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/WhatsApp Documents"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents"),
            File(Environment.getExternalStorageDirectory(), "Telegram/Telegram Documents"),
            File(Environment.getExternalStorageDirectory(), "Download")
        )

        appDirs.forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown()
                    .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
                    .forEach { file ->
                        val uri = Uri.fromFile(file)
                        pdfs.add(
                            PDFFile(
                                id = uri.toString(),
                                displayName = file.nameWithoutExtension,
                                size = file.length(),
                                lastModified = file.lastModified(),
                                path = file.absolutePath,
                                sourceType = detectSourceType(file.absolutePath, "")
                            )
                        )
                    }
            }
        }

        return pdfs
    }
}
