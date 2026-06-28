package com.awakenedredstone.multiversion.meta

import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.serialization.Contextual
import org.gradle.api.internal.provider.MissingValueException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import java.net.URI
import kotlin.reflect.KClass

open class Macros {
    private val parsers: MutableMap<String, MacroOptions<@Contextual Any>> = HashMap()

    /**
     * Register a new property macro. The type must be serializable, with [Contextual] being able to infer it.
     *
     * @param macro The macro id, used to run the macro when parsing the meta file
     * @param params The names for the parameters. Used for error messages and defining the required param count
     * @param parser The macro parser. It receives the passed parameters and returns the property value based on them
     */
    fun <T : Any> register(macro: String, vararg params: String, parser: MacroHandler<T>) {
        parsers[macro] = MacroOptions(parser, params.toList())
    }

    /**
     * Register a new property macro. The type must be serializable, with [Contextual] being able to infer it.
     *
     * @param macro The macro id, used to run the macro when parsing the meta file
     * @param lazy Defines if the macro should only run when the property is queried. Useful if the macro can only run after evaluation
     * @param params The names for the parameters. Used for error messages and defining the required param count
     * @param parser The macro parser. It receives the passed parameters and returns the property value based on them
     */
    fun <T : Any> register(macro: String, lazy: Boolean, vararg params: String, parser: MacroHandler<T>) {
        parsers[macro] = MacroOptions(parser, params.toList(), lazy = lazy)
    }

    internal fun <T : Any> process(macro: String, params: String, allowLazy: Boolean = true): T? {
        @Suppress("UNCHECKED_CAST")
        val options: MacroOptions<T>? = parsers[macro] as MacroOptions<T>?

        var splitParams = if (params.isBlank()) listOf() else params.split(" ")

        if (options == null) {
            throw IllegalArgumentException("Invalid macro $macro")
        }

        if (allowLazy && options.lazy) return null

        if (splitParams.size < options.params.size) {
            val missingParams = options.params.subList(splitParams.size, options.params.size)
            throw MissingValueException("Missing parameter(s) $missingParams, required format #$macro ${options.params.joinToString(" ")}")
        } else if (splitParams.size > options.params.size) {
            val extraParams = splitParams.subList(options.params.size, splitParams.size)
            splitParams = splitParams.subList(0, options.params.size)
            LOGGER.warn("Too many parameters, ignoring $extraParams")
        }

        val paramsLog = splitParams.mapIndexed { index, param -> "${options.params[index]}:$param" }.joinToString(",")
        LOGGER.debug("Processing {} macro with ({})", macro, paramsLog)
        return options.handler.process(splitParams)
    }

    companion object {
        val LOGGER: Logger = Logging.getLogger("Macros")
        private val mapper = jacksonObjectMapper()

        fun <T> fetchData(url: String, typeRef: TypeReference<T>): T {
            LOGGER.lifecycle("Fetching data from $url")
            return URI(url).toURL().openStream().use {
                mapper.readValue(it, typeRef)
            }
        }

        fun <T : Any> fetchData(url: String, `class`: KClass<T>): T {
            LOGGER.lifecycle("Fetching data from $url")
            return URI(url).toURL().openStream().use {
                mapper.readValue(it, `class`.java)
            }
        }
    }

    private data class MacroOptions<out T : Any>(val handler: MacroHandler<T>, val params: List<String>, val lazy: Boolean = false)

    fun interface MacroHandler<out T : Any> {
        fun process(params: List<String>): T
    }
}
