package com.smartpdfhub.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartpdfhub.data.PDFRepository
import com.smartpdfhub.data.model.PDFFile
import com.smartpdfhub.data.model.SortOption
import com.smartpdfhub.data.model.SourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class PDFViewModel @Inject constructor(
    private val repository: PDFRepository
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.RECENT)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _selectedFilter = MutableStateFlow<SourceType?>(null)

    // BUG FIX #7 & #8: pdfList now correctly combines the DB flow with search/sort/filter
    // Previously: repository methods were never connected to dataSource, and filter/sort were no-ops
    val pdfList: StateFlow<List<PDFFile>> = combine(
        repository.allPDFs,
        _searchQuery.debounce(300),
        _sortOption,
        _selectedFilter
    ) { pdfs, query, sort, filter ->
        pdfs
            .filter { pdf ->
                (query.isBlank() || pdf.displayName.contains(query, ignoreCase = true)) &&
                (filter == null || pdf.sourceType == filter)
            }
            .sortedWith(getSortComparator(sort))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyOpened = repository.getRecentlyOpened()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    fun setFilter(sourceType: SourceType?) { _selectedFilter.value = sourceType }

    fun toggleFavorite(pdf: PDFFile) {
        viewModelScope.launch { repository.toggleFavorite(pdf) }
    }

    fun markAsOpened(pdf: PDFFile) {
        viewModelScope.launch { repository.markAsOpened(pdf) }
    }

    // BUG FIX #9: refresh() now actually calls repository.refresh() which scans device for PDFs
    // Previously it just set the same StateFlow value — a complete no-op
    fun refresh() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                repository.refresh()
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to load PDFs")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() { _error.postValue(null) }

    private fun getSortComparator(sort: SortOption): Comparator<PDFFile> = when (sort) {
        SortOption.NAME_ASC, SortOption.NAME -> compareBy { it.displayName.lowercase() }
        SortOption.NAME_DESC -> compareByDescending { it.displayName.lowercase() }
        SortOption.SIZE_ASC, SortOption.SIZE -> compareBy { it.size }
        SortOption.SIZE_DESC -> compareByDescending { it.size }
        SortOption.DATE_ASC -> compareBy { it.lastModified }
        SortOption.DATE_DESC, SortOption.DATE, SortOption.RECENT -> compareByDescending { it.lastModified }
    }
}
