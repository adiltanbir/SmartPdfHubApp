package com.smartpdfhub.data.model

// BUG FIX #3: Added NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC, DATE_ASC, DATE_DESC
// which are referenced in MainActivity and PDFViewModel but were missing from the enum
enum class SortOption {
    RECENT,
    NAME,
    NAME_ASC,
    NAME_DESC,
    DATE,
    DATE_ASC,
    DATE_DESC,
    SIZE,
    SIZE_ASC,
    SIZE_DESC
}
