package com.awakenedredstone.multiversion.meta

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.problems.internal.impl.logger
import java.net.URI

class Macros {
    private val parsers: MutableMap<String, MacroOptions> = HashMap()

    fun register(macro: String, vararg params: String, parser: MacroHandler) {
        parsers.put(macro, MacroOptions(parser, params.toList()))
    }

    fun process(macro: String, params: String): String {
        val parser = parsers[macro];
        var splitParams = if (params.isBlank()) listOf() else params.split(" ")

        if (parser == null) {
            throw IllegalArgumentException("Invalid macro $macro")
        }

        if (splitParams.size < parser.params.size) {
            val missingParams = parser.params.subList(splitParams.size, parser.params.size)
            throw MissingValueException("Missing parameter(s) $missingParams, required format #$macro ${parser.params.joinToString(" ")}")
        } else if (splitParams.size > parser.params.size) {
            val extraParams = splitParams.subList(parser.params.size, splitParams.size)
            splitParams = splitParams.subList(0, parser.params.size)
            logger.warn("Too many parameters, ignoring $extraParams")
        }

        val paramsLog = splitParams.mapIndexed { index, param -> "${parser.params[index]}:$param" }.joinToString(",")
        logger.debug("Processing {} macro with ({})", macro, paramsLog)
        return parser.handler.process(splitParams)
    }

    companion object {
        private val json = jacksonObjectMapper()

        fun <T> fetchData(url: String, typeRef: TypeReference<T>): T {
            logger.lifecycle("Fetching data from $url")
            return URI(url).toURL().openStream().use {
                json.readValue(it, typeRef)
            }
        }
    }

    private data class MacroOptions(val handler: MacroHandler, val params: List<String>)

    fun interface MacroHandler {
        fun process(params: List<String>): String
    }
}
