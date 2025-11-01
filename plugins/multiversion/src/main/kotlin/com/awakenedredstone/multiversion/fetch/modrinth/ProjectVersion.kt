package com.awakenedredstone.multiversion.fetch.modrinth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import kotlinx.serialization.Serializable

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ProjectVersion(
    val versionNumber: String,
    val gameVersions: List<String>,
    val versionType: VersionType,
) {
    @Suppress("unused")
    enum class VersionType {
        @JsonProperty("release") Release,
        @JsonProperty("beta") Beta,
        @JsonProperty("alpha") Alpha
    }
}
