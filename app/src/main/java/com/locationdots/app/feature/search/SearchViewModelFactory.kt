package com.vikram.locationdots.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vikram.locationdots.domain.search.SearchRepository

class SearchViewModelFactory(
    private val repository: SearchRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SearchViewModel(repository) as T
}
