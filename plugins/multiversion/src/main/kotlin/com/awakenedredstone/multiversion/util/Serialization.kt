@file:OptIn(ExperimentalSerializationApi::class)

package com.awakenedredstone.multiversion.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import tools.jackson.databind.EnumNamingStrategies
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.javaprop.JavaPropsMapper
import tools.jackson.dataformat.toml.TomlMapper
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

private val kotlinModule = KotlinModule.Builder()
    .enable(KotlinFeature.StrictNullChecks)
    .build()

internal lateinit var metaMapper: ObjectMapper

internal val jsonMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule)
    .enumNamingStrategy(EnumNamingStrategies.SnakeCaseStrategy.INSTANCE)
    .propertyNamingStrategy(PropertyNamingStrategies.SnakeCaseStrategy())
    .configure(SerializationFeature.CLOSE_CLOSEABLE, true)
    .build()

internal fun setMapper(format: String) {
    metaMapper = when (format) {
        "json", "json5" -> JsonMapper.builder()
        "xml" -> XmlMapper.builder()
        "toml" -> TomlMapper.builder()
        "properties" -> JavaPropsMapper.builder()
        "yaml", "yml" -> YAMLMapper.builder()
        else -> throw IllegalArgumentException("Unknown or unsupported format \"$format\"")
    }.build()
}

@Deprecated("Use jackson serialization instead")
internal val json = Json { namingStrategy = JsonNamingStrategy.SnakeCase }
