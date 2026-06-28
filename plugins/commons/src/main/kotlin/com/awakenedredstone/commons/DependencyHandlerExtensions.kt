package com.awakenedredstone.commons

import org.gradle.api.artifacts.dsl.DependencyHandler

fun DependencyHandler.bom(dependencyNotation: Any) {
    add("implementation", platform(dependencyNotation))
}
