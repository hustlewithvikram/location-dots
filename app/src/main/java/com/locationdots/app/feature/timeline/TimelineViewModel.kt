package com.locationdots.app.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.model.TimelineEvent
import com.locationdots.app.domain.timeline.TimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TimelineViewModel(private val repository: TimelineRepository) : ViewModel() {
    companion object {
        const val PAGE_SIZE = 30
    }

    private val _timeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val timeline: StateFlow<List<TimelineEvent>> = _timeline.asStateFlow()

    private var offset = 0
    private var loading = false
    private var hasMore = true

    init {
        loadMore()
    }

    fun loadMore() {
        if (loading || !hasMore) return

        loading = true
        viewModelScope.launch {
            try {
                val page = repository.getPage(PAGE_SIZE, offset)
                _timeline.value = _timeline.value + page
                offset += page.size
                hasMore = page.size == PAGE_SIZE
            } finally {
                loading = false
            }
        }
    }
}
