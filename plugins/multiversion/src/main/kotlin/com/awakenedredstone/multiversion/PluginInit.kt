package com.awakenedredstone.multiversion

import com.awakenedredstone.multiversion.meta.ProjectMeta
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginInit : Plugin<Project> {
    override fun apply(project: Project) {
        project.getExtensions().create("meta", ProjectMeta::class.java, project)

        project.plugins.apply( com.awakenedredstone.commons.PluginInit::class.java)
    }
}
