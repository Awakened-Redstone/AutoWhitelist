package com.awakenedredstone.multiversion.loom

import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.LoomRemapGradlePlugin
import net.fabricmc.loom.api.RemapConfigurationSettings.PublishingMode
import net.fabricmc.loom.util.Constants
import net.fabricmc.loom.util.gradle.SourceSetHelper
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

class DynamicRemapping(
    private val project: Project,
    private val stonecutter: StonecutterBuildExtension,
) {
    val newMc = stonecutter.eval(stonecutter.current.version, ">=26.1-snapshot-1")

    internal fun setup() {
        project.plugins.apply(if (newMc) LoomNoRemapGradlePlugin::class.java else LoomRemapGradlePlugin::class.java)

        setupRemapping()
    }

    private fun setupRemapping() {
        if (newMc) remappingFallback()
    }

    private fun remappingFallback() {
        project.configurations.create("mappings") {
            it.isCanBeResolved = false
            @Suppress("UnstableApiUsage")
            it.isCanBeDeclared = false
            it.isCanBeConsumed = false
            it.isTransitive = false
        }

        val sourceSet = SourceSetHelper.getMainSourceSet(project)
        for (option in getValidOptions(sourceSet)) {
            val redirect = project.configurations.create(option.name(sourceSet)) {
                it.isTransitive = true
            }

            project.configurations.named(option.targetName(sourceSet)!!) {
                it.extendsFrom(redirect)
            }
        }
    }

    private companion object {
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
}
