package com.awakenedredstone.multiversion.util

import org.gradle.api.invocation.Gradle
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType

object GradleExtensions {
    inline fun <reified T : Any> Gradle.createContainer(): T =
        extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" })

    inline fun <reified T : Any> Gradle.createContainer(vararg args: Any): T =
        extensions.create<T>(requireNotNull(T::class.simpleName) { "Provided class has no name" }, *args)

    inline fun <reified T : Any> Gradle.getContainer(): T =
        extensions.getByType<T>()
}
