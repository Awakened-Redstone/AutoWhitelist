package com.awakenedredstone.multiversion

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.create

class MultiversionPlugin : Plugin<ExtensionAware> {
    override fun apply(project: ExtensionAware) {
        when (project) {
            is Project -> BuildScriptPlugin().apply(project)
            is Settings -> SettingsPlugin().apply(project)
        }
    }
}

internal inline fun <reified P : Any, reified R : P> ExtensionAware.createExtension(name: String, vararg args: Any): P {
    return extensions.create(P::class, name, R::class, this, *args)
}

val logger: Logger = Logging.getLogger("Multiversion")
