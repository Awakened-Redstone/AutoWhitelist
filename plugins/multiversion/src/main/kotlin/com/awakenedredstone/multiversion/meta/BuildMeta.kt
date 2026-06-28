package com.awakenedredstone.multiversion.meta

import com.awakenedredstone.multiversion.logger
import com.awakenedredstone.multiversion.serialization.fetch.MavenMetadata
import com.awakenedredstone.multiversion.serialization.fetch.fabric.yarn.YarnVersion
import com.awakenedredstone.multiversion.serialization.fetch.modrinth.ProjectVersion
import com.awakenedredstone.multiversion.serialization.get
import com.awakenedredstone.multiversion.serialization.getOrAdd
import com.awakenedredstone.multiversion.util.GradleExtensions.getContainer
import com.awakenedredstone.multiversion.util.formattedString
import com.awakenedredstone.multiversion.values.Repo
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.kotlinModule
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import kotlinx.serialization.ExperimentalSerializationApi
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.provider.MissingValueException
import java.io.FileNotFoundException
import java.net.URI
import kotlin.time.TimeSource
import kotlin.time.measureTime


@OptIn(ExperimentalSerializationApi::class)
open class BuildMeta internal constructor(
    private val project: Project,
    private val stonecutter: StonecutterBuildExtension,
    @Suppress("unused")
    val hasRemapping: Boolean,
) {
    private val metadata = project.gradle.getContainer<Metadata>()
    private val properties: MutableMap<String, Any> = HashMap()
    private val lazyProperties: MutableMap<String, String> = HashMap()

    val propertyMacros: Macros = Macros()
    val propertyTemplates: Macros = Macros()

    init {
        val duration = measureTime { init() }
        logger.debug("Loaded project meta in {}", duration.formattedString())
    }

    private fun init() {
        val times: MutableMap<String, TimeSource.Monotonic.ValueTimeMark> = linkedMapOf()
        val timeSource = TimeSource.Monotonic
        times["baseline"] = timeSource.markNow()
        registerDefaultMacros()
        times["macroRegistration"] = timeSource.markNow()
        registerDefaultTemplates()
        times["templateRegistration"] = timeSource.markNow()

        val propertiesToApply = mutableMapOf<String, String>()
        if (metadata.projectMeta.properties != null) {
            for (property in metadata.projectMeta.properties) {
                propertiesToApply[property.key] = property.value
            }
        }
        times["staticProperties"] = timeSource.markNow()

        if (metadata.projectMeta.versions != null) {
            val currentVersion = metadata.projectMeta.versions[stonecutter.current.version]!!

            if (currentVersion.properties != null) {
                for (property in currentVersion.properties) {
                    propertiesToApply[property.key] = property.value
                }
            }
            times["versionProperties"] = timeSource.markNow()

            parseVersion()
        }
        times["versionMeta"] = timeSource.markNow()

        for ((name, value) in propertiesToApply) {
            val property = parseProperty(value)
            if (property != null) {
                properties[name] = property
                if (project.hasProperty(name)) {
                    project.setProperty(name, property)
                } else {
                    project.extensions.extraProperties.set(name, property)
                }
            } else {
                lazyProperties[name] = value
            }
        }
        times["applyProperties"] = timeSource.markNow()

        metadata.saveCache()
        times["saveCache"] = timeSource.markNow()

        val entries = ArrayList(times.entries)
        for ((index, entry) in entries.withIndex()) {
            if (index == 0) continue
            val (action, time) = entry
            val (_, lastTime) = entries[index - 1]
            logger.debug("{} took {} ({} total)", action, time - lastTime, time - entries[0].value)
        }
    }

    @Suppress("unused")
    inline fun <reified T> property(property: String): T {
        val value = tryGetProperty(property) ?: throw NoSuchElementException("Property $property is not set")
        return value as T
    }

    fun tryGetProperty(property: String): Any? {
        if (lazyProperties.containsKey(property)) {
            properties[property] = parseProperty(lazyProperties[property]!!, false)!!
            lazyProperties.remove(property)
            metadata.saveCache()
        }

        return properties[property]
    }

    private fun parseProperty(property: String, allowLazy: Boolean = true): Any? {
        return applyMacros(replaceTemplates(property), allowLazy)
    }

    private fun replaceTemplates(property: String): String {
        val openIndex = property.indexOf($$"${")
        if (openIndex == -1) return property

        val closeIndex = property.indexOf("}")
        if (closeIndex == -1) throw IllegalStateException("Unclosed template!")

        val template = property.substring(openIndex + 2, closeIndex)

        val split = template.split(":", limit = 2)
        val processed = propertyTemplates.process<String>(split[0], split.getOrNull(1) ?: "", false)!!

        return replaceTemplates(property.replaceRange(openIndex, closeIndex + 1, processed))
    }

    private fun applyMacros(property: String, lazy: Boolean = true): Any? {
        if (!property.startsWith('#')) return property

        val split = property.split(' ', limit = 2)
        val macro = split[0].substring(1)
        val params = split.getOrNull(1) ?: ""

        return propertyMacros.process(macro, params, lazy)
    }

    private fun parseVersion() {
        val parsedVersion = metadata.parseVersion(stonecutter.current.version)

        properties["predicate"] = parsedVersion.predicate
        properties["versions"] = parsedVersion.versions
        properties["jvm"] = parsedVersion.java
    }

    private fun registerDefaultMacros() {
        propertyMacros.register("modrinth", "slug", "loader", "game_version") { (slug, loader, gameVersion) ->
            val properties: MutableMap<String, String> = metadata.macroCache.getOrAdd("modrinth:$loader/$gameVersion") {
                metadata.markDirty()
                HashMap()
            }

            if (properties.containsKey(slug)) {
                logger.debug("modrinth: Found $slug version in cache, using it")
                return@register properties[slug]!!
            }

            val modrinthResponse = Macros.fetchData(
                "https://api.modrinth.com/v2/project/$slug/version?loaders=%5B%22$loader%22%5D&game_versions=%5B%22$gameVersion%22%5D",
                object : TypeReference<List<ProjectVersion>>() {}
            )

            if (modrinthResponse.isEmpty()) {
                throw NoSuchElementException("No versions of $slug is available for $loader on $gameVersion")
            }

            val version = modrinthResponse.first().versionNumber

            properties[slug] = version

            return@register version
        }

        propertyMacros.register("yarn", "game_version") { (gameVersion) ->
            if (metadata.macroCache.containsKey("yarn:$gameVersion")) {
                logger.debug("yarn: Found yarn version in cache, using it")
                return@register metadata.macroCache.get<String>("yarn:$gameVersion")
            }

            val yarnResponse = Macros.fetchData(
                "https://meta.fabricmc.net/v2/versions/yarn/$gameVersion",
                object : TypeReference<List<YarnVersion>>() {}
            )
            val version = yarnResponse.first().build

            metadata.macroCache["macro:yarn/$gameVersion"] = version
            metadata.markDirty()

            return@register version
        }

        propertyMacros.register("maven", true, "repo") { (repo) ->
            if (metadata.macroCache.containsKey("maven:$repo")) {
                logger.debug("maven: Found $repo version in cache, using it")
                return@register Repo(repo, metadata.macroCache.get<String>("maven:$repo"))

            }

            if (!repo.contains(':')) {
                throw IllegalArgumentException("Invalid repository pattern!")
            }

            val (group, artifact) = repo.split(':')
            val groupPath = group.replace('.', '/')

            val version = project.repositories.withType(MavenArtifactRepository::class.java).distinct()
                .firstNotNullOfOrNull { repository ->
                    val url = "${repository.url}/$groupPath/$artifact/maven-metadata.xml"

                    try {
                        logger.lifecycle("Fetching maven $url")
                        val mapper = XmlMapper.builder().addModule(kotlinModule()).build()

                        val repoData = URI(url).toURL().openStream().use {
                            mapper.readValue(it, MavenMetadata::class.java)
                        }

                        return@firstNotNullOfOrNull repoData.versioning.release
                    } catch (e: JacksonException) {
                        throw IllegalStateException("Failed to process maven data!", e)
                    } catch (_: FileNotFoundException) {
                    } catch (e: Exception) {
                        throw IllegalStateException("Failed to fetch or process maven data!", e)
                    }

                    return@firstNotNullOfOrNull null
                }

            if (version == null) {
                throw IllegalStateException("Could not find version for $repo")
            }

            metadata.macroCache["macro:maven/$repo"] = version
            metadata.markDirty()

            return@register Repo(repo, version)
        }
    }

    private fun registerDefaultTemplates() {
        propertyTemplates.register("version") { stonecutter.current.version }
        propertyTemplates.register("property") { params ->
            if (params.isEmpty()) throw MissingValueException("No property was provided!")
            project.property(params.joinToString("/"))!!.toString()
        }
    }
}
