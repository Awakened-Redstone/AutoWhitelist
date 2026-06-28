package com.awakenedredstone.multiversion.util

import kotlin.time.Duration
import kotlin.time.DurationUnit

fun Duration.formattedString(): String {
    if (isInfinite()) return (if (isPositive()) "" else "-") + "Infinite"

    return when (this.toLong(DurationUnit.NANOSECONDS)) {
        0L -> "0s"
        else -> {
            val isNegative = isNegative()
            buildString {
                if (isNegative) append('-')
                absoluteValue.toComponents { days, hours, minutes, seconds, nanoseconds ->
                    val hasDays = days != 0L
                    val hasHours = hours != 0
                    val hasMinutes = minutes != 0
                    val hasSeconds = seconds != 0 || nanoseconds != 0
                    var components = 0
                    if (hasDays) {
                        append(days).append('d')
                        components++
                    }
                    if (hasHours || (hasDays && (hasMinutes || hasSeconds))) {
                        if (components++ > 0) append(' ')
                        append(hours).append('h')
                    }
                    if (hasMinutes || (hasSeconds && (hasHours || hasDays))) {
                        if (components++ > 0) append(' ')
                        append(minutes).append('m')
                    }
                    if (hasSeconds) {
                        if (components++ > 0) append(' ')
                        when {
                            seconds != 0 || hasDays || hasHours || hasMinutes ->
                                appendFractional(seconds, nanoseconds, 2, "s")
                            nanoseconds >= 1_000_000 ->
                                appendFractional(nanoseconds / 1_000_000, nanoseconds % 1_000_000, 2, "ms")
                            nanoseconds >= 1_000 ->
                                appendFractional(nanoseconds / 1_000, nanoseconds % 1_000, 2, "us")
                            else ->
                                append(nanoseconds).append("ns")
                        }
                    }
                    if (isNegative && components > 1) insert(1, '(').append(')')
                }
            }
        }
    }
}

private fun StringBuilder.appendFractional(whole: Int, fractional: Int, fractionalSize: Int, unit: String) {
    append(whole)
    if (fractional != 0) {
        append('.')
        val fracString = fractional.toString().padStart(fractionalSize, '0')
        val nonZeroDigits = fracString.indexOfLast { it != '0' } + 1
        when {
            nonZeroDigits < fractionalSize -> appendRange(fracString, 0, nonZeroDigits)
            else -> appendRange(fracString, 0, fractionalSize)
        }
    }
    append(unit)
}
