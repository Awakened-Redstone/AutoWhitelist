package com.awakenedredstone.multiversion.meta

import com.awakenedredstone.multiversion.extension.SettingsExtension
import com.awakenedredstone.multiversion.util.GradleExtensions.createContainer
import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension
import org.gradle.api.initialization.Settings
import javax.inject.Inject

internal abstract class SettingsExtensionImpl @Inject constructor(
    internal val settings: Settings,
    private val stonecutter: StonecutterSettingsExtension
) : SettingsExtension() {
    private val metadata: Metadata

    init {
        metaFile.convention("project.meta.json")
        metadata = settings.gradle.createContainer<Metadata>(settings, this, stonecutter)
    }

    override fun setupStonecutter() {
        stonecutter.create(settings.rootProject) {
            val allVersions = metadata.projectMeta.versions ?: return@create
            val versions = allVersions.filter { (_, version) -> version.enabled }.keys
            it.versions(versions)
            // TODO: add config option for this
            it.vcsVersion.set(versions.last())
        }
    }

    override fun setupLoom() {
        settings.gradle.beforeProject {
            it.buildscript.dependencies.add("classpath", "net.fabricmc:fabric-loom:${loomVersion.get()}")
        }
    }
}
