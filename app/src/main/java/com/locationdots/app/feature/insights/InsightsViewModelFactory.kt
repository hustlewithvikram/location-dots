package com.locationdots.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locationdots.app.domain.insights.InsightsRepository

class InsightsViewModelFactory(
    private val repository: InsightsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        InsightsViewModel(repository) as T
}
