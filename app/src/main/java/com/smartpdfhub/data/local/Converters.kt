package com.smartpdfhub.data.local

import androidx.room.TypeConverter
import com.smartpdfhub.data.model.SourceType

class Converters {
    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)
}
