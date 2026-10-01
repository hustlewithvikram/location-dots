package com.locationdots.app.core.common

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val cause: Throwable) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}
