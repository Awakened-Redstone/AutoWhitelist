package com.awakenedredstone.multiversion.game

import kotlinx.serialization.Serializable

@Serializable
data class GameVersion(val version: String, val stable: Boolean)