package com.vikram.locationdots.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vikram.locationdots.domain.model.TimelineEvent
import com.vikram.locationdots.domain.timeline.TimelineRepository
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimelineUiState(
    val events: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val currentTime: Instant = Instant.now()
)

class TimelineViewModel(private val repository: TimelineRepository) : ViewModel() {
    companion object {
        const val PAGE_SIZE = 30
        private const val CLOCK_TICK_MILLIS = 60_000L
        private const val REFRESH_INDICATOR_MIN_MILLIS = 250L
    }

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private var recentEvents: List<TimelineEvent> = emptyList()
    private var olderEvents: List<TimelineEvent> = emptyList()
    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeRecent(PAGE_SIZE).collect { latest ->
                recentEvents = latest
                publishEvents(hasMore = latest.size == PAGE_SIZE)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(CLOCK_TICK_MILLIS)
                _uiState.value = _uiState.value.copy(currentTime = Instant.now())
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing || _uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)

        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            try {
                recentEvents = repository.getPage(PAGE_SIZE, 0)
                publishEvents(hasMore = recentEvents.size == PAGE_SIZE)
            } catch (error: Exception) {
                setError(error.message ?: "Unable to refresh timeline")
            } finally {
                val remaining = REFRESH_INDICATOR_MIN_MILLIS -
                    (System.currentTimeMillis() - startedAt)
                if (remaining > 0) delay(remaining)
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing || !_uiState.value.hasMore) return
        if (loadMoreJob?.isActive == true) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        loadMoreJob = viewModelScope.launch {
            try {
                val offset = recentEvents.size + olderEvents.size
                val page = repository.getPage(PAGE_SIZE, offset)
                val knownIds = (recentEvents + olderEvents).mapTo(HashSet()) { it.id }
                olderEvents = olderEvents + page.filterNot { it.id in knownIds }
                publishEvents(hasMore = page.size == PAGE_SIZE)
            } catch (error: Exception) {
                setError(error.message ?: "Unable to load more timeline")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun retry() {
        if (_uiState.value.events.isEmpty()) refresh() else loadMore()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun publishEvents(hasMore: Boolean = _uiState.value.hasMore) {
        val merged = (recentEvents + olderEvents)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
        _uiState.value = _uiState.value.copy(
            events = merged,
            hasMore = hasMore,
            errorMessage = null,
            currentTime = Instant.now()
        )
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
