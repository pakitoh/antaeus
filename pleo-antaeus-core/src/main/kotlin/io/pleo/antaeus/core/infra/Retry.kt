package io.pleo.antaeus.core.infra

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

const val MAX_ATTEMPTS = 4

class Retry(private val maxAttempts: Int = MAX_ATTEMPTS) {

    fun <T, R> withRetry(
        f: (t: T) -> R,
        t: T,
        checkResult: (R) -> Boolean
    ): R {
        var attempt = 0
        var resul = f(t)
        while (attempt < maxAttempts && checkResult(resul)) {
            Thread.sleep(calculateExponentialBackoff(attempt))
            logger.debug { "Retrying ${t} for ${attempt} time" }
            resul = f(t)
            attempt++
        }
        return resul
    }

    private fun calculateExponentialBackoff(attempt: Int) =
        Math.pow(2.0, attempt.toDouble()).toLong() * 1000
}