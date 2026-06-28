package com.awakenedredstone.multiversion.util

fun String.surround(prefix: Char, suffix: Char = prefix): String {
    return "${if (startsWith(prefix)) "" else prefix}$this${if (endsWith(suffix)) "" else suffix}"
}

fun String.surround(prefix: CharSequence, suffix: CharSequence = prefix): String {
    return "${if (startsWith(prefix)) "" else prefix}$this${if (endsWith(suffix)) "" else suffix}"
}

fun String.prefixed(prefix: CharSequence): String {
    return "${if (startsWith(prefix)) "" else prefix}$this"
}

fun String.suffixed(suffix: CharSequence): String {
    return "${if (endsWith(suffix)) "" else suffix}$this"
}
