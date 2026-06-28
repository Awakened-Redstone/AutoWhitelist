package com.awakenedredstone.multiversion.loom

import net.fabricmc.loom.api.RemapConfigurationSettings
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

@JvmRecord
data class ConfigurationOption(
    val targetNameFunc: (SourceSet) -> String?,
    val compileClasspath: Boolean,
    val runtimeClasspath: Boolean,
    val publishingMode: RemapConfigurationSettings.PublishingMode?,
) {
    fun targetName(sourceSet: SourceSet): String? {
        return targetNameFunc(sourceSet)
    }

    fun validFor(sourceSet: SourceSet): Boolean {
        return targetName(sourceSet) != null
    }

    fun name(sourceSet: SourceSet): String {
        var targetName = targetName(sourceSet) ?: throw UnsupportedOperationException("Configuration option is not available for source set (${sourceSet.name})")

        if (targetName.startsWith(sourceSet.name)) {
            targetName = targetName.substring(sourceSet.name.length)
        }

        val builder = StringBuilder()
        builder.append("mod")

        if (SourceSet.MAIN_SOURCE_SET_NAME != sourceSet.name) {
            builder.append(sourceSet.name.uppercaseFirstChar())
        }

        builder.append(targetName.uppercaseFirstChar())
        return builder.toString()
    }
}
