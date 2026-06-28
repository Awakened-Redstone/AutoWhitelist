package com.awakenedredstone.multiversion.serialization.fetch.fabric.yarn

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.serialization.Serializable

@Serializable
@JsonIgnoreProperties(ignoreUnknown = true)
data class YarnVersion(
    val gameVersion: String,
    val separator: String,
    val build: Int,
    val maven: String,
    val version: String,
    val stable: Boolean,
)
