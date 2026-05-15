package com.skillexchange.core

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}

fun Throwable.readableMessage(): String = localizedMessage ?: "Something went wrong. Please try again."
