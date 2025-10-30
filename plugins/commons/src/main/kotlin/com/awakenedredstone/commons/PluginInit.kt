package com.awakenedredstone.commons

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.ide.idea.model.IdeaModel

class PluginInit : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("idea")

        project.extensions.configure<IdeaModel>("idea") {
            module {
                isDownloadJavadoc = true
                isDownloadSources = true
            }
        }
    }
}