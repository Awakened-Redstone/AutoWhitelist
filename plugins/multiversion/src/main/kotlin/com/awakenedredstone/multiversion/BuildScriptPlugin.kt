package com.awakenedredstone.multiversion

import com.awakenedredstone.commons.CommonsPlugin
import com.awakenedredstone.multiversion.loom.DynamicRemapping
import com.awakenedredstone.multiversion.meta.BuildMeta
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class BuildScriptPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply(CommonsPlugin::class.java)

        val stonecutter = project.extensions.getByName("stonecutter") as StonecutterBuildExtension
        val dynamicRemapping = DynamicRemapping(project, stonecutter)
        dynamicRemapping.setup()

        project.extensions.create("meta", BuildMeta::class.java, project, stonecutter, !dynamicRemapping.newMc)
    }
}
