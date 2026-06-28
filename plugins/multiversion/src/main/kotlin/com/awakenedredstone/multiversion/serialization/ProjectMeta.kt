@file:OptIn(ExperimentalSerializationApi::class)

package com.awakenedredstone.multiversion.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class ProjectMeta(val properties: Map<String, String>? = null, val versions: LinkedHashMap<String, Version>? = null) {
    @Serializable
    data class Version(val enabled: Boolean = true, val predicate: String, val properties: VersionProperties?)
}

typealias VersionProperties = Map<String, String>
