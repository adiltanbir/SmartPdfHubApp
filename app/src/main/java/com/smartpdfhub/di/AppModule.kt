package com.smartpdfhub.di

import android.content.Context
import androidx.room.Room
import com.smartpdfhub.data.local.AppDatabase
import com.smartpdfhub.data.local.PDFDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_pdf_hub.db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePDFDao(database: AppDatabase): PDFDao = database.pdfDao()
}
