package com.locationdots.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationdots.app.domain.insights.InsightsRepository
import com.locationdots.app.domain.insights.InsightsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InsightsViewModel(
    private val repository: InsightsRepository
) : ViewModel() {
    private val _snapshot = MutableStateFlow(InsightsSnapshot())
    val snapshot: StateFlow<InsightsSnapshot> = _snapshot.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching { repository.getSnapshot() }
                .onSuccess { _snapshot.value = it }
                .onFailure { _error.value = it.message ?: "Unable to load insights" }
            _isLoading.value = false
        }
    }
}
