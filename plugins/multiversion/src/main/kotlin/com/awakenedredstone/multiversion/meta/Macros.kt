package com.awakenedredstone.multiversion.meta

class Macros(val parsers: MutableMap<String, (params: List<String>) -> String> = HashMap()) {
    fun register(macro: String, parser: (params: List<String>) -> String) {
        parsers.put(macro, parser)
    }

    fun process(macro: String, params: String): String {

        val parser = parsers[macro];
        val splitParams = params.split(" ")

        if (parser == null) {
            throw IllegalArgumentException("Invalid macro $macro")
        }

        return parser.invoke(splitParams)
    }
}
