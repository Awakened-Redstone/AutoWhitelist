@file:OptIn(ExperimentalSerializationApi::class)

package com.awakenedredstone.multiversion.meta

import com.awakenedredstone.multiversion.extension.SettingsExtension
import com.awakenedredstone.multiversion.logger
import com.awakenedredstone.multiversion.serialization.MacroCache
import com.awakenedredstone.multiversion.serialization.ProjectMeta
import com.awakenedredstone.multiversion.serialization.VersionCache
import com.awakenedredstone.multiversion.serialization.fetch.mojang.Manifest
import com.awakenedredstone.multiversion.util.createParent
import com.awakenedredstone.multiversion.util.jsonMapper
import com.awakenedredstone.multiversion.util.json
import com.awakenedredstone.multiversion.util.readOrNull
import com.awakenedredstone.multiversion.util.setMapper
import tools.jackson.module.kotlin.readValue
import dev.kikugie.semver.data.SemanticVersion
import dev.kikugie.stonecutter.settings.StonecutterSettingsExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.initialization.Settings
import java.io.File
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

internal open class Metadata @Inject constructor(
    private val settings: Settings,
    extension: SettingsExtension,
    private val stonecutter: StonecutterSettingsExtension
) {
    private val buildDirectory by lazy { settings.gradle.rootProject.layout.buildDirectory }
    private val macroCacheFile by lazy { buildDirectory.file("multiversion-cache/macros.json").map { it.asFile } }
    private val versionCacheFile by lazy { buildDirectory.file("multiversion-cache/game-versions.json").map { it.asFile } }
    private var isDirty = false;

    val projectMeta: ProjectMeta = run {
        val fileName = extension.metaFile.get()
        setMapper(extension.fileFormat.getOrElse(fileName.substringAfterLast('.')))
        decode<ProjectMeta>(settings.rootDir.resolve(fileName), ::ProjectMeta)
    }
    val projectGameVersions by lazy {
        val versions = ArrayList(projectMeta.versions?.keys ?: listOf())
        versions.sortWith { a, b -> stonecutter.compare(a, b) }
        versions
    }
    val macroCache by lazy { decode<MacroCache>(macroCacheFile.get(), ::LinkedHashMap) }
    val versionCache by lazy {
        val cache = decode<VersionCache>(versionCacheFile.get(), ::VersionCache)
        updateVersionCache(cache)
        cache
    }

    fun saveCache() {
        if (!isDirty) return
         jsonMapper.writeValue(macroCacheFile.get().createParent().outputStream(), macroCache)
        isDirty = false
    }

    private fun saveVersionCache(versionCache: VersionCache) {
        jsonMapper.writeValue(versionCacheFile.get().createParent().outputStream(), versionCache)
    }

    fun updateVersionCache() {
        updateVersionCache(versionCache)
    }

    private fun updateVersionCache(versionCache: VersionCache) {
        val expired = versionCache.expiration.isBefore(Instant.now())

        val cachedVersions = versionCache.versions.keys
        val uncachedVersions: Boolean =
            if (projectMeta.versions == null) false
            else {
                (projectGameVersions - cachedVersions).isNotEmpty()
            }

        if (expired || uncachedVersions) {
            logger.lifecycle("Getting version meta from Mojang")
            val manifest = URI("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json").toURL().openStream().use { stream ->
                json.decodeFromStream<Manifest>(stream)
            }

            val gameVersions = manifest.versions.filter { it.type.stable || projectGameVersions.contains(it.id) }
            val fetch = gameVersions.filterNot { cachedVersions.contains(it.id) }

            if (fetch.isNotEmpty()) markDirty()

            for (version in fetch) {
                logger.lifecycle("Getting version data for ${version.id}")
                versionCache.versions[version.id] =  URI(version.url).toURL().openStream().use { stream ->
                    // If there is no Java version details, it's probably an old version, so default to Java 8
                    VersionCache.Version(jsonMapper.readTree(stream)?.get("javaVersion")?.get("majorVersion")?.asText() ?: "8")
                }
            }

            versionCache.expiration = Instant.now().plus(7, ChronoUnit.DAYS)
            saveVersionCache(versionCache)
        }
    }

    fun parseVersion(gameVersion: String): ParsedVersion {
        if (projectMeta.versions == null) throw IllegalStateException("Project has no versions set")
        if (versionCache.versions[gameVersion] == null) {
            // Maybe it's just not cached
            updateVersionCache()
            // If it's still missing, we don't know about it
            if (versionCache.versions[gameVersion] == null) throw NoSuchElementException("Unknown game version $gameVersion")
        }

        logger.debug("Processing project version meta")

        val currentVersion = projectMeta.versions[gameVersion]!!

        var predicate: String
        var versionRange: List<String>

        if (currentVersion.predicate == "auto") {
            logger.debug("Creating predicate from context")
            val versionIndex = projectGameVersions.indexOf(gameVersion)

            if (versionIndex + 1 >= projectGameVersions.size) {
                val mcSemver = stonecutter.parse(gameVersion) as SemanticVersion
                // This is still valid for the new version system, as drops are breaking changes,
                // and 27.1 is still higher than 26.5 and so saying it breaks on 26.5 is ok
                val newSemver = SemanticVersion(listOf(mcSemver.components[0], mcSemver.components[1] + 1))

                predicate = ">=$gameVersion <${newSemver.value}"
            } else {
                val nextVersion = projectGameVersions[versionIndex + 1]
                predicate = ">=$gameVersion <${nextVersion}"
            }
        } else {
            predicate = currentVersion.predicate
        }

        versionRange = versionCache.versions.keys.filter { version -> stonecutter.eval(version, predicate) }

        logger.debug("Version: {}", gameVersion)
        logger.debug("Predicate: {}", predicate)
        logger.debug("Version list: {}", versionRange)

        return ParsedVersion(predicate, versionRange, versionCache.versions[gameVersion]!!.java)
    }

    fun markDirty() {
        isDirty = true
    }

    fun stonecutterTree() {
        settings.rootProject.children
    }

    internal inline fun <reified T> decode(file: File, default: () -> T): T {
        val fileContent = file.readOrNull() ?: return default()
        return jsonMapper.readValue(fileContent)
    }

    data class ParsedVersion(val predicate: String, val versions: List<String>, val java: String)
}
