package com.awakenedredstone.commons

import net.fabricmc.loom.api.RemapConfigurationSettings.PublishingMode
import net.fabricmc.loom.util.Constants
import org.gradle.api.tasks.SourceSet

object ExtendableConfigurations {
    val OPTIONS = listOf(
        createOption(mainOnly { it.apiConfigurationName }, PublishingMode.COMPILE_AND_RUNTIME),
        createOption({ it.implementationConfigurationName }, PublishingMode.RUNTIME_ONLY),
        createOption({ it.compileOnlyConfigurationName }, PublishingMode.NONE, compileClasspath = true, runtimeClasspath = false),
        createOption(mainOnly { it.compileOnlyApiConfigurationName }, PublishingMode.COMPILE_ONLY, compileClasspath = true, runtimeClasspath = false),
        createOption({ it.runtimeOnlyConfigurationName }, PublishingMode.RUNTIME_ONLY, false),
        createOption(mainOnly(Constants.Configurations.LOCAL_RUNTIME), PublishingMode.NONE, false)
    )

    private fun createOption(
        targetNameFunc: (SourceSet) -> String?,
        publishingMode: PublishingMode,
        compileClasspath: Boolean = true,
        runtimeClasspath: Boolean = true,
    ): ConfigurationOption {
        return ConfigurationOption(targetNameFunc, compileClasspath, runtimeClasspath, publishingMode)
    }

    private fun mainOnly(name: String): (SourceSet) -> String? {
        return mainOnly { name }
    }

    private fun mainOnly(function: (SourceSet) -> String?): (SourceSet) -> String? {
        return { if (it.name == SourceSet.MAIN_SOURCE_SET_NAME) function(it) else null }
    }

    private fun getValidOptions(sourceSet: SourceSet): MutableList<ConfigurationOption> {
        return OPTIONS.stream().filter { option: ConfigurationOption -> option.validFor(sourceSet) }.toList()
    }
}
