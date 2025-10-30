package com.awakenedredstone.multiversion.meta

import org.gradle.api.internal.provider.MissingValueException
import org.gradle.problems.internal.impl.logger

class Macros(val parsers: MutableMap<String, MacroHandler> = HashMap()) {
    fun register(macro: String, parser: MacroHandler) {
        parsers.put(macro, parser)
    }

    fun process(macro: String, params: String): String {

        val parser = parsers[macro];
        val splitParams = params.split(" ")

        if (parser == null) {
            throw IllegalArgumentException("Invalid macro $macro")
        }

        return parser.process(splitParams)
    }

    fun interface MacroHandler {
        fun process(params: List<String>): String
    }

    class MacroHandlerUtil {
        companion object {
            fun requireParams(params: List<String>, vararg errorMessages: String) {
                if (params.size < errorMessages.size) {
                    throw MissingValueException(errorMessages[params.size - 1])
                } else if (params.size > errorMessages.size) {
                    logger.warn("Too many arguments provided to the macro, ignoring extra arguments")
                }
            }
        }
    }
}
