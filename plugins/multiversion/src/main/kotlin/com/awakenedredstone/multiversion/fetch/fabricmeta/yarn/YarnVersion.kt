package com.awakenedredstone.multiversion.fetch.fabricmeta.yarn

import kotlinx.serialization.Serializable

@Serializable
data class YarnVersion(
    val gameVersion: String,
    val separator: String,
    val build: Int,
    val maven: String,
    val version: String,
    val stable: Boolean,
)
