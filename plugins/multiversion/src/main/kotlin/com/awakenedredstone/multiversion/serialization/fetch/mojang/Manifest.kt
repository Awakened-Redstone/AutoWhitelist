@file:OptIn(ExperimentalSerializationApi::class)

package com.awakenedredstone.multiversion.serialization.fetch.mojang

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
@JsonIgnoreUnknownKeys
data class Manifest(val versions: List<Version>) {
    @Serializable
    @JsonIgnoreUnknownKeys
    data class Version(val id: String, val type: VersionType, val url: String)

    @Suppress("unused")
    enum class VersionType(val stable: Boolean) {
        @SerialName("release") Release(true),
        @SerialName("snapshot") Snapshot(false),
        @SerialName("old_beta") OldBeta(false),
        @SerialName("old_alpha") OldAlpha(false),
    }
}
