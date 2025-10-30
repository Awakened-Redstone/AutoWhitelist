package com.awakenedredstone.multiversion.fetch.fabricmeta.game

import kotlinx.serialization.Serializable

@Serializable
data class GameVersion(val version: String, val stable: Boolean)