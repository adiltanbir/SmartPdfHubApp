package com.smartpdfhub.data

import com.smartpdfhub.data.local.PDFDao
import com.smartpdfhub.data.model.PDFFile
import com.smartpdfhub.data.model.SortOption
import com.smartpdfhub.data.model.SourceType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PDFRepository @Inject constructor(
    private val pdfDao: PDFDao,
    private val dataSource: PDFDataSource
) {
    fun getAllPDFs(sort: SortOption): Flow<List<PDFFile>> = pdfDao.getAllPDFs()

    fun getFavorites(): Flow<List<PDFFile>> = pdfDao.getFavorites()

    fun getRecentlyOpened(): Flow<List<PDFFile>> = pdfDao.getRecentlyOpened()

    suspend fun toggleFavorite(pdf: PDFFile) {
        pdfDao.setFavorite(pdf.id, !pdf.isFavorite)
    }

    suspend fun markAsOpened(pdf: PDFFile) {
        pdfDao.setLastOpened(pdf.id, System.currentTimeMillis())
    }

    fun searchPDFs(query: String, sort: SortOption): Flow<List<PDFFile>> {
        // Implementation for searching
        return pdfDao.getAllPDFs() 
    }

    fun filterBySource(source: SourceType): Flow<List<PDFFile>> {
        // Implementation for filtering
        return pdfDao.getAllPDFs()
    }
}
