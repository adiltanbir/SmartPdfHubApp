package com.smartpdfhub.data

import com.smartpdfhub.data.local.PDFDao
import com.smartpdfhub.data.model.PDFFile
import com.smartpdfhub.data.model.SourceType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDFRepository @Inject constructor(
    private val pdfDao: PDFDao,
    private val dataSource: PDFDataSource
) {
    // BUG FIX #5: Expose raw flow from DB — ViewModel applies search/sort/filter on top of this
    val allPDFs: Flow<List<PDFFile>> = pdfDao.getAllPDFs()

    fun getFavorites(): Flow<List<PDFFile>> = pdfDao.getFavorites()

    fun getRecentlyOpened(): Flow<List<PDFFile>> = pdfDao.getRecentlyOpened()

    // BUG FIX #6: Added refresh() that actually calls the DataSource scan and inserts into DB
    // Previously dataSource.scanAllPDFs() was NEVER called — DB was always empty
    suspend fun refresh() {
        val pdfs = dataSource.scanAllPDFs()
        pdfDao.insertAll(pdfs)
    }

    suspend fun toggleFavorite(pdf: PDFFile) {
        pdfDao.setFavorite(pdf.id, !pdf.isFavorite)
    }

    suspend fun markAsOpened(pdf: PDFFile) {
        pdfDao.setLastOpened(pdf.id, System.currentTimeMillis())
    }
}
