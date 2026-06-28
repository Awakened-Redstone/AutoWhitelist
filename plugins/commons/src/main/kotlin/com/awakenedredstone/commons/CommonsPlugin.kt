package com.awakenedredstone.commons

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel

class CommonsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("idea")

        project.extensions.configure<IdeaModel>("idea") { plugin ->
            plugin.module {
                it.excludeDirs = setOf(it.excludeDirs, project.file(".idea"), project.file("run"), project.file(".kotlin"))
                it.isDownloadJavadoc = true
                it.isDownloadSources = true
            }
        }
    }
}
