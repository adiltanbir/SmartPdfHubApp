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

    val pdfList: StateFlow<List<PDFFile>> = combine(
        _searchQuery.debounce(300),
        _sortOption,
        _selectedFilter
    ) { query, sort, filter ->
        Triple(query, sort, filter)
    }.flatMapLatest { (query, sort, filter) ->
        _isLoading.value = true
        try {
            val flow = when {
                query.isNotBlank() -> repository.searchPDFs(query, sort)
                filter != null -> repository.filterBySource(filter)
                else -> repository.getAllPDFs(sort)
            }
            flow.onEach { _isLoading.value = false }
        } catch (e: Exception) {
            _error.value = e.message
            _isLoading.value = false
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favorites = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyOpened = repository.getRecentlyOpened()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOption(option: SortOption) { _sortOption.value = option }
    fun setFilter(sourceType: SourceType?) { _selectedFilter.value = sourceType }

    fun toggleFavorite(pdf: PDFFile) {
        viewModelScope.launch { repository.toggleFavorite(pdf) }
    }

    fun markAsOpened(pdf: PDFFile) {
        viewModelScope.launch { repository.markAsOpened(pdf) }
    }

    fun refresh() { _sortOption.value = _sortOption.value }
    fun clearError() { _error.value = null }
}
