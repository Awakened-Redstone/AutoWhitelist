package com.awakenedredstone.multiversion.serialization.fetch

import kotlinx.serialization.Serializable

@Serializable
data class MavenMetadata(val groupId: String, val artifactId: String, val versioning: Versioning)

@Serializable
data class Versioning(val latest: String, val release: String, val versions: List<String>, val lastUpdated: String)
