package com.awakenedredstone.multiversion.meta

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Meta(val properties: Map<String, String>?, val versions: Map<String, Version>?)
