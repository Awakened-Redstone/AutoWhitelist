package com.awakenedredstone.multiversion.fetch.modrinth

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
@OptIn(ExperimentalSerializationApi::class)
data class ProjectVersion(
    @SerialName("version_number")
    val versionNumber: String,
    @SerialName("game_versions")
    val gameVersions: String,
    @SerialName("version_type")
    val versionType: VersionType,
) {
    enum class VersionType {
        @SerialName("release") Release,
        @SerialName("beta") Beta,
        @SerialName("alpha") Alpha
    }
}
