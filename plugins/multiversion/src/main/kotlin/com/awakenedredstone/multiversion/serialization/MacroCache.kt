package com.awakenedredstone.multiversion.serialization

import dev.kikugie.stonecutter.Identifier

typealias MacroCache = MutableMap<Identifier, Any>

inline fun <reified T : Any> MacroCache.getOrAdd(key: String, noinline mappingFunction: (Identifier) -> T): T {
    return computeIfAbsent(key, mappingFunction) as T
}

inline fun <reified T : Any> MacroCache.get(key: String): T {
    return get(key) as T
}
