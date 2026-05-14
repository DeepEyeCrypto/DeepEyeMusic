package com.deepeye.musicpro.core.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.deepeye.musicpro.core.result.Result

/**
 * Kotlin extension functions used across the project.
 * Pure Kotlin — zero Android dependencies.
 */

/**
 * Wraps each emission of a Flow in a [Result.Success], catching errors as [Result.Failure].
 */
fun <T> Flow<T>.asResult(): Flow<Result<T, Throwable>> =
    this.map<T, Result<T, Throwable>> { Result.Success(it) }
        .catch { emit(Result.Failure(it)) }

/**
 * Returns the string if it is not null or blank, otherwise returns the fallback.
 */
fun String?.orDefault(fallback: String = "Unknown"): String =
    if (this.isNullOrBlank()) fallback else this

/**
 * Clamps a value between a minimum and maximum.
 */
fun Float.clamp(min: Float, max: Float): Float = coerceIn(min, max)

/**
 * Clamps an Int between a minimum and maximum.
 */
fun Int.clamp(min: Int, max: Int): Int = coerceIn(min, max)
