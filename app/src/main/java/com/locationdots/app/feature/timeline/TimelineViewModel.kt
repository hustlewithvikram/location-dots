package com.locationdots.app.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.timeline.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineUiState(
    val events: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null
)

class TimelineViewModel(private val repository: TimelineRepository) : ViewModel() {
    companion object {
        const val PAGE_SIZE = 30
    }

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadMore()
    }

    fun refresh() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing) return

        _uiState.value = _uiState.value.copy(
            isRefreshing = true,
            hasMore = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val page = repository.getPage(PAGE_SIZE, 0)
                _uiState.value = TimelineUiState(
                    events = page.distinctBy { it.id },
                    isRefreshing = false,
                    hasMore = page.size == PAGE_SIZE
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    errorMessage = error.message ?: "Unable to refresh timeline"
                )
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || !state.hasMore) return

        _uiState.value = state.copy(
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val current = _uiState.value.events
                val page = repository.getPage(PAGE_SIZE, current.size)
                val merged = (current + page).distinctBy { it.id }

                _uiState.value = _uiState.value.copy(
                    events = merged,
                    isLoading = false,
                    hasMore = page.size == PAGE_SIZE
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load timeline"
                )
            }
        }
    }

    fun retry() {
        if (_uiState.value.events.isEmpty()) refresh() else loadMore()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
