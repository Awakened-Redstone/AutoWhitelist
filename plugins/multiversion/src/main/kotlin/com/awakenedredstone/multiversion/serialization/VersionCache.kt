package com.awakenedredstone.multiversion.serialization

import java.time.Instant


data class VersionCache(var expiration: Instant = Instant.ofEpochMilli(0), val versions: LinkedHashMap<String, Version> = LinkedHashMap()) {
    data class Version(val java: String)
}
