package com.locationdots.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.search.SearchRepository
import com.locationdots.app.domain.search.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        viewModelScope.launch {
            if (query.trim().length < 2) {
                _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = repository.search(query)
            _uiState.value = _uiState.value.copy(results = results, isSearching = false)
        }
    }

    fun clear() {
        _uiState.value = SearchUiState()
    }
}
