package com.awakenedredstone.commons

import org.gradle.api.Project


fun Project.property(propertyName: String): String {
    return this.property(propertyName) as String
}

inline fun <reified T> Project.property(propertyName: String): T {
    return this.property(propertyName) as T
}
