package com.awakenedredstone.multiversion.values

import kotlinx.serialization.Serializable

@Serializable
data class Repo(val repo: String, val version: String)
