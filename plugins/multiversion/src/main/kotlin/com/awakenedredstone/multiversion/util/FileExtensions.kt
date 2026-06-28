package com.awakenedredstone.multiversion.util

import java.io.File

fun File.readOrNull(): String? {
    return if (exists()) readText() else null
}

fun File.createParent(): File {
    parentFile.mkdirs()
    return this
}
