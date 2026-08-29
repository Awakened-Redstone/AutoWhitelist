package com.awakenedredstone.multiversion

import com.awakenedredstone.commons.CommonsPlugin
import com.awakenedredstone.multiversion.extension.SettingsExtension
import com.awakenedredstone.multiversion.meta.SettingsExtensionImpl
import dev.kikugie.stonecutter.StonecutterPlugin
import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.getByName

class SettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        if (!settings.plugins.hasPlugin(StonecutterPlugin::class.java)) {
            throw IllegalStateException("Stonecutter is not configured, please add stonecutter before multiversion")
        }

        val stonecutter = settings.extensions.getByName<StonecutterSettingsExtension>("stonecutter")
        val extension = settings.createExtension<SettingsExtension, SettingsExtensionImpl>("multiversion", stonecutter)

        extension.setupStonecutter()
        extension.setupLoom()

        settings.gradle.rootProject { it.plugins.apply(CommonsPlugin::class.java) }
    }
}
