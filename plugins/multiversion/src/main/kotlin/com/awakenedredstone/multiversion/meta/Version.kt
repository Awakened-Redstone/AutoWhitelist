package com.awakenedredstone.multiversion.meta

import kotlinx.serialization.Serializable

@Serializable
data class Version(val predicate: String, val properties: Map<String, String>?)
