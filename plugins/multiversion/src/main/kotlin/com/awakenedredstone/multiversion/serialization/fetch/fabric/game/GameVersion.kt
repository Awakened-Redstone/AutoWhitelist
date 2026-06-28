package com.awakenedredstone.multiversion.serialization.fetch.fabric.game

import kotlinx.serialization.Serializable

@Serializable
data class GameVersion(val version: String, val stable: Boolean)
