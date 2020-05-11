package net.pechorina.kairos.core.utils

import kotlinx.coroutines.delay
import java.io.IOException

object RetryUtils {
    suspend fun <T> retryIO(
            times: Int = Int.MAX_VALUE,
            initialDelay: Long = 200, // 0.2 second
            maxDelay: Long = 5000,    // 5 second
            factor: Double = 2.0,
            block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: IOException) {
                // you can log an error here and/or make a more finer-grained
                // analysis of the cause to see if retry is needed
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }

    suspend fun <T> retryWithPredicate(
            times: Int = Int.MAX_VALUE,
            initialDelay: Long = 200, // 0.2 second
            maxDelay: Long = 5000,    // 5 second
            factor: Double = 2.0,
            predicate: (T?) -> Boolean,
            block: suspend () -> T): T {
        var currentDelay = initialDelay
        repeat(times - 1) {

            val value: T = block()

            if (predicate(value)) {
                return value
            }

            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block() // last attempt
    }
}
