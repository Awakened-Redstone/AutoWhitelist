package com.awakenedredstone.commons

inline fun <reified T> setOf(extend: Set<T>, vararg args: T): Set<T> {
    return setOf(*extend.toTypedArray(), *args)
}
