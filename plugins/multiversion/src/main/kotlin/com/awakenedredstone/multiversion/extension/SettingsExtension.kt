package com.awakenedredstone.multiversion.extension

import org.gradle.api.provider.Property

@DslMarker @Retention(AnnotationRetention.BINARY)
private annotation class SettingsDsl

@SettingsDsl
abstract class SettingsExtension {
    /**
     * The path for the project meta JSON, relative to the project root.
     *
     * Defaults to `project.meta.json`
     */
    abstract val metaFile: Property<String>

    /**
     * The path for the project meta JSON, relative to the project root.
     *
     * Defaults to `project.meta.json`
     */
    abstract val fileFormat: Property<String>

    internal abstract fun setupStonecutter()
}
